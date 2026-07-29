# Investigation: CS Migration — LicenceScheduleDetail per Case Date

> **Status: Implemented.** Shipped in `0ac86df7` ("Add ability to create schedule history for
> term extensions and add WP activity migration"). This document now describes the delivered
> design in `CarbonStorageLicenceMigrationService.migrateSchedules()` rather than a proposal.

## Problem

The original `CarbonStorageLicenceMigrationService.migrateSchedules()` created exactly one
`LicenceScheduleDetail` (status ACTIVE) per carbon storage licence. Both
`cs_term_migration_extract` and `cs_work_programme_migration_extract` contain `case_id` and
`case_date` columns representing distinct amendment cases. The migration needed to:

- Create one `LicenceScheduleDetail` per unique `case_date` across both tables, with
  `createdInstant` sourced from `case_date`.
- Attach terms to the schedule detail whose `case_date` matches the term's own `case_date`. Where
  a case has no terms of its own (i.e. the case_date appears only in the work programme extract),
  inherit the terms from the previous case.
- Attach work programme activities to the schedule detail whose `case_date` matches the
  activity's `case_date`, along with their status history and comments.

## Delivered Design

### Composite case key

Schedule details are indexed by a **`licenceRef + "|" + caseDate`** composite key
(`caseKey(licenceRef, caseDate)` helper), not by `case_date` alone. Two licences can share a
`case_date`, so a `case_date`-only key would collide across licences and attach terms/activities
to the wrong detail. Every `detailsByCaseDate` put/get and the cross-table dedup key go through
`caseKey(...)`.

### Step 1 — Repositories for distinct cases

Neither extract table has a primary key, so distinct cases are read via native-query projections
onto the `CsLicenceCase` interface rather than full JPA entities:

```java
public interface CsLicenceCase {
  String getLicenceRef();
  String getCaseDate();
}
```

Both repositories extend `ListCrudRepository` (so `findAll()` returns a `List` that can be
streamed directly) and expose a distinct-cases query:

```java
// CarbonStorageTermMigrationRepository / CarbonStorageWorkProgrammeMigrationRepository
@Query(value = """
    SELECT DISTINCT licence_ref AS licenceRef, case_date AS caseDate
    FROM lms.cs_term_migration_extract        -- (…_work_programme_migration_extract)
    """, nativeQuery = true)
List<CsLicenceCase> findDistinctCases();
```

### Step 2 — Build a unified, ordered case list per licence (`buildCasesByLicence`)

Distinct cases from both tables are merged, deduplicated by `caseKey`, grouped by `licenceRef`,
and sorted by `case_date` ascending. The latest case becomes ACTIVE; earlier ones become REPLACED.

```java
var allCases = Stream.concat(
    carbonStorageTermMigrationRepository.findDistinctCases().stream(),
    carbonStorageWorkProgrammeMigrationRepository.findDistinctCases().stream()
).collect(Collectors.toMap(
    c -> caseKey(c.getLicenceRef(), c.getCaseDate()),
    c -> c,
    (a, b) -> a  // deduplicate — same case, keep either
)).values();

var casesByLicence = allCases.stream()
    .collect(Collectors.groupingBy(
        CsLicenceCase::getLicenceRef,
        Collectors.collectingAndThen(
            Collectors.toList(),
            list -> list.stream()
                .sorted(Comparator.comparing(c -> LocalDate.parse(c.getCaseDate(), CASE_DATE_FORMAT)))
                .toList())));
```

### Step 3 — Create one LicenceScheduleDetail per case (`buildScheduleDetails`)

Returns a `ScheduleDetailsResult(details, detailsByCaseDate)`. Licences with **no** cases in
either table fall back to a single ACTIVE detail with `createdInstant = Instant.now()`
(preserving the original behaviour — see Resolved Question 1). Otherwise, one detail per case:
the last case ACTIVE, the rest REPLACED, `createdInstant` sourced from the case date.

```java
detail.setStatus(i == cases.size() - 1
    ? LicenceScheduleDetailStatus.ACTIVE
    : LicenceScheduleDetailStatus.REPLACED);
detail.setCreatedInstant(LocalDate.parse(c.getCaseDate(), CASE_DATE_FORMAT)
    .atStartOfDay(ZoneOffset.UTC).toInstant());
detailsByCaseDate.put(caseKey(licenceRef, c.getCaseDate()), detail);
```

Start dates (`buildStartDates`) attach to the ACTIVE detail per licence.

### Step 4 — Apply terms, inheriting when a case has none (`buildTerms`)

Migration terms are grouped by `(licenceRef, caseDate)`. Iterating each licence's cases in order,
a case with its own terms uses them; otherwise `previousTerms` carries forward from the last case
that had terms. Each term is linked to both its detail and its schedule.

```java
for (var c : cases) {
  var termsForCase = termsByCase.getOrDefault(c.getCaseDate(), List.of());
  if (!termsForCase.isEmpty()) {
    previousTerms = termsForCase;  // carry-forward source
  }
  var detail = detailsByCaseDate.get(caseKey(licenceRef, c.getCaseDate()));
  for (var migrationTerm : previousTerms) {
    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(detail);
    term.setLicenceSchedule(detail.getLicenceSchedule());
    term.setTermType(migrationTerm.getTerm().equals("Initial")
        ? TermType.INITIAL_CS
        : EnumUtils.getEnum(TermType.class, migrationTerm.getTerm().toUpperCase()));
    term.setTermDuration(new ThreeFieldDuration(
        migrationTerm.getYears(), migrationTerm.getMonths(), migrationTerm.getDays()));
    terms.add(term);
  }
}
```

After saving, schedule dates are recalculated once per detail (driven off the saved start dates,
`distinct()` by detail).

### Step 5 — Attach work programme activities, statuses and comments

`buildWorkProgrammeActivities` groups WP rows by `(licenceRef, caseDate)`, resolves the detail via
`caseKey`, and returns a single `List<MigratedActivity>` — each bundling the built
`WorkProgrammeActivity` with its raw status display name, comment, and the case instant:

```java
record MigratedActivity(
    WorkProgrammeActivity activity, String statusDisplayName, String comment, Instant caseInstant) {}
```

A single bundle list (rather than the activity list plus two maps keyed by the unsaved activity)
keeps the case instant in one place and avoids using not-yet-persisted entities as map keys. The
activities are saved first, then `buildWorkProgrammeActivityStatuses` and
`buildWorkProgrammeActivityComments` iterate the same list to build the
`WorkProgrammeActivityStatus` and `EventComment` rows (comments with a `null` body are skipped).

Field mapping performed in `buildWorkProgrammeActivity`: `category` (with `otherCategoryName` when
`OTHER_ACTIVITY`), `description`, `originalEventId` from `uniqueEventId`, `commitment`, the linked
`LicenceScheduleTerm` (matched by term type within the detail), `dateOption`, and
`relativeDuration` when `dateOption == RELATIVE_DATE`.

## Resolved Questions

1. **Licences with no cases in either table** — fall back to a single ACTIVE detail with
   `createdInstant = Instant.now()`, matching the original behaviour.
2. **`case_date` format** — TEXT in `dd/MM/yyyy` (the shared `CASE_DATE_FORMAT`).
3. **Work programme activity field mapping** — resolved as described in Step 5.

## Affected Files

- `CarbonStorageLicenceMigrationService` — `migrateSchedules()` and its `build*` helpers
- `CarbonStorageTermMigrationExtract` / `CarbonStorageTermMigrationRepository` — `caseDate` field,
  `findDistinctCases()`, `ListCrudRepository`
- `CarbonStorageWorkProgrammeMigrationExtract` / `CarbonStorageWorkProgrammeMigrationRepository`
- `CsLicenceCase` — projection interface
- `ScheduleDetailsResult`, `MigratedActivity` — internal carrier records

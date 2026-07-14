# Investigation: CS Migration — LicenceScheduleDetail per Case Date

## Problem

The current `CarbonStorageLicenceMigrationService.migrateSchedules()` creates exactly one
`LicenceScheduleDetail` (status ACTIVE) per carbon storage licence. Both
`cs_term_migration_extract` and `cs_work_programme_migration_extract` contain `case_id` and
`case_date` columns representing distinct amendment cases. The migration needs to:

- Create one `LicenceScheduleDetail` per unique `case_date` across both tables, with
  `createdInstant` sourced from `case_date`.
- Attach terms to the schedule detail whose `case_date` matches the term's own `case_date`. Where
  a case has no terms of its own (i.e. the case_date appears only in the work programme extract),
  inherit the terms from the previous case.
- Attach work programme activities to the schedule detail whose `case_date` matches the
  activity's `case_date`.

## Proposed Approach

### Step 1 — Repositories for distinct cases

Because neither table has a primary key, use native-query projections rather than full JPA
entities. A typed projection interface is preferable to raw `Object[]`:

```java
public interface CsLicenceCase {
  String getLicenceRef();
  String getCaseDate();
}
```

Add a query to each repository that returns distinct `(licence_ref, case_date)`:

```java
// CarbonStorageTermMigrationRepository
@Query(value = """
    SELECT DISTINCT licence_ref AS licenceRef, case_date AS caseDate
    FROM lms.cs_term_migration_extract
    """, nativeQuery = true)
List<CsLicenceCase> findDistinctCases();

// CarbonStorageWorkProgrammeMigrationRepository (new)
@Query(value = """
    SELECT DISTINCT licence_ref AS licenceRef, case_date AS caseDate
    FROM lms.cs_work_programme_migration_extract
    """, nativeQuery = true)
List<CsLicenceCase> findDistinctCases();
```

### Step 2 — Build a unified, ordered case list per licence

Merge the distinct cases from both tables, deduplicate by `case_date`, and sort by `case_date`
ascending. The latest case becomes ACTIVE; all earlier ones become REPLACED.

```java
// Merge and deduplicate cases from both tables
var allCases = Stream.concat(
    termMigrationRepository.findDistinctCases().stream(),
    workProgrammeMigrationRepository.findDistinctCases().stream()
)
.collect(Collectors.toMap(
    c -> c.getLicenceRef() + "|" + c.getCaseDate(),
    c -> c,
    (a, b) -> a  // deduplicate — same case_date, keep either
))
.values();

// Group by licenceRef, sorted by case_date ascending
var casesByLicence = allCases.stream()
    .collect(Collectors.groupingBy(
        CsLicenceCase::getLicenceRef,
        Collectors.collectingAndThen(
            Collectors.toList(),
            list -> list.stream()
                .sorted(Comparator.comparing(c ->
                    LocalDate.parse(c.getCaseDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .toList()
        )
    ));
```

### Step 3 — Create one LicenceScheduleDetail per case

```java
// caseDate → LicenceScheduleDetail, needed when attaching terms and activities below
var detailsByCaseDate = new LinkedHashMap<String, LicenceScheduleDetail>();
var licenceScheduleDetails = new ArrayList<LicenceScheduleDetail>();

for (var savedSchedule : savedLicenceSchedules) {
  var licenceRef = savedSchedule.getLicence().getLicenceReference();
  var cases = casesByLicence.getOrDefault(licenceRef, List.of());

  for (int i = 0; i < cases.size(); i++) {
    var c = cases.get(i);
    var caseDate = LocalDate.parse(c.getCaseDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    var detail = new LicenceScheduleDetail();
    detail.setLicenceSchedule(savedSchedule);
    detail.setStatus(i == cases.size() - 1
        ? LicenceScheduleDetailStatus.ACTIVE
        : LicenceScheduleDetailStatus.REPLACED);
    detail.setCreatedInstant(caseDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    licenceScheduleDetails.add(detail);
    detailsByCaseDate.put(c.getCaseDate(), detail);
  }
}

licenceScheduleDetailService.saveLicenceScheduleDetails(licenceScheduleDetails);
```

### Step 4 — Apply terms, inheriting when a case has none

Group migration terms by `(licenceRef, caseDate)`. Then iterate over each licence's cases in
order: if a case has its own terms use them, otherwise carry forward the terms from the
previous case.

```java
// Group migration terms by licenceRef then caseDate
var termsByLicenceAndCase = migrationTerms.stream()
    .collect(Collectors.groupingBy(
        CarbonStorageTermMigrationExtract::getLicenceRef,
        Collectors.groupingBy(CarbonStorageTermMigrationExtract::getCaseDate)
    ));

var terms = new ArrayList<LicenceScheduleTerm>();

for (var savedSchedule : savedLicenceSchedules) {
  var licenceRef = savedSchedule.getLicence().getLicenceReference();
  var cases = casesByLicence.getOrDefault(licenceRef, List.of());
  var termsByCase = termsByLicenceAndCase.getOrDefault(licenceRef, Map.of());

  List<CarbonStorageTermMigrationExtract> previousTerms = List.of();

  for (var c : cases) {
    var termsForCase = termsByCase.getOrDefault(c.getCaseDate(), List.of());
    if (!termsForCase.isEmpty()) {
      previousTerms = termsForCase;
    }
    // If termsForCase is empty, previousTerms carries forward from the last case that had terms

    var detail = detailsByCaseDate.get(c.getCaseDate());
    for (var migrationTerm : previousTerms) {
      var term = new LicenceScheduleTerm();
      term.setLicenceScheduleDetail(detail);
      var termType = migrationTerm.getTerm().equals("Initial")
          ? TermType.INITIAL_CS
          : EnumUtils.getEnum(TermType.class, migrationTerm.getTerm().toUpperCase());
      term.setTermType(termType);
      term.setTermDuration(
          new ThreeFieldDuration(migrationTerm.getYears(), migrationTerm.getMonths(), migrationTerm.getDays())
      );
      terms.add(term);
    }
  }
}

licenceScheduleTermService.saveTerms(terms);
```

After saving, recalculate schedule dates once per detail (not once per term):

```java
detailsByCaseDate.values().stream()
    .distinct()
    .forEach(licenceScheduleCalculationService::calculateAndSaveLicenceScheduleDates);
```

### Step 5 — Attach work programme activities

Group work programme rows by `(licenceRef, caseDate)` and attach each to the matching detail
via `detailsByCaseDate`.

```java
// Group work programme rows by licenceRef then caseDate
var wpByLicenceAndCase = workProgrammeMigrationRepository.findAll().stream()
    .collect(Collectors.groupingBy(
        CarbonStorageWorkProgrammeMigrationExtract::getLicenceRef,
        Collectors.groupingBy(CarbonStorageWorkProgrammeMigrationExtract::getCaseDate)
    ));

var activities = new ArrayList<WorkProgrammeActivity>();

for (var entry : wpByLicenceAndCase.entrySet()) {
  for (var caseEntry : entry.getValue().entrySet()) {
    var detail = detailsByCaseDate.get(caseEntry.getKey()); // look up by caseDate
    if (detail == null) continue;

    for (var wpRow : caseEntry.getValue()) {
      var activity = new WorkProgrammeActivity();
      activity.setLicenceScheduleDetail(detail);
      // map remaining fields from wpRow...
      activities.add(activity);
    }
  }
}

workProgrammeActivityService.saveAll(activities);
```

## Open Questions

1. **Licences with no cases in either table** — decide whether to skip those licences or fall
   back to creating a single ACTIVE detail (as the current code does).

2. **`case_date` format** — both columns are TEXT. Confirm the format is `dd/MM/yyyy`
   (matching `cs_start_date_migration_extract`) before parsing.

3. **Work programme activity fields** — `cs_work_programme_migration_extract` has `category`,
   `description`, `commitment`, `status`, `term`, and `comments` columns. Confirm the mapping
   to `WorkProgrammeActivity` fields before implementing Step 5.

## Affected Files

- `CarbonStorageLicenceMigrationService` — `migrateSchedules()`
- `CarbonStorageTermMigrationExtract` — add `caseDate` field
- `CarbonStorageTermMigrationRepository` — add `findDistinctCases()` projection query
- New: `CarbonStorageWorkProgrammeMigrationExtract` entity
- New: `CarbonStorageWorkProgrammeMigrationRepository`
- New: work programme activity entity/service wiring (Step 5)

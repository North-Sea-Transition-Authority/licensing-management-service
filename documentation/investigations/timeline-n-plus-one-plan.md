# Plan: Eliminate N+1 Queries in `LicenceScheduleTimelineService`

## Changes since this plan was written

This plan was written against commit `170a24ca`. The following changes have landed on
`LicenceScheduleTimelineService` and its collaborators since then and are reflected in the
rest of this document:

- **The `Active` prefix was dropped from every method this plan references** —
  `getActivePhasesByTerm` → `getPhasesByTerm`, `getActiveLicenceScheduleRatesByPhase` →
  `getLicenceScheduleRatesByPhase`, `getActiveTermsByLicenceScheduleDetail` →
  `getTermsByLicenceScheduleDetail`, and so on for the WPA and other-event equivalents.
  This is a **pure naming cleanup with no behavioural change**: commit `d14f0fb6`
  (LMS1-485, #338) replaced status-based soft deletion of schedule events with a hard
  delete and removed the status field from `LicenceSchedulePhase`, `LicenceScheduleRate`,
  `LicenceScheduleTerm`, `OtherScheduleEvent`, and `WorkProgrammeActivity` entirely — there
  is no "active vs inactive" row to filter any more, so the pre-fetch work below needs no
  status/active filtering either.
- **A new `finalProgressDate` parameter is now threaded through the entire private call
  chain** (`getLicenceScheduleEventViews` → `convertToTimelineTermView` →
  `getScheduleEventsForTerm`/`getEndOfTermRequirementEvents` →
  `convertToTimelinePhaseView` → `getScheduleEventsForPhase`/`getEndOfPhaseRequirementEvents`),
  added to support `showStartDateProgress`/`showEndDateProgress` on terms and phases
  (LMS1-513, #480). Every signature change proposed in Step 2 needs to preserve this
  parameter alongside the new grouped-data maps/record.
- **`getEventsBeyondFinalTerm` (and its `getActiveWorkProgrammeActivitiesAfterDate`/
  `getActiveRatesAfterDate`/`getActiveEventsAfterDate` calls) has been deleted entirely**
  (LMS1-571, #475, "remove events requiring attention section"). It was not part of the
  original N+1 problem table and there are no remaining references to it anywhere in the
  codebase, so it drops out of scope for this plan rather than needing conversion.
- **`TimelineFilterForm` was renamed to `ScheduleTimelineFilterForm`** — cosmetic, but the
  type name appears in `getLicenceScheduleEventViews`'s signature.
- Comment lookups switched from `eventReference.getId()` to `getOriginalEventId()`
  (LMS1-575, #442, JPA-inheritance event-reference refactor) — unrelated to query count,
  but worth knowing if you're re-reading the diff.
- One implementation nuance confirmed while re-checking the services: the per-term/per-phase
  WPA and other-event "date range" methods (`getWorkProgrammeActivitiesByDateRangeFor`,
  `getScheduleEventsByDateRangeFor`) already delegate to a query scoped by
  `licenceScheduleDetail` + a date range (`findAllByLicenceScheduleDetailAndDueDateBetween` /
  `...AndEventDateBetween`) rather than by the term/phase FK directly. This doesn't change
  the N+1 classification — it's still one DB round-trip per loop iteration — but it does
  mean the in-memory filter proposed in Step 1 (filtering `allActivities`/`allOtherEvents` by
  `dueDate`/`eventDate` against the term/phase's date range) is functionally identical to
  what the existing per-call query already does, just fetched once instead of T+TP times.

## Problem

For a schedule with **T terms** and **P phases per term**, rendering the timeline page
fires approximately **5T + 5TP** database queries:

| Call site | Query | Count |
|---|---|---|
| `getScheduleEventsForTerm` | `getPhasesByTerm(term)` | T |
| `getScheduleEventsForTerm` | `getLicenceScheduleRatesAttachedToTerm(term)` (when phases exist) | T |
| `getScheduleEventsForTerm` | `getWorkProgrammeActivitiesByDateRangeFor(term)` (no phases) | T |
| `getScheduleEventsForTerm` | `getLicenceScheduleRatesByTerm(term)` (no phases) | T |
| `getScheduleEventsForTerm` | `getScheduleEventsByDateRangeFor(term)` (no phases) | T |
| `getEndOfTermRequirementEvents` | `getWorkProgrammeActivitiesByTermAndDateOption(term, WITHIN_A_TERM)` | T |
| `getEndOfTermRequirementEvents` | `getScheduleEventsByTermAndDateOption(term, WITHIN_A_TERM)` | T |
| `getScheduleEventsForPhase` | `getWorkProgrammeActivitiesByDateRangeFor(phase)` | T×P |
| `getScheduleEventsForPhase` | `getLicenceScheduleRatesByPhase(phase, firstPhaseType)` | T×P |
| `getScheduleEventsForPhase` | `getScheduleEventsByDateRangeFor(phase)` | T×P |
| `getEndOfPhaseRequirementEvents` | `getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WITHIN_A_PHASE)` | T×P |
| `getEndOfPhaseRequirementEvents` | `getScheduleEventsByPhaseAndDateOption(phase, WITHIN_A_PHASE)` | T×P |

(Method names above reflect the current codebase — the `Active` prefix used at the time
this plan was first written has since been dropped; see "Changes since this plan was
written" above.)

A 3-term schedule with 2 phases per term produces ~21 queries just to build the timeline.
The target is a fixed number of queries regardless of T and P.

## Approach

Fetch all child data for the schedule **once at the top level** using the existing
`findAllByLicenceScheduleDetail` repository methods (these already exist for all four
entity types). Group and filter the results in memory, then thread the grouped maps down
through `convertToTimelineTermView` → `getScheduleEventsForTerm` → `convertToTimelinePhaseView`
→ `getScheduleEventsForPhase` as method parameters.

No new repository methods are required for most entity types. The rate logic is the
exception (see Step 3).

## Steps

### Step 1 — Pre-fetch phases, WPAs, and other events at the schedule level

In `getLicenceScheduleEventViews`, before the `.stream().map(term -> ...)` call, add:

```java
// Fetch all phases for the schedule and group by term
var allPhases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);
var phasesByTerm = allPhases.stream()
    .collect(Collectors.groupingBy(LicenceSchedulePhase::getLicenceScheduleTerm));

// Fetch all WPAs for the schedule and group by term/phase/dateOption
var allActivities = workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail);
var activitiesByTerm = allActivities.stream()
    .filter(a -> a.getLicenceScheduleTerm() != null && a.getDateOption() == WorkProgrammeActivityDateOption.WITHIN_A_TERM)
    .collect(Collectors.groupingBy(WorkProgrammeActivity::getLicenceScheduleTerm));
var activitiesByPhase = allActivities.stream()
    .filter(a -> a.getLicenceSchedulePhase() != null && a.getDateOption() == WorkProgrammeActivityDateOption.WITHIN_A_PHASE)
    .collect(Collectors.groupingBy(WorkProgrammeActivity::getLicenceSchedulePhase));

// Fetch all other schedule events similarly
var allOtherEvents = otherScheduleEventService.getOtherScheduleEvents(licenceScheduleDetail);
var otherEventsByTerm = allOtherEvents.stream()
    .filter(e -> e.getLicenceScheduleTerm() != null && e.getDateOption() == OtherScheduleEventDateOption.WITHIN_A_TERM)
    .collect(Collectors.groupingBy(OtherScheduleEvent::getLicenceScheduleTerm));
var otherEventsByPhase = allOtherEvents.stream()
    .filter(e -> e.getLicenceSchedulePhase() != null && e.getDateOption() == OtherScheduleEventDateOption.WITHIN_A_PHASE)
    .collect(Collectors.groupingBy(OtherScheduleEvent::getLicenceSchedulePhase));
```

Confirmed against the current codebase: `getPhasesByLicenceScheduleDetail(LicenceScheduleDetail)`
already exists in `LicenceSchedulePhaseService` (line 45, delegates to the
`@DuplicateThisOnUpdate`-annotated `findAllByLicenceScheduleDetail`). `getWorkProgrammeActivities
(LicenceScheduleDetail)` already exists in `WorkProgrammeActivityService` (line 49, was line 101
when this plan was first drafted — line numbers have moved as the class grew). `getOtherScheduleEvents
(LicenceScheduleDetail)` already exists in `OtherScheduleEventService` (line 38, used by the
duplication service). None of these three batch methods need a status/active filter — see
"Changes since this plan was written".

For the date-range queries (activities/events displayed within a term or phase's date
window rather than via the `dateOption` FK), filter by `dueDate`/`eventDate` in memory:

```java
// Used instead of getActiveWorkProgrammeActivitiesByDateRangeFor(term)
allActivities.stream()
    .filter(a -> !a.getDueDate().isBefore(term.getStartDate())
              && !a.getDueDate().isAfter(term.getEndDate()))
    .toList()
```

Apply the same pattern for phases and for `OtherScheduleEvent.getEventDate()`.

### Step 2 — Thread the grouped maps through the private call chain

Every method in this chain already carries a `finalProgressDate` parameter (added by
LMS1-513 after this plan was first drafted, for `showStartDateProgress`/
`showEndDateProgress`). Add the new maps/record alongside it — don't drop it.

Change the signatures of the following private methods to accept the pre-fetched maps
instead of calling the services internally:

| Method | Maps to add as parameters |
|---|---|
| `convertToTimelineTermView` | `phasesByTerm`, `activitiesByTerm`, `activitiesByPhase`, `otherEventsByTerm`, `otherEventsByPhase`, `allActivities`, `allOtherEvents` |
| `getScheduleEventsForTerm` | same as above |
| `getEndOfTermRequirementEvents` | `activitiesByTerm`, `otherEventsByTerm` |
| `convertToTimelinePhaseView` | `activitiesByPhase`, `otherEventsByPhase`, `allActivities`, `allOtherEvents` |
| `getScheduleEventsForPhase` | `allActivities`, `allOtherEvents` (filtered in-method by phase date range) |
| `getEndOfPhaseRequirementEvents` | `activitiesByPhase`, `otherEventsByPhase` |

To keep the signatures manageable, consider introducing a private `ScheduleEventData`
record to carry the grouped maps as a single parameter:

```java
private record ScheduleEventData(
    Map<LicenceScheduleTerm, List<LicenceSchedulePhase>> phasesByTerm,
    Map<LicenceScheduleTerm, List<WorkProgrammeActivity>> withinTermActivities,
    Map<LicenceSchedulePhase, List<WorkProgrammeActivity>> withinPhaseActivities,
    List<WorkProgrammeActivity> allActivities,
    Map<LicenceScheduleTerm, List<OtherScheduleEvent>> withinTermOtherEvents,
    Map<LicenceSchedulePhase, List<OtherScheduleEvent>> withinPhaseOtherEvents,
    List<OtherScheduleEvent> allOtherEvents
) {}
```

All of these are private methods within `LicenceScheduleTimelineService`, so the signature
changes are fully internal with no impact on callers.

### Step 3 — Handle rates (the complex case)

`getLicenceScheduleRatesByPhase` has conditional logic that makes it harder to
pre-group naively:

1. If this is the **first phase** of its term and the term has any TERM-linked rates
   (`RateDefinitionOption.TERM`): return empty (those rates are rendered at the term level).
2. Otherwise if the phase has PHASE-linked rates: return those.
3. Otherwise: return rates whose `startDate` falls within the phase's date range.

`LicenceScheduleRateRepository` already has the singular versions of these —
`findAllByLicenceScheduleTermAndRateDefinitionOption` (line 27) and
`findAllByLicenceSchedulePhaseAndRateDefinitionOption` (line 32) — used today by the
per-term/per-phase service calls above. A clean approach is to add the `...In` batch
equivalents:

```java
// LicenceScheduleRateRepository
List<LicenceScheduleRate> findAllByLicenceScheduleTermInAndRateDefinitionOption(
    Collection<LicenceScheduleTerm> terms, RateDefinitionOption option);

List<LicenceScheduleRate> findAllByLicenceSchedulePhaseInAndRateDefinitionOption(
    Collection<LicenceSchedulePhase> phases, RateDefinitionOption option);
```

Spring Data derives these from the method names — no custom JPQL needed. As with the other
entities, there's no status/active field to worry about excluding here (see "Changes since
this plan was written") — the hard-delete refactor means every row returned is live.

With these, the pre-fetch in `getLicenceScheduleEventViews` becomes:

```java
var allTerms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
var allPhases = ...; // already loaded in Step 1

var termLinkedRatesByTerm = licenceScheduleRateService
    .getTermLinkedRatesByTerms(allTerms)  // wraps the new findAllByLicenceScheduleTermIn...
    .stream().collect(Collectors.groupingBy(LicenceScheduleRate::getLicenceScheduleTerm));

var phaseLinkedRatesByPhase = licenceScheduleRateService
    .getPhaseLinkedRatesByPhases(allPhases)  // wraps the new findAllByLicenceSchedulePhaseIn...
    .stream().collect(Collectors.groupingBy(LicenceScheduleRate::getLicenceSchedulePhase));

var allRates = licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail);
// used for date-range fallback, filtered in memory by date range
```

Add the grouped rate maps to `ScheduleEventData` and replicate the conditional logic from
`getLicenceScheduleRatesByPhase`/`getLicenceScheduleRatesByTerm` in memory
inside `getScheduleEventsForTerm` and `getScheduleEventsForPhase`. Note `getLicenceScheduleRatesByTerm`
and `getLicenceScheduleRatesByPhase` currently fall back to
`findAllByLicenceScheduleDetailAndStartDateBetween` (not a status/active-filtered query —
see above) when there's no TERM/PHASE-linked rate; the in-memory equivalent is filtering
`allRates` by `startDate` against the term/phase's date range, as already described.

### Step 4 — Remove the now-redundant per-term/per-phase service calls

Once the maps are threaded through, delete the internal service calls inside:
- `getScheduleEventsForTerm`
- `getScheduleEventsForPhase`
- `getEndOfTermRequirementEvents`
- `getEndOfPhaseRequirementEvents`

The services (`LicenceSchedulePhaseService`, `WorkProgrammeActivityService`, etc.) keep
their existing single-item methods untouched — they are used elsewhere in the codebase.

### Step 5 — Update tests

`LicenceScheduleTimelineServiceTest` (and any related tests) will need stubs updated. Note
the test file has already been substantially rewritten since this plan was drafted (to
match the `finalProgressDate`/`ScheduleTimelineFilterForm`/`getOriginalEventId()` changes
described above), so re-read it fresh rather than assuming the shape from when this plan
was written:

- Replace per-term stubs like `when(licenceSchedulePhaseService.getPhasesByTerm(term1))`
  with a single `when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(detail))`
  returning all phases.
- Replace per-term/phase WPA and other-event stubs similarly.
- Add stubs for the two new rate repository/service batch methods.
- Existing tests for `getLicenceScheduleRatesByPhase` and `getLicenceScheduleRatesByTerm`
  can be kept as-is (those methods still exist for other callers).

## Result

After this change, a 3-term, 2-phase-per-term schedule renders the timeline from
approximately **6 fixed queries** (one each for: terms, phases, activities, rates TERM-linked,
rates PHASE-linked, rates all-detail, other events) rather than ~21 variable queries.

## Implementation status: done

Implemented as described above, with a few naming/structuring choices made along the way:

- The pre-fetch step lives in a new private `buildScheduleEventData` method, returning the
  `ScheduleEventData` record described in Step 2 (all ten fields as planned).
- The two new rate repository/service batch methods were named
  `findAllByLicenceScheduleTermInAndRateDefinitionOption`/`getLicenceScheduleRatesForTermsAndDefinitionOption`
  and `findAllByLicenceSchedulePhaseInAndRateDefinitionOption`/`getLicenceScheduleRatesForPhasesAndDefinitionOption`
  — plural forms of the existing singular `getLicenceScheduleRatesForTermAndDefinitionOption`/
  `getLicenceScheduleRatesForPhaseAndDefinitionOption` methods already on `LicenceScheduleRateService`.
- The date-range filter (Step 1's in-memory replacement for `...ByDateRangeFor`) and the rate
  fallback logic (Step 3) were factored into small private helpers — `filterByDateRange`
  (generic, reused for activities/events/rates) and `resolveRatesForTerm`/`resolveRatesForPhase`
  (replicating `LicenceScheduleRateService.getLicenceScheduleRatesByTerm`/`...ByPhase` against the
  pre-fetched maps) — rather than inlining the logic at each call site.
- `LicenceScheduleTimelineServiceTest` needed its fixtures to set real FK relationships
  (`WorkProgrammeActivity`/`OtherScheduleEvent`'s `dateOption` + `licenceScheduleTerm`/
  `licenceSchedulePhase`) that the old per-call mocks didn't require, since the new
  implementation groups pre-fetched data in memory by those relationships rather than
  returning canned lists per mocked method call.
- All 24 tests in that class pass, as do the full `licence.schedule.*` and `architecture.*`
  suites (including the `@IntegrationTest` ones exercising the new repository methods against
  real Postgres via Testcontainers). `checkstyleMain` is clean.

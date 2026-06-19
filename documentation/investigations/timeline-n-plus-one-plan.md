# Plan: Eliminate N+1 Queries in `LicenceScheduleTimelineService`

## Problem

For a schedule with **T terms** and **P phases per term**, rendering the timeline page
fires approximately **5T + 5TP** database queries:

| Call site | Query | Count |
|---|---|---|
| `getScheduleEventsForTerm` | `getActivePhasesByTerm(term)` | T |
| `getScheduleEventsForTerm` | `getActiveLicenceScheduleRatesAttachedToTerm(term)` (when phases exist) | T |
| `getScheduleEventsForTerm` | `getActiveWorkProgrammeActivitiesByDateRangeFor(term)` (no phases) | T |
| `getScheduleEventsForTerm` | `getActiveLicenceScheduleRatesByTerm(term)` (no phases) | T |
| `getScheduleEventsForTerm` | `getActiveScheduleEventsByDateRangeFor(term)` (no phases) | T |
| `getEndOfTermRequirementEvents` | `getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WITHIN_A_TERM)` | T |
| `getEndOfTermRequirementEvents` | `getActiveScheduleEventsByTermAndDateOption(term, WITHIN_A_TERM)` | T |
| `getScheduleEventsForPhase` | `getActiveWorkProgrammeActivitiesByDateRangeFor(phase)` | T×P |
| `getScheduleEventsForPhase` | `getActiveLicenceScheduleRatesByPhase(phase, firstPhaseType)` | T×P |
| `getScheduleEventsForPhase` | `getActiveScheduleEventsByDateRangeFor(phase)` | T×P |
| `getEndOfPhaseRequirementEvents` | `getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WITHIN_A_PHASE)` | T×P |
| `getEndOfPhaseRequirementEvents` | `getActiveScheduleEventsByPhaseAndDateOption(phase, WITHIN_A_PHASE)` | T×P |

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
var allPhases = licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail);
var phasesByTerm = allPhases.stream()
    .collect(Collectors.groupingBy(LicenceSchedulePhase::getLicenceScheduleTerm));

// Fetch all WPAs for the schedule and group by term/phase/dateOption
var allActivities = workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail);
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

Check whether `getActiveWorkProgrammeActivities(LicenceScheduleDetail)` already exists in
`WorkProgrammeActivityService` — it does at line 101. Check whether
`getOtherScheduleEvents(LicenceScheduleDetail)` exists in `OtherScheduleEventService` — it
also already exists (used by the duplication service).

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

`getActiveLicenceScheduleRatesByPhase` has conditional logic that makes it harder to
pre-group naively:

1. If this is the **first phase** of its term and the term has any TERM-linked rates
   (`RateDefinitionOption.TERM`): return empty (those rates are rendered at the term level).
2. Otherwise if the phase has PHASE-linked rates: return those.
3. Otherwise: return rates whose `startDate` falls within the phase's date range.

A clean approach is to add two new batch repository methods:

```java
// LicenceScheduleRateRepository
List<LicenceScheduleRate> findAllByLicenceScheduleTermInAndRateDefinitionOption(
    Collection<LicenceScheduleTerm> terms, RateDefinitionOption option);

List<LicenceScheduleRate> findAllByLicenceSchedulePhaseInAndRateDefinitionOption(
    Collection<LicenceSchedulePhase> phases, RateDefinitionOption option);
```

Spring Data derives these from the method names — no custom JPQL needed.

With these, the pre-fetch in `getLicenceScheduleEventViews` becomes:

```java
var allTerms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);
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
`getActiveLicenceScheduleRatesByPhase`/`getActiveLicenceScheduleRatesByTerm` in memory
inside `getScheduleEventsForTerm` and `getScheduleEventsForPhase`.

### Step 4 — Remove the now-redundant per-term/per-phase service calls

Once the maps are threaded through, delete the internal service calls inside:
- `getScheduleEventsForTerm`
- `getScheduleEventsForPhase`
- `getEndOfTermRequirementEvents`
- `getEndOfPhaseRequirementEvents`

The services (`LicenceSchedulePhaseService`, `WorkProgrammeActivityService`, etc.) keep
their existing single-item methods untouched — they are used elsewhere in the codebase.

### Step 5 — Update tests

`LicenceScheduleTimelineServiceTest` (and any related tests) will need stubs updated:

- Replace per-term stubs like `when(licenceSchedulePhaseService.getActivePhasesByTerm(term1))`
  with a single `when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(detail))`
  returning all phases.
- Replace per-term/phase WPA and other-event stubs similarly.
- Add stubs for the two new rate repository/service batch methods.
- Existing tests for `getActiveLicenceScheduleRatesByPhase` and `getActiveLicenceScheduleRatesByTerm`
  can be kept as-is (those methods still exist for other callers).

## Result

After this change, a 3-term, 2-phase-per-term schedule renders the timeline from
approximately **6 fixed queries** (one each for: terms, phases, activities, rates TERM-linked,
rates PHASE-linked, rates all-detail, other events) rather than ~21 variable queries.

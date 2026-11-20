# Investigation: Migrating PEARS Licence Schedules into LMS

> **Status: Proposal.** Nothing here is implemented. It plans a schedule migration for every PEARS
> licence schedule on a PEARS-managed licence type, whatever the licence status. It reuses the PEARS Oracle datasource
> (`migration/pears`) and the schedule-building pattern from the carbon storage migration
> (`migration/carbonstorage`). Survey queries and their live results are kept in
> [pears-schedule-migration-data-survey.md](pears-schedule-migration-data-survey.md).

## Problem

PEARS licences are already refreshed into LMS from PEARS (`PearsLicenceRefreshService`), and
their positions are rebuilt from the PEARS operation history (`LicenceWritebackService`). Their
**schedules** are not migrated: terms, phases, rates, mandatory relinquishments, expiry and the
user-defined event types (Bespoke Milestone, Drill-or-drop, and so on) exist only in the legacy Fox
licence schedule screen (`PED012X`). Nor are the licences' **retention/development and
pre-producing (prospective/producing) areas**, whose approval, expiry and work programme events
appear on the schedule as `RD` (onshore) and `PA` (offshore) events. These areas are migrated in
the **same exercise**, so the schedule and its areas arrive in LMS together (see "Retention,
development and pre-producing areas").

The migration needs to:

- Read each licence's schedule from PEARS over the existing read-only Oracle connection.
- **Decided: migrate every PEARS licence schedule on a PEARS-managed licence type
  (`LicenceType.isManagedByLms() == false`), whatever the licence status.** See "Scope: licence
  types and statuses".
- Build a `LicenceSchedule`, `LicenceScheduleDetail`, `LicenceStartDate` and the schedule events
  (terms, phases, rates, expiry, other schedule events) through the existing services.
- Arrive at the **same dates PEARS shows**, and report every licence where it cannot.
- Be safe to re-run, like `CarbonStorageLicenceMigrationService.migrateSchedules()`.

### Scope: licence types and statuses

**Decided:** the migration includes every PEARS licence schedule where one exists, on **every
licence type LMS doesn't manage itself** (`LicenceType.managedByLms = false`), **regardless of
licence status**. Those types are:
- `SEAWARD_PRODUCTION` (`P`);
- `LANDWARD_PRODUCTION`, which `getFromPrefix` maps from `PEDL`, `PL`, `EXL`, `ML`, `AL`, `CE` and
  `DL`.

**LMS change (LMS1-774, November 2026):** the placeholder types (`A`, `AL`, `B`, `CE`, `DL`, `NA`,
`XL`) have been removed from `LicenceType`. `AL`, `CE` and `DL` map to `LANDWARD_PRODUCTION`, and
`getFromPrefix` now **throws** for `A`, `B`, `NA` and `XL`. None of those has a PEARS schedule (S17),
so nothing in scope is lost, but the PEARS read must only return licences whose prefix maps, or a
stray licence would fail the run. `LicenceType` is also now release-phase gated (`ReleaseFeature`):
both production types are `CURRENT_LICENCE_TYPE`, so migrated schedules are visible in every
environment.

LMS-managed types (carbon storage, gas storage, landward and seaward exploration, methane drainage)
are excluded and have no PEARS schedules anyway. Every licence type that has a PEARS schedule maps
to a PEARS-managed type. On live (27 October 2026;
data survey S2 and S18) that is 1,799 licences and 10,834 schedule versions:

| PEARS type | Licences | Versions | LMS `LicenceType` (`LicenceType.getFromPrefix`) | LMS term types today |
|---|---|---|---|---|
| `P` | 1,525 | 8,312 | `SEAWARD_PRODUCTION` | Initial, Second, Third |
| `PEDL` | 220 | 1,927 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `PL` | 26 | 336 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `EXL` | 12 | 91 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `ML` | 7 | 80 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `DL` | 5 | 53 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `AL` | 3 | 30 | `LANDWARD_PRODUCTION` | Initial, Second, Third |
| `CE` | 1 | 5 | `LANDWARD_PRODUCTION` | Initial, Second, Third |

- **Status:** extant, surrendered, expired, revoked and no-status licences are all migrated. The
  report shows each licence's status, so skipped live licences stand out. Of
  the `P` and `PEDL` licences, only 381 of 1,745 are extant (S18). Of the other 54 typed licences,
  39 are extant (S18), including 6 of the 9 `AL`/`DL`/`CE` licences.
- **`PL`, `EXL`, `ML`, `AL`, `DL`, `CE`** map to `LANDWARD_PRODUCTION` in LMS. `AL`, `CE` and `DL`
  were LMS placeholder types with no term types until 28 October 2026, when LMS mapped them to
  `LANDWARD_PRODUCTION` (`LicenceType.getFromPrefix`, plus the
  `map_al_ce_dl_licences_to_landward_production.sql` manual script for existing rows). So LMS can
  now create and maintain schedules on all of them (Open question 14, resolved). `EXL`, most `PL`
  and the 3 `AL` migrate exactly like `PEDL`, with `RD` area events (onshore). No `ML`, `DL` or
  `CE` schedule has terms, and nor do 5 `PL` (rule 7); the licensing team can now create those in
  LMS.
- **Schedules with no terms** (all `ML`, `DL` and `CE`, 5 `PL`, 22 `P`) are not migrated: they are
  reported, and the licensing team creates them in LMS manually ("Term boundaries" rule 7).
- **Data survey coverage:** the survey has been re-run for the other types, and the results are
  merged into each section. Done for every section (27 October 2026).

## Source: how PEARS holds a schedule

### Tables

| Object | Purpose |
|---|---|
| `pedmgr.ped_licence_master` | Licence (`id`, `licence_type`, `licence_no`, `ped_ts_id`). The LMS licence reference is `licence_type \|\| licence_no`. |
| `pedmgr.ped_licence_schedule_details` | One row per **saved version** of a licence's schedule: `ped_lm_id`, `start_datetime`, `end_datetime` (null = current), `created_by`, `xml_data`. `template_schedule.update_licence_schedule` end-dates the current row and inserts a new one on every save. |
| `pedmgr.ped_template_event_types` | The event type catalogue (`mnem`, `short_title`, `display_seq`, `active_offshore`, `active_onshore`, `recurrable`, `acknowledgeable`). Rows can be added by users through the Fox Event Type Search screen (`PED013X`), so live can hold types that dev does not. |
| `pedmgr.xview_ped_lic_schedule_events` | Xview that already flattens `xml_data` to one row per event, for every version (columns below). This is the preferred read source. |

### Schedule XML (`ped_licence_schedule_details.xml_data`)

```
SCHEDULE
  ORIGIN_DATE                 -- licence start date (may be missing on migrated licences)
  RETENTION_AREA_RATE         -- onshore only
  EVENT_LIST/EVENT*
    EVENT_MNEM                -- T, P, R, MR, E, BM, DD, 2M, 2N, RD, PA ...
    TERM_NO | PHASE | RATE | PERCENTAGE
    YEARS, MONTHS, DAYS       -- offset from ORIGIN_DATE
    EVENT_DATE                -- the date PEARS calculated and displays
    RECURRING, RECURRANCES    -- e.g. YEARLY, n
    COMMENTS                  -- free text; for BM it holds the obligation wording
    EVENT_SUBTYPE, EVENT_ID, RETENTION_AREA/*, DUMMY_EVENT, EDITABLE, SELECTED, ACTIONS
```

`xview_ped_lic_schedule_events` exposes: `plsd_id`, `ped_lm_id`, `start_datetime`,
`end_datetime`, `origin_date`, `event_mnem`, `term_no`, `phase`, `years`, `months`, `days`,
`event_date`, `recurring`, `recurrances`, `recur_end_date`, `rate`, `comments`,
`relinquishment_percentage`, `event_subtype`, `event_id`, `retention_area`, `retention_area_rate`,
`dummy_event_flag`.

`DUMMY_EVENT='true'` rows are UI padding generated by `PED012X` (`qry-populate-dummy-data`). They are
stripped on load and must be excluded from the migration.

### Date arithmetic in PEARS

`pedmgr.template_schedule.get_julian_or_event_date(years, months, days, origin_date)`:

```sql
ADD_MONTHS(origin_date + days, years * 12 + months)
```

Days are added **first**, then months. Oracle `ADD_MONTHS` snaps to the last day of the month when
the input is the last day of a month.

### Event type catalogue

`pedmgr.ped_template_event_types` holds 11 types: `P`, `T`, `R`, `MR` (fixed grid columns,
`display_seq` < 0) and `E`, `PA`, `BM`, `DD`, `2M`, `2N`, `RD` (generated columns). `RD` is onshore
only and `PA` offshore only. The full query and result are in the data survey,
[S1](pears-schedule-migration-data-survey.md#s1-event-type-catalogue).

The migration must **re-read this table at run time** and fail on any `event_mnem` it has no
mapping for, rather than hard-coding the list, since users can add types on live.

## Target: the LMS schedule model

- `LicenceSchedule` (one per licence) → `LicenceScheduleDetail` (versions: `DRAFT`, `ACTIVE`,
  `REPLACED`, `DELETED`; `createdInstant`).
- `LicenceStartDate` per detail.
- `ScheduleEvent` (joined inheritance, `schedule_events`, `original_event_id`) subclasses:
  - `LicenceScheduleTerm`: `TermType` (`INITIAL`, `SECOND`, `THIRD` for production, plus a new
    `FOURTH` on the 13 four-term licences only, see "Decision: four-term licences") + `termDuration`.
  - `LicenceSchedulePhase`: `PhaseType` (`PHASE_A/B/C`, initial term only) + `phaseDuration`, linked to its term.
  - `LicenceScheduleRate`: `RateDefinitionOption` (`TERM`, `PHASE`, `CUSTOM_PERIOD`), optional `RateRelativeDateOption` + `relativeDuration`, `rentalRate`.
  - `LicenceScheduleExpiry`: `expiryDate`, `comments`.
  - `OtherScheduleEvent`: `OtherScheduleEventCategory` (`MANDATORY_RELINQUISHMENT`, `OTHER_ACTIVITY` + `otherCategoryName`), `description`, `OtherScheduleEventDateOption` (`WITHIN_A_TERM`, `WITHIN_A_PHASE`, `RELATIVE_DATE` anchored on a term or phase + `relativeDuration`), `eventDate`.
  - `WorkProgrammeActivity` (not used by this migration, see Out of scope).
- `EventComment` (`PUBLISHED`) for comment history on any schedule event.
- `LicenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(detail)` derives every
  start/end/event date from the start date and durations.

### Date arithmetic in LMS

```java
// calculateRelativeStartDueDate
startDate.plusYears(y).plusMonths(m).plusDays(d)
// calculateDurationEndDate: same, then minus one day when years or months > 0
```

Months are added **first**, then days, the opposite order to PEARS. The two agree for most
schedules (for example licence P2244: start 01-Dec-2014, BM at 8y 11m 29d and 9y 3m 30d give
30-Nov-2023 and 31-Mar-2024 either way), but they diverge around month ends. For example:
start 30-Jan, +1m +1d is 28-Feb in PEARS (31-Jan then `ADD_MONTHS` snaps to month end) and
01-Mar in LMS. The migration therefore treats PEARS `EVENT_DATE` as authoritative and checks
the LMS result against it (see Validation).

## Mapping

| PEARS | LMS | Notes |
|---|---|---|
| licence `type \|\| no` | `Licence` by `licenceReference` | The licence must already exist (from the PEARS refresh); skip and report otherwise. |
| every `ped_licence_schedule_details` row for the licence | one `LicenceScheduleDetail` per version: the row with `end_datetime IS NULL` is `ACTIVE`, the rest `REPLACED`; `createdInstant = start_datetime` | **Decided: full history.** See "Schedule history". |
| `ORIGIN_DATE` (per version) | `LicenceStartDate.startDate` on **every** detail | **Decided:** the schedule start date is used, not PEARS's licence start date. The two can legitimately differ (7 current versions do, data survey S10b), and the difference is not reported. Every current version has one. 1,125 historical versions don't (`PED012X` initialises an empty one at raise): they borrow a neighbouring version's start date (see "Schedule history"). Only a licence where no version has one is skipped and reported. |
| `T` (`TERM_NO` 1/2/3, and 4 on 13 licences) | `LicenceScheduleTerm` `INITIAL`/`SECOND`/`THIRD`, and `FOURTH` (see "Decision: four-term licences") | **Decided (licence P2244):** the first term present starts on `ORIGIN_DATE`; terms may be missing but must be in order (business rule). A `T` event with `TERM_NO` n marks the **end boundary of term n**: the anniversary on which the next term present starts. Term n's duration = `Period.between(start of term n, T(n) date)`. LMS then ends term n the day before and starts the next term present on the `T(n)` date. A `T` with no `TERM_NO` is ignored. See "Term boundaries". |
| `P` (`PHASE` A/B/C) | `LicenceSchedulePhase` `PHASE_A/B/C` linked to the initial term | **Decided (licence P2522):** same boundary convention as terms. The first phase present starts on `ORIGIN_DATE`. A `P` event's `PHASE` value names the phase that **ends** on its date, and the next phase present starts on that date. Phases may be missing but must be in order (business rule): P2522 has A and C, with no B ("Continuation into Phase C"). The last phase's `P` date must equal `T(1)`; otherwise skip and report. See "Phase boundaries". |
| `R` (`RATE`, `RECURRING=YEARLY`, `RECURRANCES`) | `LicenceScheduleRate`, one per term/phase the rate's yearly dates fall into | **Decided:** `RECURRING=YEARLY` means the rate is redefined every year at the same value. The yearly dates are expanded, and each is migrated against the phase (or, outside phases, the term) whose dates contain it. See "Rates and yearly recurrence". `rentalRate` = `RATE` as a decimal, rounded to 2 dp where needed; values that do not fit are reported (see "Rate values"). |
| `MR` (`PERCENTAGE`) | `OtherScheduleEvent` `MANDATORY_RELINQUISHMENT`, `RELATIVE_DATE` against the containing term, `description = COMMENTS`, or the default "Mandatory relinquishment" when `COMMENTS` is empty (see "Default descriptions") | **Percentage: decided, migrated** into a new LMS percentage field on the event (0–100, up to 1 dp), and written back. PEARS holds it nowhere else, and it feeds generated licence documents. See "Decision: mandatory relinquishment percentage". |
| `E` (optional) | (1) confirms the **end boundary of the final term** (the final `T` sets it), and (2) `LicenceScheduleExpiry` with `comments = COMMENTS` | **Decided:** the expiry event marks the end of the final term. On P2244 it shares its date with the final `T` (01-Dec-2040), and the final term's LMS end date is `E - 1 day` (30-Nov-2040). **`expiryDate`: `E - 1 day` (30-Nov-2040) for now (Open question 8)**, because LMS checks that the expiry date equals the final term end. **No `E` event:** migrated without an expiry record (see "Term boundaries" rule 3). An `E` whose date differs from the final `T`: migrated as it is (no extra term), and reported (see "Term boundaries" rule 3). |
| `BM` | `OtherScheduleEvent` with a **new dedicated category** `BESPOKE_MILESTONE` ("Bespoke milestone"), `description = COMMENTS` (default "Bespoke milestone" when empty), `RELATIVE_DATE` against the containing term with `relativeDuration` = offset from that term's start | **Decided.** Prerequisite: add `BESPOKE_MILESTONE` to `OtherScheduleEventCategory` (production licence types), so the category is selectable in the UI and `getCategoryString()` returns its display name. |
| `DD`, `2M`, `2N` | `OtherScheduleEvent` `OTHER_ACTIVITY`, `otherCategoryName = ped_template_event_types.short_title` ("Drill-or-drop", "Second Term with autosurrender", "2nd term with all acreage"), `description = COMMENTS` (default: the same `short_title` when empty), `RELATIVE_DATE` as for `BM` | **Decided.** On the timeline, `getCategoryString()` returns `otherCategoryName`, so the PEARS title is the caption. |
| Any other user-defined type with `display_seq >= 0` | Same as `DD`/`2M`/`2N` (`OTHER_ACTIVITY` + `short_title`) | None on live (data survey S1). The migration re-reads the catalogue at run time and fails on any type it has no mapping for, so a type added later is caught before it is migrated. |
| Empty `EVENT_MNEM` | Not migrated | **Decided:** ignored, whether empty or holding data, and only counted in the report. The schedule screen never shows them, so the licensing team never sees them. See "Events with no event type". |
| `RD` (onshore), `PA` (offshore) | **Not migrated as schedule events.** Copied as they are into a new LMS table, `licence_schedule_area_events`, which the LMS schedule looks up for display | **Decided:** migrated in this exercise. Phase 1 copies the events as they are in the PEARS XML (subtype, date, comments, `EVENT_ID`, area block). The area records (`ped_retention_areas`) and work programmes come in a later phase. The write-back writes the `RD`/`PA` events back from the LMS table. See "Retention, development and pre-producing areas". |
| `DUMMY_EVENT='true'` | Not migrated | UI padding. |
| `COMMENTS` on T/P/R | LMS event comments: `EventComment` `PUBLISHED`, `timestamp` = the PEARS version's `start_datetime` | **Decided:** written to LMS comments. Same as `buildWorkProgrammeActivityComments` in the CS migration. Comments attach to the event's `original_event_id`, which is shared across versions. So a comment is only added when the event's `COMMENTS` text is **new or different** from the matched event in the previous version. The LMS comment history then shows when each PEARS comment first appeared or changed, without repeats. LMS-only from then on: event comments are not written back to PEARS (not reported on). |
| `RETENTION_AREA_RATE` | Not migrated | **Unused in PEARS (checked on live, October 2026):** no schedule version, current or historical, has ever held a value. The query is under "Retention/development area rate: unused". Nothing to migrate (including with the retention areas), and the PEARS write-back leaves it empty. |
| Acknowledgements (`ped_licence_acknowledgements`) | Not migrated | Out of scope. The write-back keeps them working (see "PEARS write-back: regenerating the schedule XML"). |

### Offset models: absolute in PEARS, chained/anchored in LMS

- **PEARS:** every event (terms included) is an offset from the single `ORIGIN_DATE`. Term 2 is
  "n years from the licence start", not "n years after Term 1".
- **LMS terms and phases chain:** each has a duration. The first term starts on the licence start
  date and each later term starts the day after the previous one ends (`endDate.plusDays(1)`). Phases chain
  the same way within the initial term.
- **LMS rates, other events and work programme activities are anchored:** they are relative to the
  start of a chosen term or phase (`RELATIVE_DATE` / `CUSTOM_PERIOD`), or fall on its end
  (`WITHIN_A_TERM` / `WITHIN_A_PHASE`). They are never relative to the previous event.

Conversion:

1. The first term present starts on `ORIGIN_DATE`. Term n's duration = `Period.between(start of
   term n, T(n) boundary date)` (see "Term boundaries"). Because LMS ends a term at `start + duration - 1
   day` and starts the next one the day after, the next term starts exactly on the PEARS boundary.
2. Every other event is re-expressed as an offset from the start of the term (or phase) it falls in.
3. Because terms chain, one term duration that does not reproduce its PEARS date shifts every later
   term **and** every event anchored to them. Validation checks term and phase boundaries first,
   then events.

### Term boundaries

PEARS dates terms by their **end boundary**, not their start. Licence P2244 (`ORIGIN_DATE`
2014-12-01) has no `T` at the licence start. Its `T` events are:

| `TERM_NO` | PEARS date | Meaning | LMS term |
|---|---|---|---|
| (none) | 01-Dec-2014 (`ORIGIN_DATE`) | Licence start | Initial starts 01-Dec-2014 |
| 1 | 01-Dec-2018 (4y) | Initial term ends; second term starts | Initial 4y, ends 30-Nov-2018; Second starts 01-Dec-2018 |
| 2 | 01-Dec-2028 (14y) | Second term ends (after 1 + 2 + 3 years of extensions, per its comments) | Second 10y, ends 30-Nov-2028; Third starts 01-Dec-2028 |
| 3 | 01-Dec-2040 (26y) = `E` | Third term and licence end | Third 12y, ends 30-Nov-2040 |

**Business rule (confirmed):** a licence schedule does **not** need every term. For example, a
licence can have only Term 2 and Term 3. But the terms that exist must be **in order**: term numbers
increase with their dates.

Rules:

1. Only terms with a `T` event are created. The **first term present** starts on `ORIGIN_DATE` (the
   `LicenceStartDate`), and each later term present starts on the previous `T` boundary. Examples:
   - `T(1)`, `T(2)`, `T(3)` (licence P2244): Initial, Second, Third.
   - `T(2)`, `T(3)` = `E` (49 recent `P` licences, data survey pattern A): **Second** from
     `ORIGIN_DATE` to `T(2)`, **Third** from `T(2)` to `T(3)`. No Initial term.
   - `T(1)`, `T(3)` = `E` (pattern F): Initial to `T(1)`, then **Third** from `T(1)` to `T(3)`. No
     Second term.

   LMS supports this already. `LicenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates`
   chains whichever terms exist, in `TermType` order, from the licence start date. The only LMS checks
   that need an Initial term are the phase checks (`ScheduleRelativeDateValidationService`, which
   looks up `TermType.INITIAL` when phases exist). So a schedule **with phases but no `T(1)`** is a
   data error. None exist on live (S8c: no schedule without `T(1)` has phases).
2. Term n's duration = `Period.between(start of term n, T(n) date)`. LMS ends a term on
   `start + duration - 1 day` (`calculateDurationEndDate`) and starts the next one the day after. So
   the next term starts exactly on the `T(n)` anniversary, and every boundary matches PEARS.
3. The final term's boundary is its own `T` date, so the final term can be built **with or without
   an `E` event**. Licences without an `E` exist in PEARS and are migrated, not skipped: the final
   term ends the day before its `T` date and no `LicenceScheduleExpiry` is created. LMS treats the
   expiry as optional: the timeline shows it blank, and the Review and Apply check
   `doesExpiryDateMatchEndOfFinalTerm` passes when there is none. Each such licence is listed in the
   report.

   **Decided:** when an `E` exists and its date **differs from the final `T`**, the licence is
   **migrated as it is and reported**. No extra term is added to fill the gap. The final term ends
   on its own `T` boundary, and the `LicenceScheduleExpiry` takes the `E` date (under the Open
   question 8 convention). The report lists each case with the licence, version, final `T` date and
   `E` date. On live, this is data survey pattern B: about 30 `P`/`PEDL` schedules, mostly 1980s
   licences (e.g. P516: `T(1)` 1991-06-14, `E` 2021-06-14), plus PEDL141 and P1159. There are also
   13 `PL` and 2 `EXL`, mostly old onshore licences with only a `T(1)` and an `E` decades later
   (e.g. PL116: `T(1)` 1975, `E` 2045).

   Because migration writes directly, LMS's validation doesn't run. But
   `doesExpiryDateMatchEndOfFinalTerm` will **fail at Review and Apply** the next time a user edits
   one of these schedules in LMS. The user then has to correct the terms or the expiry before the
   draft can be applied. That is intended: the difference is surfaced rather than hidden.

   **Under review (Open question 16, for the regulators):** with no term after the last `T`, every
   rate from that `T` onwards falls outside the terms and is not migrated, including the current
   rent (found on PL211). Whether to add the next term up to `E` instead is for the regulators to
   decide. Until they do, no term is added. Licences with no `E` lose their rates after the last
   `T` the same way (159 on live, found on P255), and are part of the same question.
4. A days-only period (no years or months) has no minus-one-day in `calculateDurationEndDate`. Use
   `Period.between(start, T(n) date - 1 day)` there, so the end still lands on the day before the
   boundary.
5. Data errors (skip and report): duplicate `TERM_NO`s, `T` dates **out of order** (a higher term
   number on an earlier or equal date), a `T(n)` on or before the start of term n, a term number
   outside 1–4, and phases without a `T(1)`. Missing term numbers are **not** errors (business
   rule above). `T(4)` maps to the Fourth term (see "Decision: four-term licences").
6. **Decided:** a `T` event with no `TERM_NO` is **ignored**. It creates no term, takes no part in
   the term rules and is not migrated. Like untyped events, it is only counted in the report, not
   listed. On live, these are 15 schedules
   with a start marker dated on `ORIGIN_DATE` (data survey pattern D: P2313, P2322, P2326, P2327,
   P2331, P2332, P1159, and PL205, PL211, PL213, PL215, PL220, PL233, PL234, PL259). Once it is
   ignored, their numbered terms are in order and fit the rules above.
7. **Decided:** a schedule with **no numbered `T` event is not migrated.** It is reported as
   **"No terms in PEARS"**, with the licence status, and creating its LMS schedule is a **manual
   step for the licensing team**. LMS anchors every rate and other event to a term or phase, so a
   migrated schedule could only hold a start date and expiry, and the write-back would then delete
   the PEARS rates and events. Until a schedule is created in LMS, the licence has no
   `ped_lms_schedule_sync` row, so PEARS keeps owning its schedule.

   On live (data survey pattern E, S8 and S8b) there are 40:
   - **22 `P`** rate-only schedules (14 extant, 7 surrendered, 1 expired). Only 3 have an `E`
     (S8d).
   - **All 7 `ML`, all 5 `DL`, CE1 and 5 `PL`** (PL89, PL199, PL240, PL241, PL249). 17 of these 18
     are extant (all but DL5), and all but CE1 have an `E`.

### Decision: four-term licences

**Decided.** 13 `P` licences from the 2004–2009 rounds have four terms (data survey pattern C):
P1162, P1165, P1368, P1373, P1374, P1484, P1485, P1487, P1488, P1636, P1638, P1640, P1641. For
example, P1484 is `1@2009-04-01, 2@2013-04-01, 3@2019-04-01, 4@2037-04-01`.

LMS will support a **Fourth term**, but **only on licences whose schedule already has one**. A
schedule without a Fourth term can never have one added or maintained.

- **Mapping:** `T(4)` ends the Fourth term, using the same rules as the other terms. The Fourth term
  starts on the `T(3)` boundary. As on any schedule, the final `T` (here `T(4)`) should equal `E`.
  Where it doesn't, rule 3 applies (migrated as it is and reported).
- **LMS model:** add `TermType.FOURTH` ("Fourth Term", display order 40) for
  `SEAWARD_PRODUCTION` and `LANDWARD_PRODUCTION`. `term_type` is `TEXT` with no check constraint
  (`V12`), so no schema change is needed. `LicenceScheduleCalculationService` chains terms by
  display order, so it handles the Fourth term with no change.
- **Gating:** today the term options come from `TermType.getTermRadioOptionsFor(licenceType)` (used
  by `LicenceScheduleTermController`) and `LicenceTypeFeatureService.getTerms`. Both list every
  term for the licence type. `FOURTH` must be left out unless the licence's **active** schedule
  version has a Fourth term. The check uses the active version, not the draft, so a user who
  removes the Fourth term in a draft can still add it back before applying. Once a version without
  a Fourth term is applied, the option goes for good.
- **Only the migration creates it.** No LMS journey can add a Fourth term to a schedule that
  doesn't already have one.
- **Write-back:** `TERM_NO` 4, as in PEARS today.
- **Check before building:** other code that branches on `TermType` needs to handle `FOURTH` or
  ignore it. This includes event reminders (`TermOrPhaseEndDeadlineServiceEvent`), the extension and
  record-of-decision journeys (`LicenceScheduleExtensionService`, `RecordDurationChanges*`), the
  continuation visibility rules (`OtherRequirementsVisibilityResolverService`) and
  `LicenceTypeFeature`.

### Which term an event belongs to

Everything dated from `ORIGIN_DATE` up to and **including** the first term's boundary (`T(1)` on
most schedules) belongs to that term. The same applies to each later term up to its own boundary. Each event is anchored to
its term with `RELATIVE_DATE` / `CUSTOM_PERIOD` and `relativeDuration = Period.between(term start,
event date)`.

- An event **on** a boundary (on P2244: the "50% Relinquishment" and the rate of 300, both
  01-Dec-2018) belongs to the term that **ends** there. It is anchored to the initial term with the
  term's full duration as its offset (Initial + 4y = 01-Dec-2018). LMS only checks that a relative
  date falls before the end of the **final** term (`ScheduleRelativeDateValidationService`), so an
  offset reaching the anchor term's boundary is allowed.

  The exception is a **rate** on a boundary: it starts the next term (`RateDefinitionOption.TERM` on
  the next term). In LMS a rate is in force from its start date onwards, so a rate of 300 "starting
  the second term" is what the schedule means.
- An event dated after the final term's LMS end date (the day before the final `T`) cannot be created through the
  LMS UI. If any exist apart from `E` itself, the migration still creates them (it bypasses form
  validation) but lists them in the report.

### Default descriptions

**Decided:** every migrated `OtherScheduleEvent` gets a description, because the LMS form requires
one and an empty description reads badly on the timeline. When `COMMENTS` is empty or only
whitespace, the mapper uses a default:

| PEARS type | Default `description` |
|---|---|
| `MR` | "Mandatory relinquishment" |
| `BM` | "Bespoke milestone" |
| `DD`, `2M`, `2N` and any other `OTHER_ACTIVITY` type | its `ped_template_event_types.short_title` (e.g. "Drill-or-drop") |

`COMMENTS` is trimmed before the check. Each default applied is recorded as a migration note, and
the comparison report marks those rows "default description", so the licensing team can see
which descriptions did not come from PEARS. Example: licence P2522's relinquishment on 01-Dec-2027
has no percentage and no comments, so it becomes "Mandatory relinquishment".

### Phase boundaries

Phases use the same end-boundary convention, inside the initial term. Licence P2522
(`ORIGIN_DATE` 2020-12-01):

| `P` event | PEARS date | Meaning | LMS phase |
|---|---|---|---|
| (none) | 01-Dec-2020 (`ORIGIN_DATE`) | Licence and phase A start | Phase A starts 01-Dec-2020 |
| `PHASE` A | 01-Dec-2023 (3y) | Phase A ends (2y + "Direction 4/5/22 Phase A, 12 mth extension") | Phase A 3y, ends 30-Nov-2023 |
| (no B) | — | "Continuation into Phase C: letter sent 23/11/23", so phase B is skipped | no `PHASE_B` row |
| `PHASE` C | 01-Dec-2024 (4y) = `T(1)` | Phase C and initial term end ("Phase C preserved") | Phase C 1y, 01-Dec-2023 to 30-Nov-2024 |

**Business rule (confirmed):** as with terms, not every phase needs to exist (e.g. only Phase A and
Phase C), but the phases present must be in order: A, B, C by date.

Rules:

1. The first phase present starts on `ORIGIN_DATE`. Each `P` event's `PHASE` value names the phase
   ending on its date. Its duration = `Period.between(phase start, P date)`, and the next phase
   **present** starts on that date.
2. Only phases with a `P` event are created. `LicenceScheduleCalculationService` chains the phases
   that exist in `PhaseType` order (`calculateAndSavePhaseDatesForTerm`), so a missing `PHASE_B`
   lets C start the day after A ends, which is what P2522 needs.
3. The last phase's `P` date must equal the `T(1)` date, so the phases exactly fill the initial
   term. Otherwise, or if the `PHASE` values are out of order, duplicated, empty or not A/B/C, skip
   the licence and report it. (A phase that is simply absent is allowed, per the business rule.)
   Live data passes on every schedule with phases (data survey, S9).
4. A rate on a phase boundary starts the next phase (`RateDefinitionOption.PHASE`). Other events on
   a phase boundary are anchored to the term (see "Which term an event belongs to"), not the phase.

### Schedule history

Every saved PEARS version becomes an LMS `LicenceScheduleDetail`, as the CS migration does with one
detail per case date (`cs-migration-schedule-details-per-case.md`).

- **Order and status.** Versions are ordered by `start_datetime`. The version with
  `end_datetime IS NULL` is `ACTIVE`, every earlier one `REPLACED`. A licence with more than one
  open version is a data error: skip and report it (none on live).
- **No current version (decided, 28 October 2026).** A licence whose versions are all end-dated is
  still migrated: its **latest** version (by `start_datetime`) becomes `ACTIVE`, the rest
  `REPLACED`, and it is reported as "No current version in PEARS". The usual rules still apply to
  each version (a historical one that breaks them is left out, see below). Only P1161 on live (2 versions, the last end-dated 14/10/2011; licence surrendered
  30/11/2012; data survey S2b).
- **Each version is complete.** A PEARS version holds the whole schedule, not a delta, so each
  detail gets its own start date, terms, phases, rates, expiry and other events, all built by the
  same mapper. (Unlike the CS migration, nothing is carried forward from an earlier case.)
- **Dates for every version.** `calculateAndSaveLicenceScheduleDates` needs a `LicenceStartDate`
  on the detail (`getByLicenceScheduleDetailOrThrow`). The CS migration only calculates dates for
  the ACTIVE detail. This migration creates a start date and runs the calculation for **every**
  detail, so replaced versions show real dates in the schedule history.
- **Same event across versions (`original_event_id`).** LMS identifies an event across versions by
  `ScheduleEvent.originalEventId` (set to its own id on first insert by `@PrePersist`). Comment
  history (`EventComment` is keyed on it) and the cross-licence event tracker
  (`LicenceEventCache.originalEventId`) both depend on it. PEARS has no stable event id
  (`EVENT_ID` is only set on `RD`/`PA` events), so the mapper matches each event to the previous
  version's events, in this order:
  1. same `EVENT_ID`, when both have one (all `RD` and most `PA` events);
  2. terms and phases: same `event_mnem` and `TERM_NO` / `PHASE`;
  3. `E`: the single expiry event;
  4. everything else: same `event_mnem` and the same offset (`YEARS`/`MONTHS`/`DAYS`);
  5. if the offset changed (e.g. a date moved by an extension): same `event_mnem` and the same
     `COMMENTS`, only when that comment is unique for the type within both versions.

  A match takes the earlier event's `originalEventId`. No match means a new event (its own id).
  Built in PR 4 (`EventKeys`): each mapped event carries an `eventKey`, shared by matched events
  across versions, and the write step gives events sharing a key one `originalEventId`. Each version
  is matched to the last version that was mapped, so a left-out version is skipped. Under option A a
  PEARS rate becomes several LMS rates: each is matched to the previous version's LMS rate from the
  matched `R` event in the same term or phase.
  **Live data (data survey,
  [S16](pears-schedule-migration-data-survey.md#s16-event_id-and-ambiguous-cross-version-matches)):**
  type + offset never repeats within a version, but type + comments often does (e.g. 472 groups of
  rates sharing a comment). That is why offset comes before comments. Any pair still ambiguous is
  matched in date order and listed in the report.
- **One LMS version per PEARS edit (decided).** PEARS writes a new `ped_licence_schedule_details`
  row on every save (`template_schedule.update_licence_schedule`), and each one becomes its own
  `LicenceScheduleDetail`, including saves that changed nothing. Versions are never merged, so
  LMS history mirrors PEARS one to one. (Unchanged re-saves were not counted separately; the
  sizing in "Migration size" counts every version.)
- **Older versions with no `ORIGIN_DATE`.** **Decided:** migrate them. If a replaced version has
  no `ORIGIN_DATE`, use the next version's start date, then the earliest that exists, and record
  the substitution. If no version has one, skip the licence. On live (27 October 2026) that is
  1,125 historical versions (1,004 `P`, 67 `PEDL`, 54 other types); every current version has one (data survey,
  [S10](pears-schedule-migration-data-survey.md#s10-origin_date-all-versions)). PEARS calculated
  these versions' `EVENT_DATE`s from its `1900-01-01` base, so they are not real dates. The
  comparison report marks every event in such a version **"No start date in PEARS"** rather than
  comparing dates, and the validation test counts them separately rather than as differences.
  Their dates are worked out from the offsets with PEARS's arithmetic. Almost all have their offsets
  set (data survey
  [S19](pears-schedule-migration-data-survey.md#s19-historical-versions-the-mapper-cannot-map)).
- **Versions with stale dates (decided, 17 November 2026).** About 1,113 historical versions
  (949 `P`, 163 `PEDL`, plus P1622) **have** an `ORIGIN_DATE`, but their stored term dates are on or
  before it: PEARS worked them out from its 1900 base and didn't recalculate them once the start
  date was set. Their offsets are right: worked out from the version's own start date, every one
  puts its terms in order (S19). So when a version's numbered `T` events are dated on or before its
  start date, the mapper ignores **all** the version's stored dates and works every event's date out
  from its offset, as for a version with no start date, and records it. The comparison report marks
  these versions "Dates worked out from offsets" rather than comparing dates. Without this rule,
  1,072 licences would fail on their history alone; with it, about 30 still have a version that
  can't be mapped (below).
- **Historical versions that still can't be mapped (decided, 17 November 2026).** A **historical**
  version that breaks a rule (no numbered term, terms out of order, a repeated or invalid term
  number, an expiry with no date, and so on) is **left out** of the licence's LMS history. The
  licence still migrates with its other versions, and the report lists each version left out with
  the rule it breaks. The **active** version is never left out: if it can't be mapped, the licence
  isn't migrated. On live this is about 30 licences and 75 versions (S19): 13 `P` versions whose
  offsets are out of order too, 8 `P` versions with no start date and zero offsets, the early
  versions with no terms on PL178, PL179, PL182 and PL235 (46), and a handful of one-offs (repeated
  term numbers, PEDL126's term number outside 1–4, P1159's undated expiry). Because PEARS saves a
  whole schedule each time, leaving a version out loses nothing from the versions either side of it.
- **Cross-licence event tracker.** After the details are saved and dated, call
  `CrossLicenceEventTrackerService.refreshScheduleCache(activeDetail)` for each migrated licence,
  so the tracker sees migrated schedules the same way as ones created in LMS.

### Rates and yearly recurrence

A PEARS `R` event with `RECURRING=YEARLY` redefines the same rate every year from its date. LMS has
no recurrence: a `LicenceScheduleRate` is a start date and a value, and it applies until the next
rate starts (or the final term ends). LMS rates are anchored to a term or phase.

**1. Expand the PEARS occurrences.** Occurrence *k* (k = 0, 1, 2 …) is the PEARS date for
`YEARS + k`, `MONTHS`, `DAYS`, using **PEARS arithmetic** (`ADD_MONTHS(origin + days, (years + k) * 12 + months)`).
This is how `PED012X` places recurrences (`qry-populate-dummy-data`: `d.years BETWEEN ped2.years AND
ped2.years + ped2.recurrances`). Port it to Java once, including `ADD_MONTHS`'s last-day-of-month rule,
and unit test it against Oracle results. Stop at the first of:
- `k > RECURRANCES` (when `RECURRANCES` is set; empty means indefinite);
- `RECUR_END_DATE`, when set;
- the date of the next `R` event (a later rate supersedes it);
- the end of the final term (its final `T` date, which is the `E` date except on the schedules
  under "Term boundaries" rule 3).

A non-recurring `R` is a single occurrence (k = 0).

**2. Place each occurrence in a period.** Use the phase whose start/end dates contain it. A date
outside any phase (second and later terms, or an initial term without phases) uses the containing
term. Periods come from the terms and phases already derived for the same version.

**3. Create LMS rates.**
- The first occurrence that falls in each period becomes a `LicenceScheduleRate` anchored to that
  period. How it is anchored follows LMS's own rules (`LicenceScheduleCalculationService`): a
  `TERM` or `PHASE` rate starts **and ends** with its period, while a `CUSTOM_PERIOD` rate runs
  until the next rate starts. So:
  - on the period's start date, **and** the next LMS rate starts on the period's end (or there is
    none and the period is the last): `RateDefinitionOption.PHASE` / `TERM`;
  - on the period's start date otherwise: `CUSTOM_PERIOD` + `ON_START_DATE`, so it stays in force
    until the next rate, inside the period or after it;
  - after it: `CUSTOM_PERIOD` + `RELATIVE_TO_START_DATE` against that phase or term, with
    `relativeDuration` worked out so LMS's arithmetic (years, then months, then days) lands on the
    occurrence date (`LmsScheduleDurations.untilDate`). That is `Period.between(periodStart,
    occurrenceDate)` except from a 29 February.
- Later occurrences of the **same rate in the same period** do not create extra rows: the LMS rate
  already applies until the next rate. So a yearly rate spanning the initial term's phases and the
  second term becomes one LMS rate per phase/term it touches, each at the same `rentalRate`.
- An occurrence is placed from the rate's offset with the years raised by `k`, as `PED012X` does,
  when the rate's date is the one its offset gives. Otherwise (no offset, or a stored date that
  differs from it) the occurrences are a year apart from the rate's date, by `ADD_MONTHS`.

  **Decided for now (Open question 7, 17 November 2026): option A**, one LMS rate per phase/term
  the yearly rate touches. Built in PR 3 (`PearsRateMapper`). If the regulator wants option B, only
  this step changes: one LMS rate per occurrence instead of one per period.

**4. Gaps.** If a recurrence stops (`RECURRANCES` or `RECUR_END_DATE`) before the next rate or the
final term end, PEARS has a period with no rate and LMS would carry the rate on. The same applies to a
**non-recurring** `R` followed by one or more anniversaries with no `R` at all. Report each such gap as
a migration note rather than guessing. On live there are about 310 on migrated schedules, all deliberate-looking
no-payment years: 298 on `P` and 12 on `PL` (data survey,
[S12b](pears-schedule-migration-data-survey.md#s12b-rate-gaps-one-row-per-gap)).

**How gaps are handled depends on what they are. The licensing team confirms which, per pattern:**

- **(a) Errors** in how those schedules were managed at the time: the previous rate should have
  run on through each gap year. The migration needs nothing extra. The LMS rate stays in force
  until the next rate, and the write-back fills the gap years in PEARS, which corrects them.
- **(b) Legitimate no-payment years:** LMS uses a **£0 rate** for them. LMS already accepts a rate
  of 0. The migration adds a £0 `LicenceScheduleRate` on the first anniversary with no payment,
  anchored to the period it falls in, and the next PEARS rate ends it. The write-back writes it back
  as a yearly rate of 0 until the next rate, so the payments report pays nothing in those years, as
  today. Users defining schedules in LMS use the same £0 rate for any future gap. The data
  warehouse owners should confirm a 0 rate row is acceptable where PEARS had no row.

Either way, LMS's "in force until the next rate" model then describes every year correctly, so the
write-back can rebuild yearly recurrence without inventing payments.

**Agreed with the NSTA (L12, 20 November 2026): (a), the gaps are errors**, pending the NSTA's
final check. It was the working assumption from 17 November 2026, so the mapper already does it. The
mapper treats every rate
as in force until the next rate or the final term's end, whatever PEARS's recurrence says: its
yearly occurrences carry on through the gap years, so a gap that crosses into a new phase or term
gives that period its own LMS rate, on the anniversary. Each gap is noted ("Years with no rate in
PEARS: the previous rate is carried on through them"), so the migration report lists them. If the
NSTA's final check finds any are legitimate after all, (b) adds a £0 rate on each such gap's first
missing anniversary, in the same place.

Still to settle with the NSTA's final check:

- **Past years gain payments in PEARS.** Once the write-back writes these schedules back, the gap
  years carry the previous rate, so PEARS's payments report and the data warehouse feed show rent
  in years that show none today. The gaps are mostly before 1999. The NSTA confirms that is
  intended for past years too, or gives a date before which gaps are left as no-payment years
  (case (b) for those).
- **The list to check.** The "L12" sheet of `pears-schedule-migration-licensing-items.xlsx` is from
  the survey's S12b query on live of 27 October 2026; run it again on live for the final check.
  After a migration run, each licence's "Years with no rate in PEARS" note gives the same list from
  the migration's side.
- **The same view may apply to Open question 16**, rates after the last term, which are dropped
  rather than carried on. Put both to the NSTA together.

Also noted, and otherwise handled the same way:
- a counted yearly rate that runs into the next rate (the `AL` overlap, step 5): it stops when the
  next rate starts;
- an indefinite yearly rate followed by another rate (P2644, survey S12b): it stops when the next
  rate starts.

**5. Fixed-count recurrences.** A yearly `R` with a `RECURRANCES` count needs no extra handling
when the count runs up to the next rate: LMS's "in force until the next rate" means the same, and
LMS does not store the count. On live, 9 `PL` and 3 `AL` schedules use a count (data survey S12b, S12c):

- **`PL` (e.g. PL116): contiguous.** `.5` ×2 from 1969 covers 1969 to 1971 and `1` starts in
  1972. One LMS rate per PEARS rate, and the write-back emits the same counts.
- **`AL` (AL6, AL9, AL10): the count overlaps the next rate.** On each licence PEARS pays `25`
  seven times, and the seventh falls in the year `50` starts (AL6 1999, AL9 2000, AL10 2001; e.g.
  AL10 `25` ×6 from 1995 covers 1995 to 2001). LMS holds one rate at a time, so `25` stops when
  `50` starts, and the write-back emits `25` ×5. The overlap is dropped. **The licensing team
  confirms the double payment was never intended** (open question 14; survey L16; the "AL rate
  overlap" sheet of the licensing items spreadsheet).
  These are 1999–2001 dates, so it only matters if the payments report is run for those years.

No count leaves a gap before the next rate, and no schedule ends on a counted rate.

### Rates and the PEARS write-back (separate piece of work)

LMS schedules will be written back to PEARS so data warehouse reporting keeps working from PEARS.
For rates, the consumer that matters is `ReportDefinitions/PED_LICENCE_PAYMENTS_REPORT.xml`. It
reads `xview_ped_lic_schedule_events` for the current version and treats:

- each **non-recurring** `R` row as a rental payment due on its `event_date`;
- each `RECURRING = 'YEARLY'` `R` row as a payment on every anniversary in the report window,
  bounded by `RECURRANCES` / `RECUR_END_DATE`.

An anniversary with neither kind of row produces **no payment**. A naive write-back of one
non-recurring `R` per LMS rate row would therefore drop every year between rate changes. For
licence P2522, rate 7500 would be paid in 2032 only, instead of every year to 2044.

How LMS should hold yearly-recurring rates is an **open decision**: see "Decision: how
yearly-recurring rates are held in LMS" below. Whichever option is chosen, rates created in LMS
after migration are entered the LMS way: once, in force until the next rate. So the write-back
needs a conversion like the one below for at least those rates. Under option A it applies to
every rate:

1. For each LMS rate, in start-date order, emit an `R` on its start date with `RATE = rentalRate`.
2. If it stays in force for more than one anniversary (its own start + k years), emit it as
   `RECURRING = YEARLY`:
   - `RECURRANCES` = the number of further anniversaries before the next rate starts;
   - `RECURRANCES` empty (indefinite) when it runs to the final term end. That matches how
     PEARS holds the tail rate today (P2244 and P2522 both end on an indefinite rate of 7500).
     **Decided (28 October 2026):** an indefinite rate is written with an **empty `RECURRANCES`
     element**, not with the element left out. PEARS reads both as indefinite (the payments report
     and `PED012X` test `recurrances IS NULL`), so this only fixes the XML shape.
3. Merge adjacent LMS rates with the same value that only exist because of a phase/term split.
   One recurring `R` is emitted rather than two, so the write-back does not rely on the payments
   report's `GROUP BY rate` folding them together.
4. A rate whose own anniversaries do not line up with the licence anniversaries (a `CUSTOM_PERIOD`
   rate starting mid-year) recurs on its own anniversaries, the same as PEARS does today.

**What the migration must guarantee for this to work:** the LMS rates must carry the same
**per-anniversary rate series** as PEARS, meaning which rate is in force on every licence
anniversary. This is the same per-occurrence comparison the comparison report already runs. A
PEARS gap (see 4 above) is the one case that needs a decision first. If the gaps are errors (a),
the write-back fills them, which is the intended correction. If they are legitimate (b), the
migrated £0 rate keeps them as no-payment years.

**Round-trip test (owned by the write-back work, specified here):** for a sample of licences
(including P2244, P2522, every licence with a recurrence gap, an `AL` and a `PL` licence with
fixed-count recurrences (data survey S12b), and the migrated licences whose LMS offsets differ from
PEARS's, e.g. P1609, P1890, P2301 and P255 (S14b)), run PEARS → LMS → write-back XML.
Run the payments report's `R` selection over both the original and the written-back XML for each
year of the schedule. Assert the same `(payment date, rate)` rows. Do not compare raw event shape:
PEARS can hold the same series as several non-recurring rows or one recurring row, and the report
treats both the same. Also assert that every written-back event's `EVENT_DATE` equals the original
PEARS date and `get_julian_or_event_date` on its written-back offset.

**Comparison.** The report expands PEARS rates into their yearly occurrences. For each occurrence it
shows the LMS rate in force on that date (value, anchor period, start date). Mismatched values or
missing coverage are flagged per occurrence.

### Retention, development and pre-producing areas

**Decided:** migrated in this exercise, so a licence's schedule and its area events arrive in LMS
together. They are held in **their own LMS tables**, the LMS schedule looks them up for display,
and the write-back regenerates the PEARS `RD`/`PA` events from them.

**LMS phase 1 scope:** only the `RD`/`PA` **schedule events as they are in the existing PEARS
schedule XML**: their subtype, date, comments, `EVENT_ID` and `RETENTION_AREA` block. The area
records themselves (`ped_retention_areas`) and the area work programmes are **not** migrated in
phase 1. The background below explains where PEARS derives the events from, for a later phase.

#### How PEARS holds them (background)

- **`pedmgr.ped_retention_areas`**: one row per area per licence position (`ped_dp_id`, live
  simulation 0), with `name`, `short_name` (≤ 15), `type`, `version`, `approval_date`,
  `expiry_date`, `si_id` (spatial instance), `start_datetime`/`end_datetime`,
  `created_by_operation_type`, `reason_for_removal`, and `work_programme_xml`.
- **Types** (`licence_retention_area_types`): onshore `RETENTION`, `DEVELOPMENT`; offshore
  `PROSPECTIVE`, `PRODUCING`. The schedule event code follows the licence type:
  `LICENSING_RETENTION_AREAS.get_schedule_event_mnem` returns `PA` for `P` licences, otherwise `RD`.
- **Work programme** (`work_programme_xml` / `ped_work_programme_store`): `EVENT_LIST/EVENT` with
  `DATE`, `DESCRIPTION` (applicant), `AMENDED_DESCRIPTION` (case officer, used on the schedule and
  consent letter) and `STATUS` (`NEW`, `CURRENT`, `CHANGED`, `NOT_COMPLETED_NEW`,
  `NOT_COMPLETED_CURRENT`), maintained in Fox `PED031L` / `PED033X`.
- **Schedule events**: `LICENSING_RETENTION_AREAS.apply_active_to_lic_schedule` writes real
  `RD`/`PA` events into the licence schedule XML for each active area. There is one `APPROVAL`
  event ("Approval: dd-mm-yyyy"), one `EXPIRY` event when there is an expiry date ("Expiry:
  dd-mm-yyyy"), and one `WORK_PROGRAMME` event per work programme event that is not "not
  completed" (comments = `AMENDED_DESCRIPTION`). Each event carries `EVENT_SUBTYPE`, an `EVENT_ID`
  from `ped_licence_schd_event_id_seq` and a `RETENTION_AREA` block (`NAME`, `SHORT_NAME`, `TYPE`,
  `VERSION`), and has no years/months/days offset. Changed events are deleted and re-added, and
  their acknowledgements are end-dated.
- **Removal** (`apply_removed_to_lic_schedule`) deletes an area's **changed and future** events.
  **Past** events stay on the schedule after the area has gone.
- **Geometry**: already migrated by the gis-framework GIS migration (`load_retention_areas`,
  `Layer.RETENTION_AREAS`), keyed by the PEARS `si_id`.

#### LMS table (phase 1; new, Flyway, audited with Envers)

`licence_schedule_area_events`, one row per `RD`/`PA` event in a migrated PEARS schedule version:

| Column | From the PEARS XML event |
|---|---|
| `id` (UUID) | — |
| `licence_schedule_detail_id` | the LMS detail for the PEARS version (`plsd_id`) it came from |
| `event_mnem` | `EVENT_MNEM` (`RD` onshore / `PA` offshore) |
| `subtype` | `EVENT_SUBTYPE` (`APPROVAL` / `EXPIRY` / `WORK_PROGRAMME` / `END`), **nullable** |
| `event_date` | `EVENT_DATE` |
| `comments` | `COMMENTS` (e.g. "Approval: 01-12-2021", or the case officer's work programme description) |
| `area_name`, `area_short_name`, `area_type`, `area_version` | `RETENTION_AREA/NAME`, `SHORT_NAME`, `TYPE`, `VERSION`, **nullable** |
| `pears_event_id` | `EVENT_ID` (kept so PEARS acknowledgements still match after write-back), **nullable** |

**Live data (data survey,
[S15](pears-schedule-migration-data-survey.md#s15-rdpa-events-current-versions)):** 463 current
`RD`/`PA` events (60 of them on the other onshore types, all complete). Subtypes include **`END`** (25 events) as well as approval, expiry and work
programme. **51 `PA` events have no subtype, no `RETENTION_AREA` block and no `EVENT_ID`**: older,
hand-entered pre-producing area events. They are copied as is with those columns empty. On the
timeline they are headed "Pre-producing area", with the comments below. The write-back writes them
without those elements. Area types in use: `PROSPECTIVE` (`PA`), `RETENTION` and `DEVELOPMENT`
(`RD`).

Phase 1 keeps the area attributes on each event row, exactly as PEARS embeds them, rather than in a
separate area table. A later phase that migrates the area records can normalise them into one.

#### Migration (phase 1)

1. For **every migrated schedule version**, copy each real (non-dummy) `RD`/`PA` event in its XML
   into `licence_schedule_area_events` against that version's LMS detail. Copy it **as is**, with no
   derivation from `ped_retention_areas` and no date arithmetic (these events have absolute dates and
   no offsets).
2. Report, without changing anything: events with no `RETENTION_AREA` block or no `EVENT_SUBTYPE`,
   events with no `EVENT_ID`, and an `RD` event on a `P` licence or a `PA` event on an onshore
   licence.
3. Same per-licence transaction as the rest of the schedule.

Keyed by schedule detail, each LMS version shows the area events its PEARS version had. That keeps
history faithful.

#### Display on the LMS schedule

- A new timeline event type, `ScheduleEventType.AREA` ("Retention, development and pre-producing
  areas", filterable), looked up from `licence_schedule_area_events` for the detail being viewed.
  The filter offers it on every schedule, as it does the other event types. Each event sits in the
  term or phase its date falls in, like other dated events. The heading is the area name and
  versioned short name, `Area one (A1v2)` (as PEARS shows it,
  `licensing_retention_areas.get_versioned_short_name`: the version is added from version 2 on),
  and the body is the comments. Built in PR 6.
- **Read-only** in phase 1. They are not `ScheduleEvent` subclasses and take no part in term/phase
  date calculation.
- When LMS creates a new draft from a detail, it **copies** that detail's area event rows to the
  draft (through the existing duplication service), so the draft shows them too.

#### Keeping `RD`/`PA` events in step (EPMQ)

**Decided:** PEARS remains the owner of the **areas** (`ped_retention_areas`, work programmes,
area screens) in phase 1. For migrated licences, **LMS is the only writer of the PEARS schedule
XML**, `RD`/`PA` events included. When an area changes, PEARS no longer rewrites the schedule
itself. It sends the resulting `RD`/`PA` events to LMS, LMS updates its schedule, and LMS's
write-back rebuilds the whole PEARS XML (see "Write-back transport (EPMQ, LMS → PEARS)").

The round trip for an area change:

1. Area approved, changed, removed or expired in PEARS (`PED031L`/`PED033X`, `auto_expire_areas`).
2. PEARS works out the licence's new `RD`/`PA` event set and **publishes it to LMS**. It does **not**
   update `ped_licence_schedule_details`.
3. LMS replaces its `RD`/`PA` events, commits, and publishes the **full schedule** (all events,
   `RD`/`PA` included) back to PEARS.
4. PEARS rebuilds the schedule XML entirely from that message and saves one new version.

**PEARS changes** in `LICENSING_RETENTION_AREAS`:

- `apply_active_to_lic_schedule` and `apply_removed_to_lic_schedule` keep their existing **event
  logic** unchanged:
  - work out approval, expiry and work programme events per area, skipping not-completed work
    programme events;
  - delete changed events, and the changed and future events of removed areas;
  - keep the past events of removed areas;
  - allocate new `EVENT_ID`s from `ped_licence_schd_event_id_seq`.

  But for a **migrated licence**, instead of
  inserting a new `ped_licence_schedule_details` row, ending acknowledgements and calling
  `generate_acknowledgements`, they **publish** the resulting complete `RD`/`PA` set. The starting
  point is the `RD`/`PA` events of the current PEARS schedule version, which the last LMS write-back
  produced.
- **Licences whose schedule LMS does not own** (never migrated, or skipped by the migration) keep today's
  behaviour unchanged. This needs a per-licence flag PEARS can check, e.g.
  `pedmgr.ped_lms_schedule_sync` (below) or a column on `ped_licence_master`, set when a licence's
  schedule is migrated.
- **Publish** through `epmq.events` (as `publish_pears_correction_applied_event` does) a new message,
  e.g. `PEARS_LICENCE_AREA_SCHEDULE_EVENTS_UPDATED`, on the PEARS licences topic.
- **Payload**: licence reference, a PEARS change sequence/timestamp (ordering key), the PEARS schedule
  version it was based on (`plsd_id`), and the **complete new set** of the licence's `RD`/`PA` events,
  each with `EVENT_ID`, subtype, date, comments and the `RETENTION_AREA` block. The licence contacts
  message works the same way, with data in the payload, so LMS needs no Oracle connection in
  production to act on it.
- **LMS subscribes** through the existing PEARS licences subscription (queue `pears-licences-lms`,
  `EpmqTopics.PEARS_LICENCES`). The message type is added to `EpmqMessageTypeMapping` and handled
  next to `PearsLicenceContactsEpmqMessage` (or in a sibling subscription service).
- **LMS applies** it by **replacing** the area event rows on the licence's `ACTIVE` detail, and on any
  open `DRAFT`, so applying a draft does not bring back an old copy. It does **not** create a new LMS
  schedule version: the change is PEARS's, not an LMS schedule edit. Envers keeps the row history.
  When that commits, LMS publishes the full schedule back to PEARS (trigger (b) of the write-back).
- **Idempotent and ordered**: replace-all semantics make redelivery harmless. A message older than
  the last one applied for the licence (by the PEARS change sequence) is ignored.
- **Delay**: the PEARS schedule, its acknowledgements and the data warehouse views only show an area
  change once the round trip completes. If LMS or EPMQ is down, PEARS's schedule lags behind its area
  data until the messages are processed. The sync table and the stuck-licence report below make this
  visible.
- **Licences not yet migrated**: messages for licences with no LMS schedule are acknowledged and
  ignored. The migration copies their events when it runs.
- **Gap between migration and go-live**: before switching the subscription on, re-run the `RD`/`PA`
  copy for every migrated licence, or replay from the latest PEARS version, so nothing changed in
  between is missed. The comparison report checks LMS against PEARS per licence.
- **New EPMQ message type**: if EPMQ message classes live in a shared library, adding the type is a
  prerequisite change there.

### PEARS write-back: regenerating the schedule XML

**Decided:** the write-back rebuilds the **whole** PEARS schedule XML from LMS: the active
schedule detail plus its `RD`/`PA` events, all carried in the write-back message. It does not patch
or merge with the existing XML.

**Decided: every write-back creates a new PEARS schedule version. It never overwrites the existing
XML.** The current `ped_licence_schedule_details` row is end-dated (`end_datetime = SYSDATE`) and a
new row is inserted with the rebuilt XML (`start_datetime = SYSDATE`, `created_by` = the write-back
user), exactly as `update_licence_schedule` does today. The write-back never runs
`UPDATE … SET xml_data`, unlike the in-place updates PEARS uses at raise (`api-set-origin-date`,
`licence_raise`). PEARS's schedule history, the data warehouse views and acknowledgements
(`plsd_id`) therefore keep every earlier version. **This applies even when nothing has changed.**
No "unchanged" check is made: PEARS already lets a schedule be saved with no changes, and comparing
versions would only add overhead. That
puts these requirements on the migration and the write-back (building it is separate work):

- **`SCHEDULE/ORIGIN_DATE`** = the active detail's `LicenceStartDate`. `RETENTION_AREA_RATE` is
  written as an empty element on onshore licences (it is unused).
- **Schedule events** (`T`, `P`, `R`, `MR`, `E`, `BM`, `DD`, `2M`, `2N`, …): `EVENT_DATE` = the LMS
  date (expiry per Open question 8). `YEARS`/`MONTHS`/`DAYS` are the offset from `ORIGIN_DATE` that
  reproduces that date under **PEARS** arithmetic, otherwise the Fox screen recalculates and moves
  it on the next save (see "Offsets in the write-back" below). Also `TERM_NO`, `PHASE`, `RATE`
  (rates per "Rates and the PEARS write-back"), `PERCENTAGE` (from the LMS percentage field, see
  "Decision: mandatory relinquishment percentage") and `COMMENTS`.

**Offsets in the write-back.** LMS lands on the PEARS dates (Open question 6), so the write-back
must keep them there. PEARS never trusts a stored `EVENT_DATE`: on every rebuild
(`template_schedule.process_event_date_from_origin`, and `PED012X`'s `qry-populate-event-dates`)
it recalculates it from the offset with `get_julian_or_event_date`, which adds **days first, then
months** (`ADD_MONTHS(origin + days, years * 12 + months)`). So the stored offset must give back
exactly the date LMS sent.

PEARS's existing date-to-offset procedure, `template_schedule.get_offset_from_event_date` (used by
`PED012X`'s "enter a date" popup), works **months first, then days**
(`FLOOR(MONTHS_BETWEEN(...))`, then the remaining days). It is **not** an exact reverse of
`get_julian_or_event_date`, so it cannot be used as is. Example, P1609 (origin 12-Feb-2009):

| | Result |
|---|---|
| Date LMS sends | 31-Dec-2016 |
| `get_offset_from_event_date` | 7y 10m 19d |
| `get_julian_or_event_date` on that offset | **03-Jan-2017** (moved) |
| Offset PEARS holds today | 7y 10m 16d → 31-Dec-2016 |

This is probably why so many PEARS offsets are irregular (data survey S14b): users adjusted the days
until the screen showed the date they wanted.

`apply_lms_schedule` therefore:

1. **Finds an offset that reproduces the date.** Take the whole months from `MONTHS_BETWEEN`, then
   find the days for which `get_julian_or_event_date` returns exactly the LMS date, trying one
   month fewer if none does. A days-only offset (0y 0m n days) always works as the fallback.
2. **Checks every event before saving.** If `get_julian_or_event_date` on the chosen offset doesn't
   equal the date LMS sent, the message is rejected (and acknowledged as failed), not saved with a
   moved date.
3. **Yearly rates depend on the offset too.** PEARS places each later occurrence from the same
   offset (`years + k`), so two offsets giving the same first date can give different later
   payment dates around month ends (e.g. 28 vs 29 February, or a month-end that `ADD_MONTHS`
   snaps). The round-trip test (below) compares every payment date, which catches this.

The offset is calculated in PEARS, not LMS, so it uses Oracle's own `ADD_MONTHS` rather than a
Java copy of it. `RD`/`PA` events keep empty offsets, as today.
- **`RD`/`PA` events: from LMS, in the message.** PEARS sends area changes to LMS instead of
  writing them itself (see "Keeping `RD`/`PA` events in step (EPMQ)"), so LMS's
  `licence_schedule_area_events` on the active detail **is** the current set, and the write-back
  carries it. PEARS writes one event per row with `EVENT_MNEM`,
  `EVENT_SUBTYPE`, `EVENT_DATE`, `COMMENTS`, `RETENTION_AREA` (`NAME`, `SHORT_NAME`, `TYPE`,
  `VERSION`), empty offsets, and `EVENT_ID`.
- **`EVENT_ID`**: written back unchanged for every migrated event that had one (from
  `pears_event_id`). New events take one from `ped_licence_schd_event_id_seq`. PEARS acknowledgements
  (`ped_licence_acknowledgements`) match on licence, event type, date and `EVENT_ID`, so a changed id
  or date creates a new unacknowledged acknowledgement while the old row stays.
- **`COMMENTS`**: PEARS holds one value per event. **Decided:** LMS event comments (`EventComment`,
  including comments added in LMS after migration) are **not** published back to PEARS, because
  they are not reported on. `COMMENTS` is written only from LMS fields that hold it: the
  `description` of other schedule events (mandatory relinquishment, bespoke milestone, other
  activity), `LicenceScheduleExpiry.comments`, and the `RD`/`PA` events' own `comments`. Those
  round-trip. **Consequence:** terms, phases and rates have no description in LMS, so their PEARS
  `COMMENTS` were migrated as `EventComment`s and are **not** written back. After the first
  write-back, PEARS's Term/Phase/Rate comments are empty: for example P2244's Term 2 extension
  history, and P2522's "Was £150, now £15" rate notes. They remain visible in LMS. They also drop
  out of PEARS's text summary (`scribe_schedule_events`). The PEARS schedule screen redirects to LMS,
  so nobody sees them missing there.
- **Not written**: LMS work programme activities (decided: LMS owns them and they aren't written
  back; see [pears-work-programme-migration-plan.md](pears-work-programme-migration-plan.md),
  "Decision: no write-back to PEARS"). LMS other events whose
  category has no PEARS event type (a free-typed "Other activity" name that matches no
  `ped_template_event_types.short_title`). **Pending check (Open question 13):** if nothing in PEARS
  reporting needs them, they are **not written back** and stay in LMS only.
- **Fox elements**: every event has `EDITABLE` = false, `SELECTED` = false and an empty `ACTIONS`,
  as `PED012X` and `apply_active_to_lic_schedule` write them.
- **Save** through `pedmgr.template_schedule.update_licence_schedule`, passing the licence's existing
  template (`ped_ts_id`, name, description) unchanged. That end-dates the previous version and keeps
  `xview_ped_lic_schedule_events` and the materialised views current. Then run
  `template_schedule.generate_acknowledgements`. Exactly one new PEARS version per write-back message,
  changed or not (see "Write-back transport (EPMQ, LMS → PEARS)").
- **One writer**: from go-live, only the LMS write-back writes a migrated licence's
  `ped_licence_schedule_details`. **Decided:** the Fox schedule screen (`PED012X`) **redirects to
  the LMS schedule** for these licences rather than becoming read-only. Use the existing
  `LMS_REDIRECT` module's `view-schedule` theme (→ `{LMS}/redirect-to-licence/{licence ref}/schedule`),
  as the licence search's "View Schedule" link already does. Every route into `PED012X` for a
  migrated licence (`licenceView`, `licenceMaintain`, `licenceMaintainAndAcknowledge`, and calls
  from Raise Licence `PED017X` or retention area screens) redirects. Template themes (`new`,
  `templateMaintain`, `templateView`, `copy`) are unaffected. The area screens (`PED031L`/`PED033X`) and `LICENSING_RETENTION_AREAS` keep working,
  because PEARS still owns the areas, but for migrated licences they publish the `RD`/`PA` set to LMS
  instead of writing the schedule (Open question 9).
- **Round-trip test**: PEARS XML → LMS → regenerated XML for sample licences (P2244, P2522, licences
  with areas, removed areas and gaps). Compare event by event on (type, subtype, date, key fields,
  `EVENT_ID`), plus the payments report rows (see "Rates and the PEARS write-back").

#### Write-back transport (EPMQ, LMS → PEARS)

**Decided:** the write-back travels over EPMQ, the reverse of the `RD`/`PA` updates. LMS publishes
the licence's schedule, and a PEARS-side consumer applies it.

**Which licences:** every licence on a **PEARS-managed licence type**
(`LicenceType.isManagedByLms() == false`), the same set the migration covers (see "Scope: licence
types and statuses"). That is `SEAWARD_PRODUCTION` and `LANDWARD_PRODUCTION` (`P`, `PEDL`, `PL`,
`EXL`, `ML`, `AL`, `DL`, `CE`); LMS no longer has placeholder types. It includes new licences raised in
PEARS whose first schedule is created in LMS. LMS-managed types (carbon storage, gas storage,
landward and seaward exploration, methane drainage) have no PEARS schedule, so LMS never publishes
the write-back for them, and the `RD`/`PA` round trip does not apply. LMS checks
`isManagedByLms()` before publishing (e.g. a `LicenceTypeRulesResolver`/feature rule
`hasPearsScheduleWriteback`), rather than relying on PEARS to reject the message.

**When LMS publishes:**

- **(a) A draft schedule is applied:** `ReviewAndApplyScheduleController.applyDraftSchedule`, once
  the new `ACTIVE` detail has committed. Checked November 2026: this
  (`LicenceScheduleDetailService.applyAndReplaceActiveScheduleDetail`) is still the only place a
  version becomes `ACTIVE`. The schedule and work programme application journeys added since
  (record of decision, duration changes, request update) read the active schedule but don't apply
  one, so they need no trigger of their own.
- **(b) LMS has received and finished processing a PEARS `RD`/`PA` message:** once the replaced area
  event rows have committed. This is LMS's confirmation back to PEARS, and the **only** way an area
  change reaches the PEARS schedule.

Both publish **after commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)` or an outbox
table drained by a scheduled job). A message is then never sent for a change that rolled back, and
a failed publish can be retried. LMS currently only **subscribes** to EPMQ
(`PearsLicenceContactsSubscriptionService`). Publishing from LMS is new, through the same EPMQ
starter (`uk.co.fivium.energyportalmessagequeue`, `v4.17.1`).

**Message**, e.g. `LMS_LICENCE_SCHEDULE_UPDATED` on a new LMS licences topic. The message types live
in the shared `energyportalmessagequeue:messages` library, so a library release is a prerequisite.

| Field | Content |
|---|---|
| `licenceReference` | e.g. `P1234` |
| `trigger` | `DRAFT_APPLIED` or `PEARS_AREA_EVENTS_PROCESSED` |
| `lmsScheduleDetailId`, `lmsAppliedAt` | the `ACTIVE` detail and when it became active (ordering key) |
| `basedOnPearsAreaChange` | the PEARS area change sequence LMS has applied: for (b), the one just processed (the confirmation); for (a), the latest one |
| `originDate` | `LicenceStartDate` |
| `events` | **every** event of the active detail in PEARS terms: the schedule events (`eventMnem`, `eventDate`, `termNo`, `phase`, `rate`, `recurring`/`recurrances`, `percentage` (mandatory relinquishments), `comments`, `pearsEventId`) **and** the `RD`/`PA` events (`eventMnem`, `eventSubtype`, `eventDate`, `comments`, `retentionArea` {name, shortName, type, version}, `pearsEventId`) |

The message is **complete**: PEARS reconstructs the whole schedule XML from it and nothing else.
`YEARS`/`MONTHS`/`DAYS` are not sent: the PEARS procedure works them out from `originDate` so that
`get_julian_or_event_date` gives back each event's date exactly, and rejects the message if it
can't (see "Offsets in the write-back"; PEARS's own `get_offset_from_event_date` is not an exact
reverse and is not used as is). `RD`/`PA` events keep empty offsets, as PEARS writes them today.

**PEARS applies it** in one PL/SQL procedure, e.g. `pedmgr.template_schedule.apply_lms_schedule`:

1. Ignore the message if a later LMS version has already been applied for the licence (by
   `lmsAppliedAt` / `lmsScheduleDetailId`). This needs a small sync table, e.g.
   `pedmgr.ped_lms_schedule_sync (ped_lm_id, lms_schedule_detail_id, lms_applied_at, pears_plsd_id, applied_datetime)`.
2. Build the XML **entirely from the message**: every event (schedule and `RD`/`PA`), plus
   `ORIGIN_DATE`, the empty `RETENTION_AREA_RATE` (onshore) and the Fox elements. Nothing is merged
   from the current version.
3. **Always save**, even if the result is identical to the current version (decided: no comparison,
   as PEARS already allows saving an unchanged schedule).
4. **Acknowledgements:** end-date (`end_datetime = SYSDATE`) every current
   `ped_licence_acknowledgements` row for the licence whose (event type, date, `EVENT_ID`) is no
   longer in the new XML. `apply_active_…`/`apply_removed_…` do this today for `RD`/`PA` events
   they delete, and that job moves here now that they no longer write the schedule.
5. Save as a **new version** through `update_licence_schedule` (end-dates the current row, inserts
   the new one; existing template passed through). Never update the current row's `xml_data` in
   place. Then run `generate_acknowledgements`, all in one transaction with the sync row.
6. It must **not** publish the area EPMQ message. Only area changes publish it, so PEARS → LMS →
   PEARS cannot loop.

`ped_lms_schedule_sync` also serves as the "schedule owned by LMS" flag that `LICENSING_RETENTION_AREAS`
checks before publishing rather than writing the schedule. The migration creates the row for each
licence it migrates.

**Consumer: mechanism to be determined (Open question 10).** Something on the PEARS side must
pick up the LMS events message and run `apply_lms_schedule` to recreate the latest schedule XML.
In this repository PEARS's Oracle code only **publishes** to EPMQ (`epmq.publish_*`,
`epmq.message_*`); I found nothing in PEARS that consumes EPMQ messages. Whatever is chosen
must:
- deliver each licence's messages **in order** (LMS version order), or leave ordering to
  `apply_lms_schedule`'s version check;
- call `apply_lms_schedule` in one transaction per message, and acknowledge the message only after
  that commits;
- retry transient failures, and dead-letter messages that cannot be applied, recording them
  against the licence;
- run as the PEARS schema user that is allowed to write `ped_licence_schedule_details` and
  `ped_licence_acknowledgements`.

`apply_lms_schedule` and the sync table are specified so they work with any consumer.

**Failures:** a message PEARS cannot apply (unknown licence, a licence on a type LMS manages itself,
an event with no PEARS type, an invalid
date) is logged against the licence and dead-lettered, and LMS is not changed. A licence whose
latest LMS version has no sync row after a set time is reported, so the two cannot drift silently.

**Ordering between the two directions:** if a draft is applied in LMS while an area change is on its
way from PEARS, the trigger (a) message carries the old `RD`/`PA` set and the trigger (b) message
that follows carries the new one. PEARS applies both in LMS version order, so it ends up correct.
PEARS computes each area change from its **current** schedule version (the last write-back). If two
area changes happen before the first round trip completes, the second is based on a version that
does not yet include the first. PEARS should therefore base the next change on the latest set it
**published** for the licence (kept in a small outbox or the sync table), not on the schedule XML.

**Go-live order:**
1. Switch the PEARS schedule screen (`PED012X`) to redirect to LMS for migrated licences, so no
   further schedule edits are made in PEARS. The work programme screen (`PED009X`) redirects in the
   same PEARS release.
2. Run the final migration and `RD`/`PA` refresh, and create the `ped_lms_schedule_sync` rows.
2a. Run the work programme migration, which needs the migrated schedules, before users edit
    schedules in LMS
    ([pears-work-programme-migration-plan.md](pears-work-programme-migration-plan.md), "Release").
3. Switch on the LMS → PEARS write-back consumer.
4. Switch `LICENSING_RETENTION_AREAS` to publish for migrated licences, and switch on LMS's
   subscription.

### New production licences (licence created in PEARS, schedule created in LMS)

**Decided:** new production licences are still **created in PEARS**, but their schedule is
**created in LMS**. PEARS gets the schedule only through the write-back.

**Today in PEARS:** on Raise Licence (`PED017X`) a template schedule is attached
(`licence_create.licence_schedule_attach` → `template_schedule.create_licence_schedule`, which also
sets `ped_licence_master.ped_ts_id`). `licence_raise` then writes `ORIGIN_DATE` into the schedule
XML and calls `process_event_date_from_origin`.

**Flow after go-live:**

1. The licence is created and raised in PEARS **without attaching a template schedule**. The attach
   step in `PED017X` is removed or skipped for production licences. `licence_raise`'s `ORIGIN_DATE`
   update and `process_event_date_from_origin` then do nothing, because there is no schedule row.
2. The licence reaches LMS through the existing PEARS licence refresh
   (`PearsLicenceRefreshService`), as a production licence with no schedule.
3. An LMS user creates the schedule (`LicenceScheduleService.getOrCreateNewLicenceScheduleForLicence`
   → draft) and applies it.
4. Applying it triggers the write-back (trigger (a)). For a licence with no PEARS schedule yet,
   `apply_lms_schedule`:
   - inserts the **first** `ped_licence_schedule_details` version. `update_licence_schedule`'s
     end-date step simply matches no rows;
   - handles **no template**. `ped_ts_id` is null, so the template name/description update is skipped.
     The Fox schedule screen is not affected, because it redirects to LMS for licences whose schedule LMS owns.
     Any other PEARS code that joins `ped_template_schedules` for a licence must cope with no
     template, or a placeholder template per licence is created (Open question 12);
   - creates the `ped_lms_schedule_sync` row, so from then on the licence is treated as LMS-managed;
   - runs `generate_acknowledgements`.
5. **Existing areas:** `apply_active_to_lic_schedule` returns early today when a licence has no
   schedule. If a new licence already has areas when its first LMS schedule is written back,
   `apply_lms_schedule` (or a step straight after it) works out the licence's `RD`/`PA` set and
   publishes it to LMS, so the round trip fills them in.

**Migration:** licences with no PEARS schedule at migration time are not given one. Their LMS schedule
is created by users. The comparison report lists them as "No PEARS schedule". On live (27 October
2026) there are 1,114 `P` and 124 `PEDL` licences with no schedule, and **none is extant**. They
are surrendered or revoked licences, or records with no live data point that never became licences
(data survey, [S17](pears-schedule-migration-data-survey.md#s17-licences-with-no-pears-schedule)).
The other PEARS-managed types also have licences with no schedule (e.g. 283 `EXL`, 240 `PL`, 25
`ML`, 7 `AL`), and **none of them is extant either**. So no existing licence without a PEARS
schedule needs one created by hand at go-live. (The 40 schedules with no terms are a separate
manual step for the licensing team: "Term boundaries" rule 7.)

**Licence start date:** the LMS `LicenceStartDate` becomes PEARS's `ORIGIN_DATE`, while PEARS's own
licence start date (`xview_ped_ld_current.licence_start_date`, set at raise) is separate. LMS should
default the schedule start date from the PEARS licence start date. The two can legitimately differ
(data survey S10b), so the write-back writes the LMS date as `ORIGIN_DATE` and does not flag a
difference.

**Licence documents:** PEARS licence documents (`document_licence_get_mm_data`, built by Raise
Licence `PED017X` and Licensing Documents `PED019X`) read term and phase lengths, the mandatory
surrender percentage and the schedule summary from the PEARS schedule. For a new licence these only
exist after the LMS schedule has been applied and written back. Documents generated before that are
missing their schedule sections (Open question 11).

**Alternative considered, not chosen:** keep the PEARS raise schedule step (the user assigns an
existing template or generates one from the round details, then may edit it in `PED012X`) and have
`licence_raise` publish the finished schedule to LMS, which would map it with the migration mapper.
That would have kept templates and licence documents at raise working. **Decision: new licence
schedules are created in LMS.** Open questions 11 and 12 stay open.

### Decision: mandatory relinquishment percentage

**Decided (28 October 2026): option 1.** The mandatory relinquishment percentage becomes its own
data point on the LMS schedule, entered with a mandatory relinquishment event. The migration
brings the PEARS values in, and the write-back includes the percentage. (Earlier it was marked out
of scope. It was reopened because PEARS holds it nowhere else and it is used downstream.)

**Where PEARS holds and uses it.** Only `PERCENTAGE` on an `MR` event in
`ped_licence_schedule_details.xml_data`. Everything else reads it from there:

| Consumer | Use |
|---|---|
| `xview_ped_lic_schedule_events.relinquishment_percentage` | Data warehouse column |
| `template_schedule.get_mand_relinquishment_perc` | The percentage on the `MR` event dated the same day as the `T(1)` event (end of the initial term) |
| `licence_create.document_licence_get_mm_data`: `MAND_SURRENDER_AREA` | Licence document merge data, built by Raise Licence (`PED017X`) and Licensing Documents (`PED019X`) |
| Document templates `PED_PRODUCTION_LICENCE`, `…_OFF_V2`, `…_OFF_V4`, `…_ONSHORE` | "The **Mandatory Surrender Area** is an area comprising X% of the Initial Licensed Area." The sentence is omitted when empty |
| `template_schedule.scribe_schedule_events` | Text schedule summary (" : 50%") |

Real data: licence P2522 has `PERCENTAGE` 50 with no comment. Licence P2244 has no `PERCENTAGE` but
comment "50% Relinquishment". Its later relinquishment has `PERCENTAGE` 0 and comment "Everything
outside FD areas".

**Chosen: bring it into LMS as a field and write it back to PEARS.**
- LMS change (prerequisite, its own change): add an optional percentage to mandatory
  relinquishment events, e.g. `OtherScheduleEvent.relinquishmentPercentage` (`BigDecimal`, 0–100),
  shown and editable only for `MANDATORY_RELINQUISHMENT`, plus Flyway column, form, validator and
  timeline display. **Allow up to one decimal place** (live values include 87.5; data survey, S13),
  e.g. column `NUMERIC(4,1)` and a validator limit of 1 dp. Historical versions hold the same
  numbers, so the field fits every version (S13c).
- Migration: `relinquishmentPercentage = PERCENTAGE` (empty stays empty), after trimming and
  **stripping a trailing `%`** (live data has `50%`; data survey, S13). **Decided:** the placeholder
  **`x`** (303 current and 767 historical events on live, S13b, S13c) is not migrated. The
  relinquishment event itself is still migrated (date, description), with an empty percentage, and
  the `x` values are counted once in the report. Values that still do not parse as 0–100 with at
  most 1 dp are not migrated and are reported: on live, only one historical P2079 event holding
  the text "Everything outside FD areas" (S13c). Comments still become the
  description.
- Write-back: `PERCENTAGE = relinquishmentPercentage` on the written `MR` event, written without a
  `%` sign and without trailing zeros (`50`, `87.5`).

Why: nothing is lost. Licence documents generated from PEARS after write-back keep the
"Mandatory Surrender Area" sentence, the data warehouse column stays populated, and LMS users see
and maintain the percentage as data rather than free text. It round-trips exactly, and the
comparison report checks it per event. The cost is an LMS model change before the migration, on a
field only mandatory relinquishments use.

**Data (data survey,
[S13](pears-schedule-migration-data-survey.md#s13-mandatory-relinquishment-percentages-current-versions),
live 27 October 2026):** 585 current `MR` events hold a number on the `T(1)` date (428 `P`, 149
`PEDL`, 8 `PL`), so they feed licence documents today. They migrate into the new field. 303 hold
`x` and migrate with an empty percentage. 713 `P` events hold the figure only in `COMMENTS`, with an
empty `PERCENTAGE`. They migrate with an empty percentage and the comment as their description, so
the figure stays visible in LMS, but not as data.

**Alternatives considered, not chosen:** dropping the percentage (licence documents and the data
warehouse would lose it), or keeping it only as an LMS comment (not data, and the write-back would
have to parse comment text to send it back).

### Retention/development area rate: unused

`SCHEDULE/RETENTION_AREA_RATE` is the only schedule value the payments report uses besides `R`
events (it multiplies the onshore retention/development area). Checked on **live** in October 2026,
it has never been set (data survey,
[S4](pears-schedule-migration-data-survey.md#s4-retentiondevelopment-area-rate)). The report
therefore always costs retention/development area at 0. The migration ignores it and the
write-back leaves it empty, which keeps the report output unchanged.

Areas (block and retention/development) are not schedule data: the report takes them from PEARS
licence positions (`ped_data_points` / `ped_licence_blocks`) and retention area data, so they are
outside the schedule write-back.

### Events with no event type

**Found on live (27 October 2026):** 1,498 non-dummy events across all versions, on 209 licences,
have an empty `EVENT_MNEM`. 201 of them are on current versions. The Fox grid and every report select
events by type (the payments report reads `R` only), so these rows are **invisible in PEARS today**.
About 180 of the 201 current ones are **empty rows** (template or grid leftovers). About 20 **hold
data**: rates missing their `R` code, terms missing their `T` code, text in the wrong field, and
comment-only rows. The queries and the per-licence list are in the data survey,
[S6](pears-schedule-migration-data-survey.md#s6-events-with-no-event-type).

**Decided rule: untyped events are ignored by the migration**, whether they are empty or hold
data. The licensing team never sees them: the schedule screen (`PED012X`) only displays and edits
events by type, so these rows can't be viewed, corrected or removed there. The LMS schedule
therefore matches the schedule the licensing team works with in PEARS today.

- **Not migrated, and never guessed.** The migration report only counts them (empty and holding
  data), for information.
- **No fix in PEARS is asked for.** It would need a data patch rather than the screen, and the
  rows have no effect today.
- **Consequence:** P1272 and P328, whose untyped rows look like terms, migrate without those terms,
  the same as the schedule shows in PEARS. P2582, P1159, P2438, P2660 and the others with untyped
  rates migrate without those rates, the same as the payments report today. (P2582's comment says
  a final 7500 rate from 2032 was added, but the row was saved without a type, so it has never
  been shown or reported.)
- **Write-back:** the rebuilt PEARS XML has no untyped rows. Nothing visible in PEARS changes,
  because they are not shown or reported today.

### Rate values

PEARS stores `RATE` as text. LMS stores `LicenceScheduleRate.rentalRate` as `BigDecimal`
(`rental_rate NUMERIC`), and its form only accepts a value of 0 or more with at most 2 decimal places
("The rental rate can only include pounds and pence", `LicenceScheduleRateFormValidator`).

**Decided, migration (PEARS → LMS):**

| PEARS `RATE` | LMS `rentalRate` | Report |
|---|---|---|
| Whole number, e.g. `7500` | `7500` (no decimal places) | — |
| Decimal with 1–2 places, e.g. `12.5`, `12.50` | `12.50` (as a decimal) | — |
| Decimal with more than 2 places, e.g. `12.345` | rounded to the nearest 2 dp, half up: `12.35` | each rounding (licence, version, date, original, rounded) |
| Leading/trailing spaces | trimmed, then as above | — |
| Anything that does not fit: empty on an `R` event, non-numeric (e.g. `£15`, `15/km2`, `TBC`), negative | not migrated | each one (licence, version, date, original value). The rate is skipped, and the licence is listed for a fix in PEARS before the production run |

Parse with `new BigDecimal(trimmed)` and no locale tricks, so thousands separators and currency
symbols count as "does not fit" rather than being guessed at.

**Decided, write-back (LMS → PEARS):** whole numbers are written **without** decimal places (`7500`,
not `7500.00`). Values with pence are written with **2** decimal places (`12.50`). That is
`rentalRate.stripTrailingZeros()` when it is whole, otherwise `setScale(2)`, in plain notation (never
`7.5E+3`).

**Data survey (live, 27 October 2026,
[S11](pears-schedule-migration-data-survey.md#s11-rate-values)):** every populated `RATE` is a whole
number, so rounding is never needed on today's data. 12 `R` events have an empty rate, all on two
rate-only schedules (P293, P295) with no terms (pattern E), which aren't migrated anyway.

### `RECUR_END_DATE`: unused

`RECUR_END_DATE` (a recurrence end date) is read by the payments report and exposed by
`xview_ped_lic_schedule_events`, but the Fox schedule screen has no field for it. Checked in October
2026, no schedule version, current or historical, holds a value (data survey,
[S5](pears-schedule-migration-data-survey.md#s5-recur_end_date)). The migration therefore never
sees it, and the mapper (PR 3) doesn't read it: with the gaps taken as errors, a rate runs on to
the next rate whatever its recurrence says. The write-back does not produce it. Recurrence is written back with `RECURRANCES` only.

### Decision: how yearly-recurring rates are held in LMS

**Status: option A for now (17 November 2026).** The mapper (PR 3) is built with option A. It
changes to option B only if the regulator asks for it, and then only "Rates and yearly recurrence"
step 3 changes.

PEARS can hold a rate as a `RECURRING = YEARLY` event. LMS has no recurrence: a rate is a start date
and a value, in force until the next rate. The PEARS payments report
(`PED_LICENCE_PAYMENTS_REPORT.xml`) needs either a non-recurring `R` or a yearly recurrence on every
anniversary a payment is due. Example: licence P2522, rate 7500 from 01-Dec-2032, recurring yearly
until the licence ends on 01-Dec-2045.

**Option A: one LMS rate per phase/term the recurrence touches; the write-back rebuilds recurrence.**
P2522 gets one LMS rate (Third term + 5y). The write-back emits `R 7500, RECURRING=YEARLY`.

For:
- It matches the LMS model. A rate is entered once and stays in force until changed, which is how
  users will enter every new rate after go-live.
- Schedules stay small and readable: one row per rate change, not up to ~20 identical rows per
  licence on the LMS timeline.
- If the final term is extended in LMS, the rate carries on automatically, like an indefinite
  PEARS recurrence. Under option B someone would have to add rows for the extra years.
- The write-back needs this conversion anyway for rates created in LMS after go-live, so it is one
  code path for migrated and new rates alike.
- The write-back reproduces PEARS's own shape (a recurring tail rate), so the payments report sees
  the same kind of data it does today.

Against:
- The write-back has to infer recurrence (count anniversaries until the next rate or the final term
  end). That is logic in a second system that must stay right.
- On its own it cannot reproduce PEARS gaps (anniversaries with no `R`, or a recurrence that stops
  early): after write-back those years would gain payments. The gap handling in "Rates and yearly
  recurrence" (step 4) removes this. Either the gaps are errors and are meant to be filled, or a
  migrated £0 rate keeps them as no-payment years.
- The round trip is equal in payments, not in raw XML. Anything else that reads raw `R` rows
  (rather than the payments logic) could see a different shape.
- In particular, `RECURRANCES` counts would appear on every licence type. Today only 9 `PL` and
  3 `AL` schedules have them; `P` and `PEDL` have only non-recurring rows and one indefinite
  yearly rate. The data
  warehouse owners need to confirm their feed handles a counted yearly `R` on `P` and `PEDL`.

**Option B: one LMS rate row per yearly redefinition; the write-back is one row to one row.**
P2522 gets 13 LMS rates of 7500 (2032 to 2044). The write-back emits 13 non-recurring `R` events.

For:
- The write-back is a direct copy: each LMS rate becomes one non-recurring `R`, with no inference.
- PEARS gaps survive: a year with no LMS row gets no `R`, so the payments report is unchanged for
  those licences.
- The PEARS series is explicit year by year, so anyone auditing the data warehouse sees every
  payment as its own row.

Against:
- It does not match the LMS model, and only migrated rates would look like this. New LMS rates are
  still entered once, so the write-back **still** needs option A's conversion for them. The result
  is two kinds of data and both code paths.
- An indefinite recurrence has to be cut off at the final term end. If the licence is later
  extended in LMS, no rate rows exist for the new years until someone adds them. PEARS's indefinite
  recurrence covered that automatically.
- It clutters the LMS timeline and schedule history: every schedule version repeats the expanded
  rows, and an edit to the rate means editing every yearly row.
- The written-back PEARS data changes shape (many non-recurring rows where there was one recurring
  row), even though the report output stays the same.

**Either way:**
- The migration's comparison report checks the rate in force on every anniversary against PEARS.
- The write-back round-trip test (below) compares payment rows, not event shape.
- PEARS gaps are listed by the data survey (about 310, S12b). The licensing team confirms whether
  they are errors (filled) or legitimate (a £0 rate), per "Rates and yearly recurrence" step 4.

### Anchoring "other" events to a term

LMS other events cannot be anchored to the licence start, only to a term or phase. Because the
first term starts on the licence start date, an event in the first term keeps PEARS's offsets
unchanged. For events in later terms, convert the offset to one measured from that term's start:
`Period.between(termStart, pearsEventDate)` normalised to years, months and days. Then recompute
the date with `calculateRelativeStartDueDate` and check it equals the PEARS date (see Validation).

### Event reminders after migration

LMS emails licensees ahead of schedule deadlines (`EventReminderJob`, see
`licence-schedule-reminders-data-model.md`). Once a licence is migrated, its schedule feeds that
job like any LMS schedule:

| Deadline | Reminded | Notice |
|---|---|---|
| Term or phase end | yes | 6 months |
| Other schedule event (`MR`, `BM`, `DD`, `2M`, `2N`) | yes | 6 months |
| Licence expiry | yes | **12 months** (changed from 6 in November 2026) |
| Rate | no | — |

- **Only what is coming up.** The job reads the `ACTIVE` version only, and only deadlines from today
  to today plus the notice period. So migrated history (the `REPLACED` versions) and past dates
  never send anything.
- **Only extant licences** (`EventReminderSuppressionService`). 420 licences with a schedule are
  extant (S18), slightly fewer once the schedules with no terms (rule 7) are left out.
- **Only where the LMS1 release phase is on.** In other environments the job does nothing.
- **A deadline on the expiry date is dropped.** The expiry reminder covers it (`EventReminderService`,
  November 2026). This interacts with Open question 8.

So on the first run after the production migration, **every extant licence with a deadline in its
window is reminded at once**. Each deadline is only reminded once (`licence_reminders`), so later
runs are back to normal. Before go-live, decide (Open question 15):

- whether licensees should get these emails from the first run, or reminders for migrated licences
  start from an agreed date (e.g. only deadlines first entering the window after go-live);
- whether PEARS sends anything equivalent today (e.g. acknowledgements) that would duplicate them;
- whether licensees have licence contacts in LMS. Missing ones are reported to the licence contacts
  managers (`licence_reminder_missing_contacts`), which could be a large first report.

The UAT rehearsal should run the reminder job against the migrated data, with email going to a test
inbox, to see the volume.

## Design

Follow the existing `migration/pears` package for reading PEARS and the `migration/carbonstorage`
package for building schedules.

**Recent changes to `migration/pears` to follow (November 2026):**

- **Read PEARS before opening the LMS transaction.** `LicenceWritebackService` now reads a licence's
  history first, then writes it in a transaction of its own (`TransactionOperations`), so a licence
  waiting on PEARS holds no LMS database connection. `PearsScheduleMigrationService.migrateLicence`
  does the same.
- **Running every licence.** `PearsLicenceWritebackAllEndpoint` (actuator
  `pears-licence-writeback-all`, LMS2-241) rebuilds every licence's positions from PEARS in one call.
  It uses virtual threads and two limits, `pears.writeback.parallelism` (licences being read, kept
  below the PEARS pool) and `pears.writeback.write-parallelism` (licences being written). The
  schedule migration still runs as a background job (section 4), because at ~450k rows a single
  call gives no progress or retry. It can borrow the two limits if one licence at a time is too
  slow.
- **Naming.** In the existing code, "writeback" (`LicenceWritebackService`,
  `pears-licence-writeback`, the licence page's "resync from PEARS", LMS2-196) means **PEARS → LMS**:
  rebuilding a licence's positions from PEARS. In this plan, "write-back" means **LMS → PEARS**.
  Pick a different name for the LMS → PEARS classes and messages (e.g. "schedule publish") so the
  two aren't confused.
- **Resync doesn't touch schedules.** A licence resync clears and rebuilds positions only
  (`LicenceCleardownService`), so it is safe to run on a licence after its schedule is migrated.
- **Spring Boot 4.1** (EDU-7312): Jackson 3 (`tools.jackson`), for example when writing the queue's
  `notes` JSON.

### 1. Read PEARS: `PearsScheduleService` (`migration/pears/schedule`)

- `@Service @ConditionalOnPearsDataSource`, constructor-injected
  `@Qualifier("pearsDataSource") DataSource`, the same as `PearsLicenceService`. No new datasource
  configuration is needed: `pears.datasource.*` (read-only Hikari, `initialization-fail-timeout=-1`)
  already exists.
- SQL in `src/main/resources/pears-migration/`, loaded with the same `sql(name)` classpath helper:
  - `licence-schedule.sql`: one licence's schedule, every version with its events, in **one
    query** so versions and events are read as of the same moment. It reads
    `ped_licence_schedule_details` with `OUTER APPLY XMLTABLE(... FOR ORDINALITY)` (the xview's
    own `BASETABLE_STATEMENT`, plus the event's position), not `xview_ped_lic_schedule_events`:
    the xview has no event position, which the link table records (section 6), and reading the
    base table needs no xview grant. A version with no events still comes back. Padding rows
    (`DUMMY_EVENT`) are returned flagged and dropped in Java, so positions match the stored XML.
    Values are returned as PEARS holds them; only `ORIGIN_DATE` and `EVENT_DATE` are converted
    (null when missing or unreadable).
  - `schedule-licence-references.sql`: every licence with at least one schedule version (current
    or not). The service keeps only those whose prefix `LicenceType.getFromPrefix` maps to a
    PEARS-managed type, and logs the rest.
  - `schedule-event-types.sql`: `SELECT mnem, short_title, display_seq FROM pedmgr.ped_template_event_types`.
- **Grant check:** `checkReadable()` tries each table the queries read (`ped_licence_master`,
  `ped_licence_schedule_details`, `ped_template_event_types`) and throws
  `MigrationPreconditionException` naming every one it can't, so a run fails at the start rather
  than licence by licence.
- Return immutable records (`PearsSchedule(licence, List<PearsScheduleVersion>)`, where
  `PearsScheduleVersion(plsdId, start, end, originDate, List<PearsScheduleEvent>)`),
  matching `PearsLicencePositions` / `PearsLicenceReference`. `PearsSchedule.currentVersion()`
  is empty when every version is end-dated (P1161) and throws if more than one is current.

### 2. Map: `PearsScheduleMapper`

A pure function from `PearsSchedule` + event type catalogue to a `MappedSchedule` value
(one `MappedScheduleVersion` per PEARS version, with its terms, phases, expiry, rates and other
events, + a `MigrationNotes`). Rates are mapped by `PearsRateMapper` ("Rates and yearly
recurrence"), other events by `PearsOtherEventMapper`, and `EventKeys` matches events across
versions ("Schedule history"). Comments on terms, phases and rates are carried as `MappedComment`s,
marked new only when they differ from the matched event's in the previous version. A PEARS rate's
comments go on its first LMS rate only, so a yearly rate split across terms doesn't repeat them.
Keeping it free of Spring and the database makes it unit-testable from fixtures, as
`PearsOperationMapper` is. The mapped versions are records, not unsaved entities: a term holds
PEARS's term number (1–4) and the expiry holds the PEARS `E` date, and the write step (section 3)
turns them into entities. So the mapper doesn't wait on `TermType.FOURTH`, `BESPOKE_MILESTONE`, the
percentage column or Open question 8. It:

1. Builds terms, then phases, then rates and other events, in that order, so anchors exist.
2. Skips events with an empty `event_mnem` per "Events with no event type" (ignored and only
   counted, whether empty or holding data). Throws a mapping exception for an unknown non-empty
   `event_mnem`, a `T`/`P` numbering it cannot place, or a licence where no version has an
   `ORIGIN_DATE`. A version with no `ORIGIN_DATE` of its own borrows another version's (see
   "Schedule history"). These are collected per licence, not thrown out of the whole run.
   Only the **active** version's failure skips the licence: a historical version that fails is left
   out and listed in `MappedSchedule.leftOutVersions` (see "Schedule history").

### 3. Write: `PearsScheduleMigrationService`

Modelled on `CarbonStorageLicenceMigrationService.migrateSchedules()`:

- **Idempotent**: skip licences returned by `licenceScheduleService.getIdsOfLicencesWithASchedule(...)`.
- Save through the existing services: `saveLicenceSchedules`, `saveLicenceScheduleDetails`,
  `saveLicenceStartDates`, `saveTerms`, phase/rate/other-event/expiry services, and
  `eventCommentRepository.saveAll`. Then call
  `licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(detail)` for **every**
  detail (replaced versions included), then
  `crossLicenceEventTrackerService.refreshScheduleCache(activeDetail)`.
- Transaction boundary: **one licence** (all its versions) per transaction, so a licence is either
  fully migrated with its history or not at all, and re-running picks up only what failed. PEARS is
  read and mapped **before** the transaction opens (`TransactionOperations`, as
  `LicenceWritebackService` does), so only the LMS writes hold an LMS connection.
- Save one `pears_schedule_event_migration_links` row per migrated event (see section 6), in the
  same transaction.
- Save one `pears_schedule_migrated_licences` row per migrated licence, in the same transaction.
  It marks the LMS schedule as created by the migration, which is what the cleardown (section 4a)
  uses to find what it may delete.
- **These two tables are migration-only, so they are created by a manual script**,
  `manualscripts/pearsschedulemigration/0001_pears_schedule_migration_tables.sql`, run by hand in
  each environment before the migration, as the carbon storage and PEARS licence migrations' tables
  are. Integration tests create them from a copy in `src/test/resources/migration`.
  `licence_schedule_area_events` stays in Flyway (V136): LMS's draft duplication reads it every time
  a draft is created, so it must exist everywhere. `pears_event_id` is held as text in both tables,
  as PEARS holds it.
  ```sql
  CREATE TABLE lms.pears_schedule_migrated_licences (
    licence_schedule_id UUID PRIMARY KEY REFERENCES lms.licence_schedules (id),
    licence_id          INTEGER NOT NULL,
    run_id              UUID NOT NULL,      -- no foreign key, so clearing runs leaves it
    migrated_at         TIMESTAMPTZ NOT NULL
  );
  ```
- Throw `MigrationPreconditionException` if PEARS returns no schedules at all (for example the
  wrong datasource or a missing grant), so an empty run cannot look like success.
- Exposes `migrateLicence(licenceReference, eventTypes, runId)`: migrates **one** licence in its
  own transaction and returns its outcome (`MIGRATED`, `SKIPPED` with reason, or `FAILED` with
  notes). The background worker (section 4) calls it per licence, with the event type catalogue
  read once for the run. `licencesToMigrate()` checks the grants and that PEARS holds schedules
  (the precondition above) and lists the licences. The service does not loop over the estate
  itself. Built in PR 5 (`PearsScheduleMigrationService`, `PearsScheduleWriter`).
- **Until prerequisite A lands**, a licence with a bespoke milestone in any version is `SKIPPED`
  with that reason, and migrates on a later run once LMS can hold it. Licences with a Fourth term
  migrate, as `TermType.FOURTH` (prerequisite B) is on `develop`.
- **Expiry:** stored as `E - 1 day`, the final term's LMS end date (Open question 8, decided for now).

### 4. Run in the background: queue, worker and controller

**Decided:** the migration runs as a **background job**, not inside the HTTP request. At about
1,799 licences, 10,834 versions and ~450k LMS rows (see "Migration size (live, 27 October 2026)"), a
request would hit the load balancer/proxy timeout, leave the outcome unclear and give no progress.
The existing LMS migrations (carbon storage, industry teams, and the all-licence positions rebuild
`pears-licence-writeback-all`) run inside the request and LMS has no `@Async`, so this is new. It
reuses what LMS already has: ShedLock (`ShedlockConfiguration`) and `@Scheduled` jobs (e.g.
`LicenceScheduledJobService`).

**Tables (manual script):** like the tables in section 3, these are used only by the migration, so
they are created by hand in each environment it runs in, by
`manualscripts/pearsschedulemigration/0002_pears_schedule_migration_runs.sql`, not by Flyway.
Integration tests create them from the copy in `src/test/resources/migration`.

```sql
CREATE TABLE lms.pears_schedule_migration_runs (
  id            UUID PRIMARY KEY,
  kind          TEXT NOT NULL,          -- MIGRATE; CLEARDOWN (section 4a); VALIDATE, a dry run (section 5)
  status        TEXT NOT NULL,          -- QUEUED, RUNNING, COMPLETE, CANCELLED
  requested_by  BIGINT NOT NULL,        -- wua_id
  requested_at  TIMESTAMPTZ NOT NULL,
  completed_at  TIMESTAMPTZ
);

-- Only one run is worked at a time
CREATE UNIQUE INDEX pears_schedule_migration_runs_one_active_idx
  ON lms.pears_schedule_migration_runs ((true)) WHERE status IN ('QUEUED', 'RUNNING');

CREATE TABLE lms.pears_schedule_migration_queue (
  run_id             UUID NOT NULL REFERENCES lms.pears_schedule_migration_runs (id),
  licence_reference  TEXT NOT NULL,
  status             TEXT NOT NULL,     -- PENDING, MIGRATED, CLEARED (a cleardown), MATCHED or DIFFERS (a dry run), SKIPPED, FAILED
  attempts           INTEGER NOT NULL DEFAULT 0,
  outcome            TEXT,              -- skip reason / failure summary
  notes              JSONB,             -- the mapper's migration notes, counted by reason
  started_at         TIMESTAMPTZ,
  finished_at        TIMESTAMPTZ,
  PRIMARY KEY (run_id, licence_reference)
);
```

Built in PR 7, in `migration/pears/schedule/run`: `PearsScheduleMigrationRunService` (the runs and
queue), `PearsScheduleMigrationJob` (the worker) and `PearsScheduleMigrationController`.

**Controller** (`@Profile("migration")`, only where the PEARS datasource is configured,
`/migration/pears-schedules`). Starting a run takes GET as well as POST, as on the other migration
controllers, so it can be started from a browser. Retry and cancel are POST only, so a link can't
trigger them; `/migration/**` is exempt from CSRF, so they can be sent from the browser console with
`fetch(..., {method: 'POST'})` on a logged-in LMS page. Taking a batch, retrying and cancelling lock the
run, so a cancel is never overwritten, and two requests that would make runs active at once get a 409 rather than both succeeding.

| Endpoint | Does |
|---|---|
| `POST /migration/pears-schedules` (optional `licenceReference`) | Reads the in-scope licence list from PEARS, creates a run and one `PENDING` queue row per licence (or just the one licence), returns **202 Accepted** with the run id and the number of licences queued. Returns 409 (`MigrationPreconditionException`) if PEARS returns no licences, holds no schedule for the licence given, or a run is already `QUEUED` or `RUNNING`. |
| `GET` or `POST /migration/pears-schedules/validate` (optional `licenceReference`) | Queues a dry run (section 5): each licence is migrated and compared with PEARS, then rolled back. 202 with the run id; progress, and the licences with issues, through the run status endpoint. 409 as for a migration run. |
| `GET /migration/pears-schedules/{runId}` | Run status: counts by status, requested/completed, and the failed and skipped licences with reasons and attempts. 404 for an unknown run. |
| `GET /migration/pears-schedules/{runId}/report` | Every licence of the run as a CSV download: status, attempts, the outcome (for a dry run, its issues), a column per comparison result for a dry run, and the mapper's notes. 404 for an unknown run. |
| `POST /migration/pears-schedules/{runId}/retry-failed` | Sets `FAILED` rows back to `PENDING` (e.g. after the licensing team fixes data in PEARS), and reopens the run if it had completed. 409 if the run was cancelled (start a new run instead), or it had completed and another run is active. |
| `POST /migration/pears-schedules/{runId}/cancel` | Marks the run `CANCELLED`. The worker stops claiming its rows, and a licence mid-flight finishes. 409 if the run has already finished. |

**Worker** (`PearsScheduleMigrationJob`, `@Profile("migration")`, only where the PEARS datasource is
configured):

- `@Scheduled(fixedDelayString = "${pears.schedule-migration.poll}")` (`PT5S`) with
  `@SchedulerLock(name = "pearsScheduleMigration", lockAtMostFor = "PT30M")`, so only one LMS
  instance works a run at a time.
- Each pass takes a batch of `PENDING` rows, in licence reference order, from the oldest
  `QUEUED`/`RUNNING` run (`pears.schedule-migration.batch-size`, 10; tune after the UAT rehearsal),
  and starts the run if it was queued. The PEARS event type catalogue is read once per pass. For
  each licence it checks the run is still active, calls
  `PearsScheduleMigrationService.migrateLicence` (one transaction per licence), then records the
  outcome, notes, attempts and timings in a separate short transaction. A licence whose migration
  throws is recorded as `FAILED` with the message, and the batch carries on.
- **Two instances on one licence:** should the lock expire while a batch is still running, the
  outcome is recorded only if the row is still `PENDING` (the row is locked to check), so the first
  outcome stands. The second migration of the licence is skipped anyway, as it already has a schedule.
- When no `PENDING` rows remain, the run becomes `COMPLETE`.
- **Restarts and deployments:** a licence in flight when an instance stops is rolled back and stays
  `PENDING`, so the next pass carries on. Re-running is safe anyway, because licences with a schedule
  are skipped.
- **PEARS load:** one licence at a time, so the worker holds at most one PEARS connection. The PEARS
  pool is shared with the positions write-back, so it is left as it is.

**Operational notes:**

- Time the largest licence (66 schedule versions as of 27 October 2026) in the UAT rehearsal. It is the biggest single
  transaction.
- Run the **current-only** rehearsal first (~1,744 versions, ~35k events), then the full history.
- **Go-live:** the final run must be `COMPLETE`, with failures reviewed or accepted, before the
  `RD`/`PA` EPMQ round trip and the write-back are switched on (see "Write-back transport").
- The comparison report (section 6) and the validation test (section 5) are unchanged. The
  validation test stays a separate Gradle task.

### 4a. Clear down, so the migration can be run again

**Decided:** in any environment other than production, the migrated schedules and the migration's
progress data can be cleared, so the same environment can be migrated again (after a mapper fix,
a PEARS data fix, or for the next rehearsal).

**Only the schedule goes, never the licence.** The cleardown deletes the migrated **schedule** and
the rows that hang off it. The licence (`licences`) and everything else about it stay as they
are: its positions, licensees, contacts, statuses, applications and so on. After a cleardown the
licence is simply a licence with no schedule, which is what the migration expects to find.

**What is cleared, per migrated schedule** (those in `pears_schedule_migrated_licences`), in one
transaction per schedule and children first. Bulk deletes, as `LicenceCleardownService` does for
positions, so rows that no longer load are cleared all the same. Every delete is limited to the
schedule's own rows: by `licence_schedule_id`, by its versions, or by its events' ids or
`original_event_id`s, never by licence alone:

| Data | Tables |
|---|---|
| Reminders already sent about its events | `licence_reminders`: by `schedule_event_id`, plus the licence's `LICENCE_EXPIRY` rows with no `original_event_id` (expiry reminders belong to the licence, and their event is optional) |
| Missing-contact reports about its events | `licence_reminder_missing_contacts`: by its events' `original_event_id`s, plus the licence's `LICENCE_EXPIRY` rows (no `original_event_id`), since the expiry comes only from the schedule |
| The event tracker's copy of its events | `licence_event_cache`: by its events' `original_event_id`s, not by licence |
| Comments | `event_comments` (by `event_reference_id`, which is the `schedule_events` id since V98) |
| Work programme activity statuses | `work_programme_activity_statuses` (none on migrated schedules, but users may add some) |
| Every event of every version | `licence_schedule_rates`, `other_schedule_events`, `work_programme_activities`, `licence_schedule_expiry_dates`, `_phases`, `_terms`, then `schedule_events` |
| Area events and links | `licence_schedule_area_events`, `pears_schedule_event_migration_links` |
| Versions and the schedule | `licence_start_dates`, `licence_schedule_details`, its `pears_schedule_migrated_licences` row, then `licence_schedules` |
| Audit history | the matching `_aud` rows. Events and versions are found through `schedule_events_aud` and `licence_schedule_details_aud` as well as the live tables, so the history of events a user has since deleted goes too. Those two and `licence_schedules_aud` go last. |

The whole schedule goes, including any version a user has applied in LMS since the migration, so
the next run starts from what PEARS holds.

**Applications:** none are expected. Environments are reset before each migration run, so no
application can reference a migrated schedule. The cleardown doesn't check for them: if one did
exist, the foreign key from `schedule_applications` (or `licence_continuation_application_details`,
`swp_record_of_decision_work_programme`) would stop the delete, that licence's transaction would
roll back, and it would be recorded as `FAILED`.

**What is never touched:**

- **The licence itself**, and anything not part of its schedule (see above).
- **Schedules LMS created itself** (no `pears_schedule_migrated_licences` row), e.g. carbon storage.
  Asked to clear one, the cleardown skips it.
- **PEARS.** The cleardown is LMS only. If the write-back or the `RD`/`PA` round trip has been
  switched on in that environment, PEARS still holds what LMS wrote, and `ped_lms_schedule_sync`
  still marks the licence as owned by LMS. Reset those in PEARS separately.

**Progress data:** when a cleardown run completes and no migrated schedule is left, the run and
queue rows (`pears_schedule_migration_runs`, `pears_schedule_migration_queue`) of every earlier run
are deleted, leaving only the cleardown's own run as a record of what was cleared. A single-licence
cleardown therefore keeps run history unless it cleared the last migrated schedule. The validation test
needs nothing cleared: it migrates into rolled-back transactions and rewrites its report file
(`build/reports/pears-migration/…`) on every run.

**How it runs:** as a run of its own through the same queue and worker (section 4), with
`kind = CLEARDOWN` on `pears_schedule_migration_runs`, one queue row per migrated schedule, so it
shows progress, survives a restart and reports skipped licences. A cleared licence is recorded as
`CLEARED`; the worker doesn't read PEARS for a cleardown. Failed licences can be retried as for a
migration.

**The reminder job:** it reads every due deadline first and saves the reminders afterwards, so it
could save one about a schedule cleared in between. Each licence's cleardown therefore starts by
locking the job's ShedLock row (`licenceReminders`). If the job holds its lock, the licence is put
off: it stays `PENDING`, and the worker stops the pass and tries again on the next one. Otherwise
the job can't start until the cleardown commits, as ShedLock takes its lock by updating that row,
so it reads its deadlines after the schedule has gone. This matters most for expiry reminders,
which belong to the licence alone. A reminder about an event is also held back by the cleardown
locking the schedule's events first: one being saved is waited for and deleted with the rest.

| Endpoint | Does |
|---|---|
| `POST /migration/pears-schedules/cleardown` (optional `licenceReference`) | Queues a cleardown of every migrated schedule (or one licence's) and returns **202** with the run id. Progress through the usual status endpoint. 409 if nothing (or not that licence) was migrated. |

POST only, like retry and cancel: from the browser console of a logged-in session,
`fetch('/lms/migration/pears-schedules/cleardown?licenceReference=P1556', {method: 'POST'})`.

**Guards:**

- `@Profile("migration")`, like the rest, **and** `pears.schedule-migration.cleardown-enabled`
  (default `false`, set from `PEARS_SCHEDULE_MIGRATION_CLEARDOWN_ENABLED`). It is never set in
  production; a request without it returns 404.
- Returns 409 while a migration or cleardown run is `QUEUED` or `RUNNING`.

### 5. Validate: `PearsScheduleMigrationValidationTest`

Modelled on `PearsPositionMigrationValidationTest`:

- `@Tag("pears-migration")`, run by the existing `pearsMigrationTest` Gradle task, and
  `@EnabledIfEnvironmentVariable(named = "PEARS_DATASOURCE_URL")`. Narrow the run with
  `PEARS_MIGRATION_LICENCES`.
- For every PEARS licence with a schedule: migrate it into the test database, run the calculation
  service, then compare **every** LMS date (term start/end, phase start/end, rate start, other event
  date, expiry) with the PEARS `EVENT_DATE`.
- Record counts and differences in a report (reuse `MigrationValidationReport` / `Tally` /
  `ReportText`). Each difference is a discrepancy to investigate, not a build failure.

**As built (PR 10, 19 November 2026):**

- Each licence, in parallel and in its own rolled-back transaction: create it in the test database,
  migrate it through `PearsScheduleMigrationService` as a run would (the writer runs the calculation
  service), then compare **every version** through `PearsScheduleComparisonService`, the comparison
  report's own model (section 6), so the sweep and the report can't disagree.
- **Fails** a licence with an issue to look into (a red row in the report), or whose schedule the
  migration maps but can't write (a bug, as AL10's was). **Skips** a licence the migration fails or
  skips for its PEARS data (e.g. "No terms in PEARS"): data to fix in PEARS, not a bug. Either way
  the report counts it. A licence that already has a schedule, as on a restored copy of a real LMS
  database, is compared as it is, as the dry run does.
- The report, `build/reports/pears-migration/schedule-comparison.txt`
  (`PearsScheduleValidationReport`, on `Tally` and `ReportText`): licences swept; comparison rows by
  result, every version; migration outcomes by reason; the mapper's notes; licences per result; the
  issues, each with an example row; events not migrated, by reason; and licences that couldn't be
  compared.
- Run against dev or UAT PEARS with the PEARS datasource set, narrowed if wanted:

  ```
  PEARS_MIGRATION_LICENCES=P1556,PL211 ./gradlew pearsMigrationTest --tests '*PearsScheduleMigrationValidationTest'
  ```

**Dry run, from an endpoint (PR 10):** the same check in any environment with the `migration`
profile, such as UAT, without PEARS credentials on a developer's machine and without changing the
environment. `GET` or `POST /migration/pears-schedules/validate` (optional `licenceReference`)
queues a `VALIDATE` run through the run queue and worker (section 4). For each licence,
`PearsScheduleValidationService`, in a transaction of its own that is always rolled back, migrates
it as a run would, or takes the schedule it already has (noted "Already had a schedule in LMS:
compared as it is"), and compares every version with PEARS. The queue row keeps what is learnt, as
the rest is rolled back:

| Status | When | Outcome | Notes |
|---|---|---|---|
| `MATCHED` | No issues | | The mapper's notes, and `Result: …` counts per comparison result |
| `DIFFERS` | Issues to look into | The first 20 issues, a line each | As for `MATCHED` |
| `SKIPPED`, `FAILED` | The migration skips or fails the licence, or the dry run can't be made | The reason | The mapper's notes |

Every licence of the run, with its issues and counts, downloads as a CSV from
`/migration/pears-schedules/{runId}/report`. The run status endpoint adds, for a dry run, `withIssues` (the `DIFFERS` licences and their
issues) and `comparisonResults` (the rows per result across the run). Failed licences can be
retried and the run cancelled as for any run. To see a licence's issues in full, migrate it and
use the comparison report (section 6).

### 6. Compare selected licences: `PearsScheduleComparisonController`

A comparison report for **chosen licences**, separate from the full-estate validation test, so the
licensing team can check specific schedules side by side before and after migration, in any
environment.

It cannot be a single SQL script: PEARS is Oracle and LMS is Postgres, so the report is built in the
application, which already holds both connections (`pearsDataSource` and the LMS datasource).

**Trigger.** `@Profile("migration")`, GET + POST (the same reason as the other migration controllers):

```
/migration/pears-schedules/compare?licences=P1234,P5678          -> HTML table
/migration/pears-schedules/compare?licences=P1234&format=csv     -> CSV download
/migration/pears-schedules/compare?licences=P1234&versions=all   -> every version, oldest first
/migration/pears-schedules/compare?licences=all                  -> every licence the migration wrote
/migration/pears-schedules/compare?licences=all&issues=only      -> only the licences with issues
```

`issues=only` (added 19 November 2026, after local testing) keeps the licences with a row to look
into (shaded red, below), that couldn't be compared, or that the migration fails, and says how many
were compared. Rows not migrated on purpose don't count otherwise. The options combine, e.g.
`&format=csv` for the issues as a download. Comparing every licence reads each one from PEARS in
turn within the request, so on a full UAT run it may take minutes; the full-estate sweep is the
validation test's job (step 9).

By default it compares the current PEARS version with the `ACTIVE` LMS detail. If PEARS has saved
a version since the migration (as AL10 has on UAT), the table says which PEARS version the LMS
detail came from, so its rows showing as missing and LMS only are explained. With
`versions=all` it pairs each PEARS version with its LMS detail (through `pears_plsd_id` in the link
table) and prints one table per version, each headed with its PEARS `start_datetime` and LMS
status. It flags any version missing on either side, and any event whose `original_event_id`
differs from the event it was matched to in the previous version.

**Reads.**
- PEARS: the same `PearsScheduleService.licenceSchedule(...)` the migration uses, so the report
  shows exactly what the migration read.
- LMS: the licence's `ACTIVE` `LicenceScheduleDetail` (every detail with `versions=all`), with its
  start date, terms, phases, rates, expiry, other schedule events and area events, read through the
  existing services (no recalculation; it shows the stored dates).
- The mapper, run again on what was read: it says which PEARS events the migration leaves out on
  purpose (`Not migrated`) rather than lost (`Missing in LMS`), what each LMS rate and relinquishment
  percentage should be, the dates of a version dated from its offsets, and each event's key for the
  `original_event_id` check.

**Pairing PEARS events with LMS events.** The migration records where each LMS event came from in a
small link table (manual script, see section 3), so the report pairs rows exactly instead of guessing:

```sql
CREATE TABLE lms.pears_schedule_event_migration_links (
  schedule_event_id UUID PRIMARY KEY REFERENCES lms.schedule_events (id),
  pears_plsd_id     BIGINT NOT NULL,   -- ped_licence_schedule_details.id
  pears_event_mnem  TEXT   NOT NULL,
  pears_event_index INTEGER NOT NULL,  -- position in EVENT_LIST
  pears_event_id    BIGINT             -- EVENT_ID, when PEARS set one (kept for the write-back)
);
```

LMS events with no link (added in LMS after migration) are still listed, as `LMS only`.

**Output: a heading per licence, and a table per version under it.**

Each table is headed with the PEARS version (`plsd_id`, `start_datetime`, `ORIGIN_DATE`) and the LMS
detail (status, id, start date). As built (PR 9, 19 November 2026), the columns are PEARS #, Event,
Caption, PEARS offset, PEARS date, PEARS value (with its recurrence, e.g. `1200 YEARLY`), LMS basis,
LMS date, LMS value, Result and Detail (the description, comments, or why an event wasn't migrated).
The example below, from the design, shows the idea with fewer columns:

| Event | Caption | PEARS offset (from licence start) | PEARS date | LMS basis | LMS date | Result |
|---|---|---|---|---|---|---|
| Term | Initial Term | 0y 0m 0d | 01-Dec-2014 | licence start | 01-Dec-2014 | Match |
| Term | Initial Term end (`T1`) | 4y 0m 0d | 01-Dec-2018 | Initial Term 4y ends 30-Nov-2018; Second starts | 01-Dec-2018 | Match |
| Bespoke milestone | — | 8y 11m 29d | 30-Nov-2023 | Second Term start +4y 11m 29d | 30-Nov-2023 | Match |
| Bespoke milestone | — | 9y 3m 30d | 31-Mar-2024 | Second Term start +5y 3m 30d | 31-Mar-2024 | Match |
| Other activity | Drill-or-drop | … | … | … | … | Differs by +1 day |
| Expiry | Licence Expiry Date | … | … | expiry date | … | Missing in LMS |
| Expiry | Final term end (`T3` = `E`) | 26y 0m 0d | 01-Dec-2040 | Third Term 12y ends 30-Nov-2040 (boundary 01-Dec-2040) | 01-Dec-2040 | Match |
| Expiry | Licence Expiry Date record | 26y 0m 0d | 01-Dec-2040 | expiry record (Open question 8) | 30-Nov-2040 or 01-Dec-2040 | Expected convention difference or Match |
| Expiry | (licence with no `E`) | — | none | final term ends day before final `T`; no expiry record | — | No expiry in PEARS |

(The Term, Bespoke milestone and final-term rows are real licence P2244 data. The Drill-or-drop and Licence Expiry Date rows only illustrate the other result types.)

- **PEARS offset** shows the absolute offset PEARS stores. **LMS basis** shows the anchor and
  relative offset LMS stores (or "follows previous term" for chained terms/phases), so the
  difference between the two offset models is visible on every row.
- **Result**, as built, is one of:
  - `Match`.
  - `Differs by ±n days`: the LMS date isn't the PEARS date. A term, phase or expiry is compared on
    the day after LMS's end date, which is where PEARS dates it. Where PEARS's date isn't used
    (a version dated from its offsets), it is measured from the date expected instead.
  - `Value differs`: the rate or relinquishment percentage isn't what the migration makes of PEARS's.
  - `Carried on`: a further LMS rate from one yearly PEARS rate, in a later term (rate option A),
    checked against what the mapper makes of it. These were the extra rows found in local testing.
  - `Dated from its offset`: PEARS's date isn't used, because the version's dates are `1900`-based
    or the event has none, and LMS has the date worked out from the offset.
  - `No start date in PEARS`: the start row of a version that borrowed another's start date.
  - `No expiry in PEARS`: a row of its own when PEARS has no `E` and LMS no expiry.
  - `Original event differs` (with `versions=all`): the event's `original_event_id` isn't the id of
    the first migrated event with the same key, so LMS wouldn't treat them as one event.
  - `Missing in LMS`: the migration maps the PEARS event, but no LMS event is linked to it.
  - `Not migrated`: an untyped event holding data, a `T` with no number, an event the rules leave
    out (e.g. a rate outside the terms, Open question 16), or every event of a version the migration
    leaves out or of a licence it fails or skips (for example one with a bespoke milestone, which LMS
    can't hold yet), with the reason shown above the table.
  - `LMS only`: an LMS event with no PEARS event in this version: added in LMS, or migrated from
    another PEARS version when PEARS has saved one since, which the row names.

  `PA`/`RD` events have no links, so each is paired with an LMS area event holding exactly what it
  holds (type, subtype, date, comments, `EVENT_ID`, area name, short name, type and version), and
  captioned as the timeline heads it.
- Rows to look into (`Differs`, `Value differs`, `Original event differs`, `Missing in LMS`, `LMS
  only`) are shaded red, and `Not migrated` yellow.
- Rows are ordered by PEARS date, or LMS date where PEARS has none to go by. The start, term and
  phase rows come first within equal dates, because a term mismatch explains the mismatches
  anchored to it, then PEARS's own order.
- A summary line per licence and for the whole request: counts per result.
- A licence that isn't in LMS, or that PEARS holds no schedule for, is listed with just that. A
  reference that isn't one, or an option the report doesn't have, is a 400.

**Reuse in the validation test.** `PearsScheduleMigrationValidationTest` builds the same comparison
model (`PearsScheduleComparisonService.compare(...)`) and fails nothing. It writes the same rows
into `MigrationValidationReport`, so the full-estate sweep and the per-licence report cannot
disagree.

### 7. Local testing against dev PEARS

Licences chosen on 19 November 2026 for migrating one at a time on a local LMS against dev PEARS
(`db-ogadev1`), each covering a case the migration handles differently. They were found by
profiling every dev PEARS schedule, then kept only where the licence exists in a local LMS with no
schedule yet. Revisit them after each change to the mapper or the writer, and with the comparison
report (section 6), for example `/migration/pears-schedules/compare?licences=P1556,PL211`.

**Setup:** run the two manual scripts (`0001_pears_schedule_migration_tables.sql`, then
`0002_pears_schedule_migration_runs.sql`), run LMS with the `migration` profile and
`PEARS_DATASOURCE_URL`, `PEARS_DATASOURCE_USERNAME` and `PEARS_DATASOURCE_PASSWORD` set, then start
each licence with `/migration/pears-schedules?licenceReference=…` and follow it at
`/migration/pears-schedules/{runId}`. Only one run is active at a time.

| Licence | Versions | Covers | Expected | Checked |
|---|---|---|---|---|
| P1556 | 3 | Mandatory relinquishment with a percentage; a version with no start date; a zero-length version (started and ended in the same second) | Migrated, noting the borrowed start date | **Yes (19 November 2026):** the active version matches PEARS row for row. This found the relinquishment percentage shown after a description that restates it, so the timeline now shows the percentage first. |
| PL211 | 2 | Mandatory relinquishment with a percentage, on a `PL` licence; only a `T(1)` (1986), then `E` in 2006 (pattern B) | Migrated, without the rates after `T(1)` | **Yes (19 November 2026):** migrated as rule 3 says, but its 10 rates from 1986 (20 across both versions) were dropped as outside the terms, including the current yearly 1200. Raised as Open question 16. The relinquishment (50%), the first rate, `T(1)` and the expiry match PEARS. |
| P1728 | 1 | Indefinite yearly rate; mandatory relinquishment without a percentage | Migrated | **Yes (19 November 2026):** matches PEARS row for row. The rates on the `T(1)` and `T(2)` boundaries start the next term, the indefinite yearly 7500 from 2022 is one rate as it is already in the last term, and the relinquishment has no percentage, with "50% Relinquishment" as its description. |
| AL10 | 3 | Counted yearly rate (the `AL` rate overlap); no expiry event | Migrated, with extra LMS rates where a yearly rate carries on into a later term | **Yes (19 November 2026):** matches PEARS (version 6800) row for row, with no expiry. No extra rates: the 25 recurring 6 times stops when the 50 starts in 2001, the 1000 recurring 50 times stops when the 1100 starts, and the yearly 1200 is already in the last term. The first run failed in the writer's area-event check on the untyped event at index 2; fixed in PR 5. UAT has a newer version (27478, a DoV of 8 June 2026 extending Term 3 and adding an expiry in 2028) that dev doesn't. |
| P1986 | 3 | Phases | Migrated | **Yes (19 November 2026):** matches PEARS row for row. Its only phase, C, runs from the start to the end of the Initial term; the rates on the `T(1)` and `T(2)` boundaries start the next term; the relinquishment with no comments gets the default description, and the one with no percentage has no value. |
| P1987 | 15 | Phases A, B and C | Migrated | **Yes (19 November 2026):** matches PEARS row for row. Phases A, B and C follow each other inside the Initial term, C ending with it; the rates on the A and B boundaries start the next phase ("In PHASE_B", "In PHASE_C"), and those on the `T(1)` and `T(2)` boundaries the next term. |
| P1162 | 2 | Fourth term | Migrated, the fourth term as `TermType.FOURTH` | **Yes (19 November 2026):** matches PEARS row for row. `T(4)` is a `FOURTH` term from the day `T(3)` ends to the expiry, and the rates from the `T(3)` boundary on are in it. The relinquishment's 75% is only in its comments, so it has no percentage. |
| P1920 | 2 | `DD` / `2M` / `2N` | Migrated as other activities | **Yes (19 November 2026), `DD` only:** on dev both versions have a `DD` and neither has a `2M` or `2N`, so those are covered by the mapper's unit tests alone. The `DD` is an `OTHER_ACTIVITY` "Drill n Drop" (the type's short title) with its comments as the description, in both versions. In the first version, saved before the licence started, PEARS dates it 1902-01-01; LMS works it out from the offset as 2015-08-06, the same as the current version. The active version matches PEARS row for row. |
| P255 | 3 | Hand-entered area events (no area name) | Migrated, headed "Pre-producing area" or "Retention or development area" | **Yes (19 November 2026), area events only:** its 5 `PA` events migrate on the PEARS dates with their comments, and have no links. But it has a `T(1)` (1983) and no `E`, so its 11 rates from 1983 (40 rising to a yearly 290) fall outside its only term and aren't migrated: raised under Open question 16 (159 such licences on live). The timeline headings are still to check in the LMS screen. |
| PEDL236 | 33 | Named area events, area versions 1 and 2 | Migrated, headings `Name (SHORT)` and `Name (SHORTv2)` | **Yes (19 November 2026):** the active version matches PEARS row for row, with its 11 `RD` events (TEST AREA A and B) on the PEARS dates, unlinked. The rates on the `T(1)` and `T(2)` boundaries start the next term. The timeline heads Area A's version 1 events `TEST AREA A (TEST A)` and its version 2 events `TEST AREA A (TEST Av2)`, and Area B's `TEST AREA B (TEST B)`, including its `END` event. Two Area B events on the same day show in PEARS's event order. |
| P1161 | 2 | No current version | Migrated, the latest version made active | **Yes (19 November 2026):** the active LMS version is PEARS's later end-dated version (570), and matches it row for row. The rates on the `T(1)` and `T(2)` boundaries start the next term, and the relinquishment's 50% is only in its comments. Noted "No current version in PEARS" and, for its other version, "another version's start date used". |
| P1950 | 50 | Many versions, for timing | Migrated | **Failed on dev (19 November 2026), as designed:** its current version (8542) has an `SJ` event, a type the migration doesn't map, so the licence fails with "Event type 'SJ' is not known to the migration (version 8542)". `SJ` isn't in the event type catalogue on dev or live (S1), so this is bad dev data. Timed on PEDL236 instead: 33 versions in about 14 seconds, against about 6 for a one-version licence such as P1728, so most of a run is fixed overhead. (P1949, with 84 versions, and PEDL265, with 28, aren't licences in the local LMS, so they were skipped.) |
| PEDL192 | 1 | Untyped event; its only version has no start date | Failed: "No schedule start date in any PEARS version" | **Yes (19 November 2026):** failed with exactly that reason, on the first attempt. |
| P1972 | 2 | No terms | Not migrated (rule 7): failed with "No terms in PEARS" | **Yes (19 November 2026):** failed with "No terms in PEARS", on the first attempt. |

**Every licence, by dry run (19 November 2026):** a `VALIDATE` run (section 5) of every dev PEARS
licence with a schedule, on a local LMS: 1,109 licences in about 12 minutes. Every licence the
migration migrated matched PEARS, apart from schedules changed by hand in the local LMS, and every
failure is dev data or a decided rule:

| Status | Licences | Why | On live |
|---|---|---|---|
| `MATCHED` | 889 | | |
| `DIFFERS` | 3 | Changed by hand in the local LMS: P5 had a schedule made in LMS on dev, so none of PEARS's events are in it; P1556 and PL211 had their relinquishments moved 3 days to test the comparison | |
| `SKIPPED` | 103 | "Not a licence in LMS". 95 are `DRAFT` licences in PEARS, never issued, which the licence refresh leaves out. The other 8 (P153, P251, P287, P1781, P1820, P1952, P1977, PL260) are `LIVE` in `PED_LICENCE_MASTER`, but PEARS's own licence search doesn't find them either: the search (`PEDSEARCH`) only returns a licence with a current data point in the live series (`PED_SIM_ID = 0`), and none of the 8 has one. P251 and PL260 have data points only in simulations; the other 6 have none at all. So none of the 103 is a licence PEARS treats as issued, and every one that is was compared or failed for a reason above | Drafts are skipped the same way, as there is no LMS licence to hold the schedule |
| `FAILED` | 38 | "No terms in PEARS" (pattern E): every `ML` and `DL`, CE1, and some `P` and `PL` | Decided: not migrated, created in LMS by the licensing team |
| `FAILED` | 72 | "No schedule start date in any PEARS version", all `PEDL` | None: every current schedule has an `ORIGIN_DATE` (survey S10) |
| `FAILED` | 3 | Event types not in the catalogue: `SJ` (P1200, P1950) and `B` (P1929). Dev also has `SJ` on P1969 and PEDL277 and `B` on PEDL273, which aren't in the local LMS | None: no event in any version on live has a type outside the catalogue |
| `FAILED` | 1 | P1948: "Term 3 ends on 2014-02-08, on or before it starts on 2016-01-08". Its current dev version gives `T(3)` an offset of 1 month, a typo | None: on live its terms end in 2017, 2021 and 2039, the last with the expiry |

Across the licences compared, every version: 28,386 rows match, 15,926 are dated from their offset,
708 have no start date and 126 no expiry in PEARS, 18 carry on, and 3,582 PEARS events aren't
migrated (failed licences, and the events the rules leave out). The 5 `Differs`, 19 `Missing in
LMS` and 8 `LMS only` rows are all P5, P1556 and PL211.

**Every licence, migrated (19 November 2026):** after clearing down the schedules migrated so far
(section 4a), and removing P5's hand-made one, a `MIGRATE` run of every dev PEARS licence on the
same local LMS: 1,109 licences in about 13 minutes. It agreed with the dry run licence for licence:
892 migrated (the 889 that matched, plus P1556, PL211 and P5), and the same 114 failed and 103
skipped, for the same reasons. The comparison report of every migrated licence
(`/migration/pears-schedules/compare?licences=all&issues=only`) then listed no issues.

**Gaps in dev data:**

- **No bespoke milestones:** dev PEARS holds no `BM` events. Add one to a dev licence through the
  PEARS schedule screen when prerequisite A is built, and test both the skip before it lands and
  the migration as `BESPOKE_MILESTONE` after.
- **Licences missing from local LMS:** P1884, P1949 (84 versions, the largest on dev), P1983,
  P1991, P2001, P2037 and P2039 have schedules on dev PEARS but did not come through the PEARS
  refresh, so the alternatives above were used. They are all `DRAFT` licences, never issued, which
  the refresh leaves out (see the dry run above). The Gradle sweep (section 5) creates the licence,
  so it covers these.

## Migration size (live, 27 October 2026)

- **In scope (every licence with a schedule):** 1,799 licences, 10,834 schedule versions (largest
  licence 66), 1,798 current. Of these, `P` and `PEDL` are 1,745 licences and 10,239 versions. The
  other 54 licences (595 versions) are `PL`, `EXL`, `ML`, `DL`, `AL`, `CE`. The 40 schedules with no
  terms are not migrated (rule 7), so about 1,759 licences are migrated. That includes P1161,
  which has no current version ("Schedule history").
- **Events (all licences with a type):** 215,812 across all versions (35,735 current), about three-quarters of
  them rates.
- **Estimated LMS rows for the full history:** about **450k**.
- **By licence status (all typed licences):** 420 are extant (4,607 versions). 1,354 are
  surrendered, expired or revoked (6,113 versions), and 25 have no status (114 versions). All of
  them are migrated. The write-back and the `RD`/`PA` round trip only realistically come into play
  for the extant ones.

The queries, full results and breakdowns are in the data survey,
[S2](pears-schedule-migration-data-survey.md#s2-licences-and-schedule-versions),
[S3](pears-schedule-migration-data-survey.md#s3-events-by-type) and
[S18](pears-schedule-migration-data-survey.md#s18-licences-with-a-schedule-by-status).

## Delivery steps

1. **Data survey: done** (live, read-only, 27 October 2026). Queries, results and the list of items
   for the licensing team are in
   [pears-schedule-migration-data-survey.md](pears-schedule-migration-data-survey.md). Re-run it
   before the production run, since PEARS keeps changing. It covers:
   - schedules per licence type (PEARS prefix **and** the LMS `LicenceType` it maps to, onshore
     and offshore) and per `event_mnem`;
   - licences with no `ORIGIN_DATE`;
   - licences with no `E` event (migrated without an expiry record), more than one `E` event, or
     an `E` on or before the final term's start;
   - licences whose `T`/`P` events are irregular (missing numbers, duplicates, out of date order),
     a final `T` whose date differs from `E`, and a `T(1)` on or before `ORIGIN_DATE`;
   - licences with real `P` events: which phases are present (A–C, A+C, A only …), and those whose
     last `P` date differs from `T(1)`;
   - events dated exactly on a term boundary, by type (anchored to the term that ends there, except
     rates) — **not surveyed**: the rule is decided without counts, and the comparison report shows them;
   - events where LMS date arithmetic would differ from PEARS (run both formulas in SQL);
   - schedule versions per licence (total and max; how many are identical to the version before was
     **not surveyed**, since every version is migrated), and licences with no open version or more than one;
   - versions with no `ORIGIN_DATE`;
   - events with an empty `EVENT_MNEM`: empty rows and rows holding data (counted; they are ignored,
     see "Events with no event type");
   - `R` event `RATE` values: whole, 1–2 dp, more than 2 dp (to be rounded), and values that do not
     fit (listed); see "Rate values";
   - `RD`/`PA` events: count per licence type and subtype, events with no `RETENTION_AREA` block,
     subtype or `EVENT_ID`, and `RD` on `P` licences / `PA` on onshore licences;
   - `MR` events with `PERCENTAGE` set (current versions), how many are on the `T(1)` date, and how
     many already state the figure in `COMMENTS` (input to the percentage decision);
   - `R` events by recurrence (none, yearly with `RECURRANCES`, yearly indefinite, with
     `RECUR_END_DATE`), and gaps: recurrences that stop before the next rate or the final term
     end, and non-recurring `R` events followed by anniversaries with no `R` (these change the
     payments report after write-back);
   - events with an `EVENT_ID`, and same-type events with identical comments in one version
     (the cases where cross-version matching is ambiguous).
2. Take the survey's "For the licensing team" list to the licensing team before coding: the PEARS
   fixes (L2) and the items still to confirm or decide (L12, L14, L16), using
   the licensing team review document and licensing items spreadsheet (migration documents,
   provided separately; the spreadsheet also has the L9 list). Those include Open questions
   7 (how yearly-recurring rates are held in LMS) and 8
   (expiry date convention), which also need the data warehouse owners. The term and phase
   patterns are already decided (L5–L11).
3. Add `BESPOKE_MILESTONE` to `OtherScheduleEventCategory`, with its form/timeline tests, as its
   own change ahead of the migration.
3a. **Done (18 November 2026, Fivium/licensing-management-service#970):** `TermType.FOURTH`,
    offered only when the licence's active schedule version already has a Fourth term (see
    "Decision: four-term licences"). Its own change, ahead of the migration. The continuation
    visibility rules (`OtherRequirementsVisibilityResolverService`) don't list `FOURTH` yet.
3b. **Done (18 November 2026, LMS1-813):** the mandatory relinquishment percentage field in LMS
    (see "Decision: mandatory relinquishment percentage"), with form, validation (0–100, up to
    1 dp), timeline display and tests. The form requires it for a mandatory relinquishment; rows
    saved before it (and migrated ones without one) are null.
3c. **Done (28 October 2026):** `AL`, `CE` and `DL` map to `LANDWARD_PRODUCTION`, so they have the
    Initial, Second and Third term types (Open question 14). All 3 `AL` schedules fit them (data
    survey S8b).
4. `PearsScheduleService` + SQL resources + grant check.
5. `PearsScheduleMapper` + unit tests from fixture XML (include licence P2244's two BM events, and a
   multi-version licence to test `original_event_id` matching across versions, yearly rates
   that cross phase and term boundaries, and one licence per term pattern A–G from the data survey). Unit test the Java port of PEARS date arithmetic
   (`ADD_MONTHS` included) against values computed in Oracle.
6. `PearsScheduleMigrationService` + integration test (Testcontainers Postgres, mapper input stubbed).
   In the same step: `licence_schedule_area_events` (Flyway, V136), with `RD`/`PA` events copied as is from
   each migrated version's XML in the same per-licence transaction, and copied on draft duplication.
6a. LMS timeline display of area events (read-only, filterable), with tests.
6b. EPMQ: for migrated licences, `LICENSING_RETENTION_AREAS` (`apply_active_…`, `apply_removed_…`,
    `auto_expire_areas`) publishes the new `RD`/`PA` set instead of writing the schedule (PEARS
    change; licences whose schedule LMS doesn't own keep today's behaviour). LMS maps the message type and handles it (replace on `ACTIVE`
    and open `DRAFT` details, idempotent, ordered), with tests. Switch it on at go-live after a final
    `RD`/`PA` refresh.
6c. Write-back over EPMQ (separate piece of work, specified here): the EPMQ message library release
    (new LMS topic and message type), LMS publishing after commit on draft apply and after
    processing a PEARS `RD`/`PA` message (full payload, `RD`/`PA` included), the PEARS-side consumer
    (mechanism to be determined, Open question 10), first-version handling for new licences (no
    schedule, no template, sync row created, existing areas published),
    `apply_lms_schedule` (full rebuild, acknowledgement end-dating, sync table), and the round-trip
    test.
7. Background run: queue and run tables (Flyway), `PearsScheduleMigrationJob` (ShedLock-guarded
   worker), controller (start, status, retry-failed, cancel), with tests including restart mid-run
   and a second instance.
7a. Cleardown (section 4a): `PearsScheduleCleardownService`, the `CLEARDOWN` run kind and
    endpoint, behind `pears.schedule-migration.cleardown-enabled`, with tests that migrate, clear
    and migrate again, and leave other licences and LMS-created schedules alone.
8. `PearsScheduleComparisonService` + `PearsScheduleComparisonController` (HTML + CSV) + tests; the
   link table came with the writer. Build this **before** the first UAT run, so the licensing team
   can review chosen licences from the first run onwards.
9. `PearsScheduleMigrationValidationTest` against UAT PEARS, built on the comparison service. Iterate until differences are only
   known, accepted cases.
10. Rehearse on a copy of production data, then run in production with the `migration` profile.

### Pull requests

The migration code (steps 4–9) is delivered as a **stack of pull requests**. Each is small enough
to review on its own, and nothing reaches `develop` until the whole migration is ready.

**How the stack works:**

- **PR 1** branches from `develop` and targets `develop`. It stays open and collects the rest of
  the stack.
- **Each later PR** branches from the one before and targets it, so its diff shows only its own
  change.
- **Landing, one at a time:** when a PR is approved, it is added to its parent with GitHub's
  "Rebase and merge", so the history stays linear, and the next PR is retargeted to that parent.
  So PR 2 goes into PR 1, PR 3 is retargeted to PR 1 and goes into it, and so on. When the last PR
  is in, PR 1 holds the whole migration and goes into `develop` the same way.
- **Each PR builds and passes its tests on its own.** Nothing is reachable until PR 7 adds the
  controller, and everything is under `@Profile("migration")`, so a half-built stack is never
  live.
- **The stack is rebased, never merged.** No branch in the stack has merge commits:
  - **Keeping up with `develop`:** rebase PR 1 onto `develop` regularly, then each open PR above
    it onto the rebased one below (`git rebase --onto <rebased parent> <parent's old tip>`, so
    only the PR's own commits move).
  - **A fix lower in the stack:** commit it on that PR's branch, then rebase every PR above it the
    same way.
  - **Pushing:** each rebased branch is force-pushed with `--force-with-lease`, so a branch someone
    else has pushed to since is never overwritten. Anyone with a stack branch checked out resets to
    the new tip rather than pulling.
- **Flyway:** version numbers are checked against `develop` before PR 1 goes into `develop`, and
  renumbered if `develop` has taken them.

**Before the stack (straight to `develop`, each its own PR):** these are LMS features in their own
right, and the mapper needs them.

| PR | Delivery step | Contents |
|---|---|---|
| A | 3 | `BESPOKE_MILESTONE` in `OtherScheduleEventCategory`, with form and timeline tests. Dev PEARS has no `BM` events, so add one to a dev licence to test the migration with (section 7). |
| B | 3a | `TermType.FOURTH`, offered only where the active version already has a Fourth term. **Done, on `develop`** (Fivium/licensing-management-service#970, merged 18 November 2026): `FOURTH` is a historic term type, and `TermType.getSelectableTermsFor` leaves it out unless the active version has one. |
| C | 3b | Mandatory relinquishment percentage field: form, validation (0–100, up to 1 dp), timeline display. **Done, on `develop`** (LMS1-813, Fivium/licensing-management-service#957, merged 18 November 2026): `other_schedule_events.relinquishment_percentage` (V134), required for a mandatory relinquishment. |
| — | 3c | `AL`/`CE`/`DL` → `LANDWARD_PRODUCTION`: done, on `develop`. |

Only PR 5 (the write) needs A, B and C on `develop`; B and C are there, and the stack has been
rebased onto them. Rebase the stack onto `develop` again once A lands.
The mapper (PRs 2–4) needs none of them: its records hold PEARS's term number, its own category
for bespoke milestones, and the percentage as a number, and PR 5 turns them into `TermType.FOURTH`,
`BESPOKE_MILESTONE` and the new column.

**The migration stack:**

| PR | Delivery step | Contents | Done when |
|---|---|---|---|
| 1 | 4 | **Read PEARS.** `PearsScheduleService`, `licence-schedule.sql`, `schedule-licence-references.sql`, `schedule-event-types.sql`, the immutable records (`PearsSchedule`, `PearsScheduleVersion`, `PearsScheduleEvent`, `PearsScheduleEventType`) and the grant check. Only licences whose prefix `LicenceType.getFromPrefix` maps. | Unit tests over mocked JDBC, as `PearsLicenceServiceTest`; fails clearly on a missing grant. The three queries run by hand against dev or UAT PEARS for P2244, P1161 and a licence with `RD` events. |
| 2 | 5 | **Mapper: structure.** Java port of PEARS date arithmetic (`ADD_MONTHS` rules), start date, versions (`ACTIVE`/`REPLACED`, no-current-version rule), terms (rules 1–7, Fourth term), phases, expiry, untyped and unnumbered `T` events ignored and counted, `MigrationNotes`. | Unit tests from fixture XML: P2244, P2522, one licence per pattern A–G, P1161; date port tested against Oracle values. |
| 3 | 5 | **Mapper: rates.** Occurrence expansion (`RECURRANCES` counts), placement per phase/term, gap handling ((a): gaps taken as errors and noted, per L12 for now), fixed-count rates (PL116, AL10 overlap), rate values and rounding. Option A (Open question 7, for now). | Unit tests: P2522, P516, PL116, AL10, a yearly rate crossing phase and term boundaries; S14b dates reproduced (P1609). |
| 4 | 5 | **Mapper: other events.** `MR` with percentage, `BM`, `DD`/`2M`/`2N`, default descriptions, anchoring to a term, comments as `EventComment`s (new or changed only), `original_event_id` matching across versions. | Unit tests: P2244's two `BM` events, a multi-version licence, the percentage cases (`x`, `50%`, comment-only). |
| 5 | 6 | **Write.** `PearsScheduleMigrationService.migrateLicence` (one licence per transaction, idempotent), date calculation for every version, cross-licence cache refresh, `licence_schedule_area_events` (Flyway) with `RD`/`PA` copied as is and copied on draft duplication, `pears_schedule_event_migration_links` and `pears_schedule_migrated_licences` (manual script). Turns the mapper's records into entities: term 4 into `TermType.FOURTH`, its bespoke milestone category into `BESPOKE_MILESTONE`, the percentage into the new column, events sharing an `eventKey` into one `originalEventId`, and new `MappedComment`s into published `EventComment`s. Needs A, B and C on `develop`; until A lands, licences with a bespoke milestone are skipped. Expiry stored as `E - 1 day` (Open question 8, for now). | Integration test (Testcontainers Postgres, mapper input stubbed): migrate, re-run skips, failure rolls back the whole licence. |
| 6 | 6a | **Area events on the timeline.** Read-only, filterable display of `licence_schedule_area_events` (`ScheduleEventType.AREA`, `TimelineAreaEventView`), in the term or phase each event's date falls in. | Unit tests for the heading (versioned short name, hand-entered events), placement in terms and phases, and the filter; a controller test renders them. Licences without area events show no area events. |
| 7 | 7 | **Background run.** Run and queue tables (manual script, as they are migration-only), `PearsScheduleMigrationRunService`, `PearsScheduleMigrationJob` (ShedLock), controller (start, status, retry-failed, cancel). | Tests: restart mid-run, a second instance, cancel, retry. |
| 8 | 7a | **Cleardown.** `PearsScheduleCleardownService`, the `CLEARDOWN` run kind and endpoint, behind `pears.schedule-migration.cleardown-enabled` (section 4a). | Integration tests: migrate, clear, migrate again with the same result; every row hanging off the schedule goes, audit history of deleted events included; the licence, other licences' rows and an LMS-created schedule are untouched; run history goes once no migrated schedule is left; 404 without the property. |
| 9 | 8 | **Comparison.** `PearsScheduleComparisonService`, `PearsScheduleComparisonController` (HTML, CSV, `versions=all`). | Tests over a migrated fixture licence: every result type. |
| 10 | 9 | **Validation test.** `PearsScheduleMigrationValidationTest` (`pearsMigrationTest` task), built on the comparison service. | Runs against UAT PEARS; differences are only known, accepted cases. |

When PR 10 is approved and in PR 1, PR 1 is reviewed as a whole once more and goes into
`develop`. That is the code the UAT rehearsal (step 10) runs, with the comparison report available
from the first run.

**Not in this stack:** the `RD`/`PA` EPMQ round trip (6b) and the write-back (6c). Both span the
EPMQ message library and PEARS (Perforce), and switch on at go-live, so each gets its own stack,
planned with that work.

## Open questions

1. **History.** Resolved: the full history is migrated, with one LMS schedule version per PEARS
   edit and no merging (see "Schedule history").
2. **Term and phase boundaries.** Resolved (from licences P2244 and P2522, plus the business rule):
   the first term present starts on `ORIGIN_DATE`. A `T(n)` event is the **end boundary** of term
   n, the anniversary on which the next term present starts. Terms and phases may be missing but
   must be in order. Events up to and including a boundary belong to the term ending there, apart
   from rates, which start the next term. Every term pattern found by the data survey has a decided
   rule ("Term boundaries" rules 3, 5–7 and "Decision: four-term licences"). Phases follow the same
   convention (licence P2522: A and C, no B). See "Phase boundaries".
3. **Rates.** Resolved: `RECURRING=YEARLY` redefines the rate each year at the same value. Each
   yearly date is migrated against the phase or term it falls in (see "Rates and yearly
   recurrence"). How many LMS rows a yearly rate becomes: option A for now, see Open question 7.
4. **Mandatory relinquishment percentage.** Resolved (28 October 2026): the percentage becomes its
   own data point on mandatory relinquishment events in LMS, is migrated from PEARS, and is written
   back. See "Decision: mandatory relinquishment percentage".
5. **Onshore.** Resolved: onshore licences are `LANDWARD_PRODUCTION`, which LMS `TermType`
   `INITIAL`/`SECOND`/`THIRD` already supports, so they are in scope with no model changes. (The
   one term model change is `FOURTH`, for the 13 four-term `P` licences: see "Decision: four-term
   licences".)
   Retention/development areas (`RD`) and pre-producing areas (`PA`) are migrated in this exercise
   (see "Retention, development and pre-producing areas"). `RETENTION_AREA_RATE` is unused in PEARS
   and needs no migration (see "Retention/development area rate: unused").
   **Superseded by the scope decision:** every PEARS-managed licence type (`managedByLms = false`)
   with a PEARS schedule is migrated, not only those mapping to `SEAWARD_PRODUCTION` or
   `LANDWARD_PRODUCTION`. `PL`, `EXL`, `ML`, `AL`, `DL` and `CE` map to
   `LANDWARD_PRODUCTION`. Schedules with no terms (incl. all `ML`,
   `DL`, `CE`) are not migrated and are left to the licensing team (rule 7). See
   "Scope: licence types and statuses".
6. **Date mismatches.** Resolved (28 October 2026): **LMS lands on the same dates as PEARS.**
   Where LMS arithmetic on the PEARS offset would give a different date, the migration stores the
   `relativeDuration` (or term/phase duration) that reproduces the PEARS date, and lists every such
   event in the report.
   **Evidence (data survey, S14):** copying PEARS offsets would move 96 current events (0.3%: 94
   `P`, 2 `EXL`) by up to 3 days. The mapper already derives LMS durations and offsets from the PEARS dates
   (`Period.between`), which lands every one of them on the PEARS date, so the design already does
   this.
   **Per event (data survey S14b, 28 October 2026):** the PEARS offsets are themselves irregular
   (e.g. 7y 10m 16d), so these events were set by date in PEARS and the dates (month ends, the 1st,
   fixed rent days) are what was meant. 49 of the 96 are rates on no-terms schedules that aren't
   migrated, leaving 47. Example: P1609's initial term ends 31-Dec-2016 in PEARS and in LMS;
   copying the offset would have given 28-Dec-2016.
7. **Yearly-recurring rates in LMS.** **Option A for now (17 November 2026)**, built in PR 3. It
   changes later only if the regulator has a different view. Option A: one LMS rate per
   phase/term, with the PEARS write-back rebuilding recurrence. Option B: one LMS rate row per
   year, with a one-to-one write-back. The arguments for both are in "Decision: how
   yearly-recurring rates are held in LMS".
   **New evidence (data survey, S12/S12b, live 27 October 2026):** every current schedule is "one
   non-recurring `R` per payment, then one indefinite yearly `R`". `RECURRANCES` is never used on `P`/`PEDL`, but 9 `PL` and 3 `AL` schedules use it, so the write-back must be able to emit a count. But
   **About 310 gaps** exist (298 `P`, 12 `PL`), mostly on pre-1999 licences (e.g. an initial
   rental, then the next rate 6 years later). PEARS shows no payment in those years. Option A's
   write-back on its own (each LMS rate recurs until the next one) would **invent payments for
   every gap year**.
   **Gap handling (28 October 2026):** either the gaps are errors, and the rate should have run on
   (the write-back fills them), or they are legitimate, and LMS uses a **£0 rate** for those
   years (migrated in, written back as 0, and used by users for future gaps). See "Rates and yearly
   recurrence" step 4. Either way option A no longer invents payments, and a "recurs yearly" flag
   on LMS rates isn't needed for gaps. **Agreed with the NSTA (20 November 2026): the gaps are
   errors** (case (a)), so the previous rate runs on through the missing years, and each gap is
   noted in the migration report. The NSTA does a final check; any it finds legitimate would get
   the £0 rates of case (b).
   **Data warehouse (option A):** the write-back would put `RECURRANCES` counts on `P` and `PEDL`
   schedules, which have never had them. The data warehouse owners confirm their feed handles
   that. With the gaps agreed as errors, the 0-rate rows of gap case (b) are only needed for any
   gaps the NSTA's final check finds legitimate, so confirming those can wait for its answer.
8. **Expiry date convention.** **Option A for now (18 November 2026): store `E - 1 day`**, built
   in PR 5. It changes later only if the licensing team expects the anniversary. PEARS dates the expiry on the anniversary the licence ends
   (P2244: 01-Dec-2040). In LMS the final term ends the day before (30-Nov-2040), and Review and
   Apply (`ReviewAndApplyScheduleController` → `doesExpiryDateMatchEndOfFinalTerm`) shows a
   validation error unless `expiryDate` **equals** the final term end date.
   - **Option A, store `E - 1 day` (30-Nov-2040).** Every migrated schedule passes the LMS check,
     and LMS's expiry is consistently "last day of the licence". The PEARS write-back adds the day
     back (`E = expiryDate + 1`). The displayed expiry differs by a day from PEARS, licence
     documents and the data warehouse, so the comparison report shows it as an expected
     convention difference, not a mismatch.
   - **Option B, store `E` as-is (01-Dec-2040).** The date matches PEARS and the documents, and
     the write-back copies it. But every migrated schedule with an expiry fails the LMS
     expiry/final-term check the first time a draft is reviewed. Users would need to change it, or
     LMS's check would need changing to accept "day after the final term end".
   Recommendation: confirm with the licensing team which date LMS users expect to see as "Licence
   expiry". If it is the anniversary, option B needs the LMS check changing as a prerequisite.
   **Reminders (LMS change, November 2026):** the reminder job drops any deadline that falls on the
   licence's expiry date, because the expiry reminder (now 12 months ahead) covers it. Under option
   A the final term ends on the expiry date, so licensees get one reminder. Under option B the
   expiry is the day after the final term ends, so they get both: the expiry reminder 12 months
   ahead and the final term end reminder 6 months ahead, a day apart. Option B would need the
   reminder rule changing too. See "Event reminders after migration".
9. **Who maintains areas in phase 1.** Resolved: **PEARS stays the owner of the areas**. For
   migrated licences it no longer writes the schedule itself: an area change publishes the new
   `RD`/`PA` events to LMS, LMS updates its schedule, and LMS's write-back rebuilds the whole PEARS
   schedule XML, `RD`/`PA` included. See "Keeping `RD`/`PA` events in step (EPMQ)" and "Write-back
   transport (EPMQ, LMS → PEARS)".
10. **How PEARS picks up the LMS events message.** Open, to be determined with the EPMQ and PEARS
    owners. A mechanism is needed that receives `LMS_LICENCE_SCHEDULE_UPDATED` and runs
    `apply_lms_schedule` to recreate the latest schedule XML. Nothing in PEARS consumes EPMQ today.
    The requirements it must meet are under "Write-back transport (EPMQ, LMS → PEARS)", Consumer.
11. **Licence documents for new licences.** Open. PEARS licence documents need the schedule (term and
    phase lengths, mandatory surrender percentage, schedule summary). For a new production licence
    the schedule only reaches PEARS after it is created and applied in LMS and written back. Options:
    the raise process waits for the LMS schedule before documents are generated, documents are
    regenerated once it arrives, or document generation moves to LMS. Confirm the raise process
    order with the licensing team.
12. **No template for LMS-created schedules.** Open. New licences have no `ped_ts_id`. Either the
    PEARS code that reads a licence's template is changed to cope with none, or `apply_lms_schedule`
    creates a placeholder template per licence. The Fox schedule screen is no longer a concern: it
    redirects to LMS for licences whose schedule LMS owns. Remaining readers to check include Raise Licence
    (`PED017X`, which joins `ped_template_schedules` on `ped_ts_id`) and licence document data.
13. **LMS-only "Other activity" events.** Open, pending a check of what PEARS currently reports on.
    If they are not reported on, events whose category has no PEARS event type are **not written
    back** and stay in LMS only. Starting points for the check (PEARS consumers of schedule events):
    - **Filter to specific types, so unaffected:** `PED_LICENCE_PAYMENTS_REPORT`,
      `PED_LICENSED_ACREAGE_REPORT` (`R` only); the ESRI licence views
      (`ESRIMGR.CURRENT_LICENCE_*`, `PEDMGR.ESRI_LICENCE_HISTORY_VIEW`) (`E`, `T` only).
    - **Read all event types, so check whether new types would be expected:**
      `PED_LICENCE_SCHEDULE_CHANGES`, `PED_LICENCE_RAISE_REPORTS`, `LICENSING_WEB_SERVICES`,
      `template_schedule.scribe_schedule_events` (schedule text summary), acknowledgements
      (`generate_lic_acknowledgements`, only for acknowledgeable types).
    - The external data warehouse feeds from `xview_ped_lic_schedule_events`, which are not in this
      repository.

14. **Schedules on `AL` licences (and `DL`, `CE`).** Resolved (28 October 2026): LMS now maps
    `AL`, `CE` and `DL` to `LANDWARD_PRODUCTION` instead of placeholder types, so schedules can be
    created and maintained for them with the Initial, Second and Third terms and the landward
    production phase and feature rules. All 3 `AL` (30 versions, 1 extant: AL10) have a clean
    `T(1)`, `T(2)`, `T(3)` = `E` (data survey S8b) and migrate like `PEDL`. `DL` and `CE` have no
    terms (pattern E), so they are not migrated (rule 7), but the licensing team can now create them
    in LMS.
    **Still open, rate overlap:** all 3 `AL` have `25` yearly ×6 from their start, reaching the year `50`
    starts, so PEARS may pay both in that year (AL6 1999, AL9 2000, AL10 2001; data survey S12c).
    LMS and the write-back drop the overlap ("Rates and yearly recurrence" step 5). The licensing
    team confirms on the "AL rate overlap" sheet that the double payment was never intended.
15. **Event reminders for migrated licences.** Open. Once a licence is migrated, LMS's reminder
    job emails its licensees about deadlines in its active schedule. On the first run that is every
    extant licence with a deadline in the window, all at once. Decide when reminders start for
    migrated licences, and whether PEARS sends anything equivalent today that would need to stop.
    See "Event reminders after migration".
16. **Licences whose rates carry on after their last term (pattern B, and no expiry).** Open, **for
    the regulators** (raised 19 November 2026). Under "Term boundaries" rule 3 these licences are
    migrated with no term after the last `T`, so **every rate from the last `T` onwards falls
    outside the terms and is not migrated**, including the rent being charged today. The licence is
    still counted as migrated, with the dropped rates only noted. Rule 3 was decided on terms and
    the expiry alone; the rates weren't considered.

    Found testing PL211 against dev PEARS (`T(1)` 1986-04-05, `E` 2006-04-05 on dev): LMS has the
    Initial term to 1986 and the expiry, but none of the 10 rates from 1986 to 1995 (90 rising to
    a yearly 1200).

    **Live (query of current versions, 19 November 2026):** 52 licences have a last numbered `T`
    before their `E` (the survey's L6 estimated about 45): 33 `P`, 4 `PEDL`, 13 `PL` and 2 `EXL`,
    15 of them extant. 46 lose rates, including the rent charged today on 14 of the 15 extant
    licences, e.g. PL211 (`E` 2036 on live; rates from 1986 rising to 1,200 a year), PL116 (`T(1)`
    1975, `E` 2045; rates from 1975 rising to 110 a year) and P516 (`T(1)` 1991, `E` 2021; 400
    rising to 6,000 a year). The other 6 (P726, P774, P799, P967, P1192, P2316) end on a yearly
    rate that started before their last `T`, so they lose no rate rows but have no term for their
    remaining years. The full list, with each licence's rates, is the "L6 Terms before expiry"
    sheet of `pears-schedule-migration-licensing-items.xlsx`.

    **No expiry.** The same happens without an `E`: the final term ends on the last `T` (as for
    any licence with no `E`), and every rate from it onwards is dropped. Found testing P255 against
    dev PEARS (`T(1)` 1983-11-30, no `E`): LMS has only the 25 from 1977, not the 11 rates from 1983
    rising to a yearly 290. On live (query of current versions, 19 November 2026) **159 licences**
    have no `E` and rates dated on or after their last numbered `T`: all `P` licences from 1964 to
    1989 with only a `T(1)`, or a `T(1)` and `T(2)`, ending between 1970 and 2007. 136 are extant,
    and all 136 lose the rent charged today, e.g. P1 (`T(1)` 1970; rates from 1970 rising to 290 a
    year) and P686 (`T(2)` 2007; rising to 6,000 a year): 1,902 rates in all. They are the "L6b No
    expiry after last term" sheet of the same spreadsheet. (P255 itself isn't in either live list:
    its live schedule differs from dev's.)

    **Question for the regulators:** between its last recorded term and its expiry (PL211: 1986 to
    2036), or since its last recorded term where there's no expiry (P1: since 1970), what term was
    the licence in? Should LMS show a further term, and to when?

    Options:
    - **A. Add the next term from the last `T` to `E`.** PL179's DoV note suggests this. The rates and
      events in the gap migrate, and the expiry then matches the final term's end, so Review and Apply
      won't fail on these licences later. For P1159 (last `T(3)`) the next term would be a Fourth
      term, which only the 2004–2009 four-term licences have ("Decision: four-term licences"), so it
      needs its own answer. For the licences with no `E` there's nothing to end the added term on,
      and LMS works a term's end out from its length, so the regulators would have to give an end
      date.
    - **B. Keep rule 3.** The rates stay unmigrated and reported. The licensing team adds the
      missing term and rates in LMS after the migration, and the PEARS write-back must stay off for
      these licences until they do, or it deletes the PEARS rates (as rule 7 explains for
      schedules with no terms).

    **Until answered:** B, as built. The next term is not created, and option A is not built unless
    the regulators choose it. Asked as question 2.5 of the licensing review document
    (`pears-schedule-migration-licensing-review.docx`), which no longer lists rule 3 as agreed.

    **Rate gaps (20 November 2026):** the NSTA agreed that years with no rate between two rates are
    errors and the previous rate runs on through them (L12, "Rates and yearly recurrence" step 4).
    If the same holds here, these licences' rents carry on after their last term too, which points
    to option A, and still needs a term to hold them and, with no `E`, its end date. Put to the NSTA
    with L12's final check.

    **Rates outside the terms ruled out (20 November 2026).** Migrating these rates as they are,
    after the final term (as area events are shown, LMS1-501), was considered and rejected: LMS's
    own validation keeps a licence's rent within its terms, and stays as it is. The rate, work
    programme and other event forms refuse a date after the end of the schedule
    (`ScheduleRelativeDateValidationService.validateRelativeDateBeforeEndOfSchedule`), so such a rate
    couldn't be edited or followed by another; a term can't be shortened or removed to leave a rate
    after it; and for pattern B the expiry still wouldn't match the final term's end
    (`doesExpiryDateMatchEndOfFinalTerm`). So the term has to exist, made either way:

    - **In the migration (option A):** add the term after the last `T`. For pattern B it ends on `E`,
      a single rule from the data; for the licences with no `E` the regulators give its end.
    - **A PEARS data fix by the NSTA:** add the missing term (and the expiry, where there is none) to
      each licence's PEARS schedule before the production run. The migration then takes the
      schedules as they are, with no new rule, and PEARS and LMS agree, so the write-back has nothing
      to undo.

    They can be combined, for example A for pattern B and a PEARS fix for the licences with no `E`,
    where the NSTA has to decide the dates anyway. Any licence fixed by neither by the production run
    migrates as now, with the rates after its last term reported as not migrated.

## Out of scope

- The onshore retention area rate (`RETENTION_AREA_RATE`): unused in PEARS. Onshore schedules and
  retention/development/pre-producing areas are otherwise **in scope**.
- Licences on LMS-managed licence types (`managedByLms = true`): they have no PEARS schedule.
- Area **records and work programmes** (`ped_retention_areas`, `work_programme_xml`): later phase.
  Phase 1 migrates only the `RD`/`PA` schedule events as they are in the PEARS XML.
- Area **geometry**: already migrated by the gis-framework GIS migration (`Layer.RETENTION_AREAS`,
  keyed by the PEARS `si_id`).
- Migrating schedule acknowledgements (`ped_licence_acknowledgements`, Fox `PED028X`). The
  write-back must not corrupt them: see "PEARS write-back: regenerating the schedule XML".
- Template schedules (`ped_template_schedules`): LMS has no template concept. The write-back passes
  the licence's existing template through unchanged.
- **Building** the write-back. It is a separate piece of work; this document records the
  requirements the migration must meet for it.

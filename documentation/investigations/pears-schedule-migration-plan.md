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
  `DL`;
- the placeholder types `A`, `B`, `NA` and `XL`.

LMS-managed types (carbon storage, gas storage, landward and seaward exploration, methane drainage)
are excluded and have no PEARS schedules anyway. Every licence type that has a PEARS schedule maps
to a PEARS-managed type. `A`, `B`, `NA` and `XL` have no schedules (S17). On live (27 October 2026;
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
| `E` (optional) | (1) confirms the **end boundary of the final term** (the final `T` sets it), and (2) `LicenceScheduleExpiry` with `comments = COMMENTS` | **Decided:** the expiry event marks the end of the final term. On P2244 it shares its date with the final `T` (01-Dec-2040), and the final term's LMS end date is `E - 1 day` (30-Nov-2040). **`expiryDate`: pending (Open question 8)**: `E` (01-Dec-2040) or `E - 1 day` (30-Nov-2040), because LMS checks that the expiry date equals the final term end. **No `E` event:** migrated without an expiry record (see "Term boundaries" rule 3). An `E` whose date differs from the final `T`: migrated as it is (no extra term), and reported (see "Term boundaries" rule 3). |
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
  ignore it. This includes event reminders (`TermOrPhaseEndDeadlineService`), the extension and
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
  each version. Only P1161 on live (2 versions, the last end-dated 14/10/2011; licence surrendered
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
  period:
  - on the period's start date: `RateDefinitionOption.PHASE` / `TERM`;
  - after it: `CUSTOM_PERIOD` + `RELATIVE_TO_START_DATE` against that phase or term, with
    `relativeDuration = Period.between(periodStart, occurrenceDate)`.
- Later occurrences of the **same rate in the same period** do not create extra rows: the LMS rate
  already applies until the next rate. So a yearly rate spanning the initial term's phases and the
  second term becomes one LMS rate per phase/term it touches, each at the same `rentalRate`.

  **Pending decision (Open question 7):** one LMS rate per phase/term the yearly rate touches
  (current working assumption), or one LMS rate row per yearly redefinition. The arguments for
  each, given the PEARS write-back, are in "Decision: how yearly-recurring rates are held in LMS".

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

**5. Fixed-count recurrences.** A yearly `R` with a `RECURRANCES` count needs no extra handling
when the count runs up to the next rate: LMS's "in force until the next rate" means the same, and
LMS does not store the count. On live, 9 `PL` and 3 `AL` schedules use a count (data survey S12b):

- **`PL` (e.g. PL116): contiguous.** `.5` ×2 from 1969 covers 1969 to 1971 and `1` starts in
  1972. One LMS rate per PEARS rate, and the write-back emits the same counts.
- **`AL` (AL6, AL9, AL10): the count overlaps the next rate.** `25` ×6 from 1995 covers 1995 to
  2001 and `50` also starts in 2001, so PEARS may pay both in 2001. LMS holds one rate at a time,
  so `25` stops when `50` starts, and the write-back emits `25` ×5. The overlap is dropped. **The
  licensing team confirms the 2001 double payment was never intended** (open question 14). These
  are 1990s dates, so it only matters if the payments report is run for those years.

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
hand-entered pre-producing area events. They are copied as is with those columns empty. Their
timeline caption falls back to the comments, or "Pre-producing area". The write-back writes them
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

- A new timeline event type (e.g. `ScheduleEventType.AREA`, "Retention, development and
  pre-producing areas", filterable), looked up from `licence_schedule_area_events` for the detail
  being viewed. The caption is the area name and versioned short name (as PEARS shows,
  `licensing_retention_areas.get_versioned_short_name`), and the description is the comments.
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
- **Not written**: LMS work programme activities (no PEARS equivalent). LMS other events whose
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
`EXL`, `ML`, `AL`, `DL`, `CE`) and the placeholder types (`A`, `B`, `NA`, `XL`, which have no
PEARS schedules). It includes new licences raised in
PEARS whose first schedule is created in LMS. LMS-managed types (carbon storage, gas storage,
landward and seaward exploration, methane drainage) have no PEARS schedule, so LMS never publishes
the write-back for them, and the `RD`/`PA` round trip does not apply. LMS checks
`isManagedByLms()` before publishing (e.g. a `LicenceTypeRulesResolver`/feature rule
`hasPearsScheduleWriteback`), rather than relying on PEARS to reject the message.

**When LMS publishes:**

- **(a) A draft schedule is applied:** `ReviewAndApplyScheduleController.applyDraftSchedule`, once
  the new `ACTIVE` detail has committed.
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
   further schedule edits are made in PEARS.
2. Run the final migration and `RD`/`PA` refresh, and create the `ped_lms_schedule_sync` rows.
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
  e.g. column `NUMERIC(4,1)` and a validator limit of 1 dp.
- Migration: `relinquishmentPercentage = PERCENTAGE` (empty stays empty), after trimming and
  **stripping a trailing `%`** (live data has `50%`; data survey, S13). **Decided:** the placeholder
  **`x`** (303 current events on live, S13b) is not migrated. The relinquishment event itself is
  still migrated (date, description), with an empty percentage, and the `x` values are counted once
  in the report. Values that still do not
  parse as 0–100 with at most 1 dp are not migrated and are reported. Comments still become the
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
sees it (its stop condition in "Rates and yearly recurrence" is harmless), and the write-back does
not produce it. Recurrence is written back with `RECURRANCES` only.

### Decision: how yearly-recurring rates are held in LMS

**Status: open, to be decided with the licensing and data warehouse owners before the mapper is
built.** The rest of the plan assumes option A until then.

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

## Design

Follow the existing `migration/pears` package for reading PEARS and the `migration/carbonstorage`
package for building schedules.

### 1. Read PEARS: `PearsScheduleService` (`migration/pears/schedule`)

- `@Service @ConditionalOnPearsDataSource`, constructor-injected
  `@Qualifier("pearsDataSource") DataSource`, the same as `PearsLicenceService`. No new datasource
  configuration is needed: `pears.datasource.*` (read-only Hikari, `initialization-fail-timeout=-1`)
  already exists.
- SQL in `src/main/resources/pears-migration/`, loaded with the same `sql(name)` classpath helper:
  - `licence-schedules.sql`: one row per non-dummy event of **every** schedule version:
    ```sql
    SELECT plm.licence_type, plm.licence_no, se.plsd_id, se.start_datetime, se.end_datetime,
           se.origin_date, se.event_mnem, se.term_no, se.phase, se.years, se.months, se.days,
           se.event_date, se.recurring, se.recurrances, se.recur_end_date, se.rate,
           se.relinquishment_percentage,
           se.comments, se.event_subtype, se.event_id
    FROM pedmgr.xview_ped_lic_schedule_events se
    JOIN pedmgr.ped_licence_master plm ON plm.id = se.ped_lm_id
    WHERE NVL(se.dummy_event_flag, 'false') != 'true'
    AND (? IS NULL OR (plm.licence_type = ? AND plm.licence_no = ?))
    ORDER BY plm.licence_type, plm.licence_no, se.start_datetime, se.plsd_id, se.event_date
    ```
    A version whose schedule has no events still produces a detail, so versions are also read
    from `ped_licence_schedule_details` (`id`, `start_datetime`, `end_datetime`, `ORIGIN_DATE`)
    and joined to their events in Java.
    If the migration user has no grant on the xview, fall back to the same `XMLTABLE` over
    `ped_licence_schedule_details` (the xview's own `BASETABLE_STATEMENT`). Check grants early,
    as `licence-references.sql` notes for `PED_SIMULATION_TRANSACTIONS`.
  - `schedule-event-types.sql`: `SELECT mnem, short_title, display_seq FROM pedmgr.ped_template_event_types`.
- Return immutable records (`PearsSchedule(licenceReference, List<PearsScheduleVersion>)`, where
  `PearsScheduleVersion(plsdId, start, end, originDate, List<PearsScheduleEvent>)`),
  matching `PearsLicencePositions` / `PearsLicenceReference`.

### 2. Map: `PearsScheduleMapper`

A pure function from `PearsSchedule` + event type catalogue to a `MigratedSchedule` value
(unsaved entities + a list of `MigrationNotes`). Keeping it free of Spring and the database makes it
unit-testable from fixtures, as `PearsOperationMapper` is. It:

1. Builds terms, then phases, then rates and other events, in that order, so anchors exist.
2. Skips events with an empty `event_mnem` per "Events with no event type" (ignored and only
   counted, whether empty or holding data). Throws a mapping exception for an unknown non-empty `event_mnem`, a missing `ORIGIN_DATE`, or a `T`/`P`
   numbering it cannot place. These are collected per licence, not thrown out of the whole run.

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
  fully migrated with its history or not at all, and re-running picks up only what failed.
- Save one `pears_schedule_event_migration_links` row per migrated event (see section 6), in the
  same transaction.
- Throw `MigrationPreconditionException` if PEARS returns no schedules at all (for example the
  wrong datasource or a missing grant), so an empty run cannot look like success.
- Exposes `migrateLicence(licenceReference)`: migrates **one** licence in its own transaction and
  returns its outcome (`MIGRATED`, `SKIPPED` with reason, or `FAILED` with notes). The background
  worker (section 4) calls it per licence. The service does not loop over the estate itself.

### 4. Run in the background: queue, worker and controller

**Decided:** the migration runs as a **background job**, not inside the HTTP request. At about
1,799 licences, 10,834 versions and ~450k LMS rows (see "Migration size (live, 27 October 2026)"), a
request would hit the load balancer/proxy timeout, leave the outcome unclear and give no progress.
The existing LMS migrations (carbon storage, industry teams) run inside the request and LMS has no
`@Async`, so this is new. It reuses what LMS already has: ShedLock (`ShedlockConfiguration`) and
`@Scheduled` jobs (e.g. `LicenceScheduledJobService`).

**Tables (Flyway):**

```sql
CREATE TABLE lms.pears_schedule_migration_runs (
  id            UUID PRIMARY KEY,
  status        TEXT NOT NULL,          -- QUEUED, RUNNING, COMPLETE, CANCELLED
  requested_by  INTEGER NOT NULL,       -- wua_id
  requested_at  TIMESTAMPTZ NOT NULL,
  completed_at  TIMESTAMPTZ
);

CREATE TABLE lms.pears_schedule_migration_queue (
  run_id             UUID NOT NULL REFERENCES lms.pears_schedule_migration_runs (id),
  licence_reference  TEXT NOT NULL,
  status             TEXT NOT NULL,     -- PENDING, MIGRATED, SKIPPED, FAILED
  attempts           INTEGER NOT NULL DEFAULT 0,
  outcome            TEXT,              -- skip reason / failure summary
  notes              JSONB,             -- migration notes (rounded rates, defaults, untyped rows ...)
  started_at         TIMESTAMPTZ,
  finished_at        TIMESTAMPTZ,
  PRIMARY KEY (run_id, licence_reference)
);
```

**Controller** (`@Profile("migration")`, `/migration/pears-schedules`, GET + POST as on the other
migration controllers):

| Endpoint | Does |
|---|---|
| `POST /migration/pears-schedules` (optional `licenceReference`) | Reads the in-scope licence list from PEARS, creates a run and one `PENDING` queue row per licence (or just the one licence), returns **202 Accepted** with the run id. Returns 409 (`MigrationPreconditionException`) if PEARS returns no licences, or if a run is already `RUNNING`. |
| `GET /migration/pears-schedules/{runId}` | Run status: counts by status, started/finished, and the failed and skipped licences with reasons. |
| `POST /migration/pears-schedules/{runId}/retry-failed` | Sets `FAILED` rows back to `PENDING` (e.g. after the licensing team fixes data in PEARS). |
| `POST /migration/pears-schedules/{runId}/cancel` | Marks the run `CANCELLED`. The worker stops claiming its rows, and a licence mid-flight finishes. |

**Worker** (`PearsScheduleMigrationJob`, `@Profile("migration")`):

- `@Scheduled(fixedDelayString = "${pears.schedule-migration.poll:PT5S}")` with
  `@SchedulerLock(name = "pearsScheduleMigration", …)`, so only one LMS instance works a run at a
  time.
- Each pass claims a small batch of `PENDING` rows from the oldest `RUNNING`/`QUEUED` run
  (`pears.schedule-migration.batch-size`, start at 10; tune after the UAT rehearsal). For each
  licence it calls `PearsScheduleMigrationService.migrateLicence` (one transaction per licence),
  then records the outcome, notes, attempts and timings in a separate short transaction.
- When no `PENDING` rows remain, the run becomes `COMPLETE`.
- **Restarts and deployments:** a licence in flight when an instance stops is rolled back and stays
  `PENDING`, so the next pass carries on. Re-running is safe anyway, because licences with a schedule
  are skipped.
- **PEARS load:** one licence at a time on a 3-connection read-only pool
  (`pears.datasource.hikari.maximum-pool-size=3`) keeps the load on PEARS predictable.

**Operational notes:**

- Time the largest licence (66 schedule versions as of 27 October 2026) in the UAT rehearsal. It is the biggest single
  transaction.
- Run the **current-only** rehearsal first (~1,744 versions, ~35k events), then the full history.
- **Go-live:** the final run must be `COMPLETE`, with failures reviewed or accepted, before the
  `RD`/`PA` EPMQ round trip and the write-back are switched on (see "Write-back transport").
- The comparison report (section 6) and the validation test (section 5) are unchanged. The
  validation test stays a separate Gradle task.

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
```

By default it compares the current PEARS version with the `ACTIVE` LMS detail. With
`versions=all` it pairs each PEARS version with its LMS detail (through `pears_plsd_id` in the link
table) and prints one table per version, each headed with its PEARS `start_datetime` and LMS
status. It flags any version missing on either side, and any event whose `original_event_id`
differs from the event it was matched to in the previous version.

**Reads.**
- PEARS: the same `PearsScheduleService.licenceSchedules(...)` the migration uses, so the report
  shows exactly what the migration read.
- LMS: the licence's `ACTIVE` `LicenceScheduleDetail`, with its start date, terms, phases, rates,
  expiry and other schedule events, read through the existing services (no recalculation; it shows
  the stored dates).

**Pairing PEARS events with LMS events.** The migration records where each LMS event came from in a
small link table (Flyway), so the report pairs rows exactly instead of guessing:

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

**Output: one table per licence.**

Header: licence reference, PEARS `ORIGIN_DATE` vs LMS start date, PEARS schedule version
(`plsd_id`, `start_datetime`), LMS detail id and status.

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
- **Result** is one of `Match`, `Differs by ±n days`, `Missing in LMS`, `LMS only`, `Not migrated`
  (unmapped events), or `No start date in PEARS`. The last applies to every event in a historical
  version without an `ORIGIN_DATE`, whose PEARS dates are `1900`-based and are not compared. `PA`/`RD` events are compared too: each PEARS `RD`/`PA` event against the LMS
  area event it was migrated from (area name, short name, version, subtype, date, comments).
- Rows are ordered by PEARS date (LMS date for `LMS only`). Term and phase rows come first within
  equal dates, because a term mismatch explains the mismatches anchored to it.
- A summary line per licence and for the whole request: counts per result.

**Reuse in the validation test.** `PearsScheduleMigrationValidationTest` builds the same comparison
model (`PearsScheduleComparisonService.compare(licenceReference)`) and fails nothing. It writes the
same rows into `MigrationValidationReport`, so the full-estate sweep and the per-licence report
cannot disagree.

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
   fixes (L2) and the items still to confirm or decide (L12, L14). Those include Open questions
   7 (how yearly-recurring rates are held in LMS) and 8
   (expiry date convention), which also need the data warehouse owners. The term and phase
   patterns are already decided (L5–L11).
3. Add `BESPOKE_MILESTONE` to `OtherScheduleEventCategory`, with its form/timeline tests, as its
   own change ahead of the migration.
3a. Add `TermType.FOURTH`, offered only when the licence's active schedule version already has a
    Fourth term, and check every `TermType` consumer handles it (see "Decision: four-term
    licences"). Its own change, ahead of the migration.
3b. Add the mandatory relinquishment percentage field to LMS (see "Decision: mandatory
    relinquishment percentage"), with form, validation (0–100, up to 1 dp), timeline display and
    tests. Its own change, ahead of the migration.
3c. **Done (28 October 2026):** `AL`, `CE` and `DL` map to `LANDWARD_PRODUCTION`, so they have the
    Initial, Second and Third term types (Open question 14). All 3 `AL` schedules fit them (data
    survey S8b).
4. `PearsScheduleService` + SQL resources + grant check.
5. `PearsScheduleMapper` + unit tests from fixture XML (include licence P2244's two BM events, and a
   multi-version licence to test `original_event_id` matching across versions, yearly rates
   that cross phase and term boundaries, and one licence per term pattern A–G from the data survey). Unit test the Java port of PEARS date arithmetic
   (`ADD_MONTHS` included) against values computed in Oracle.
6. `PearsScheduleMigrationService` + integration test (Testcontainers Postgres, mapper input stubbed).
   In the same step: `licence_schedule_area_events` (Flyway), with `RD`/`PA` events copied as is from
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
8. Link table migration, `PearsScheduleComparisonService` + `PearsScheduleComparisonController`
   (HTML + CSV) + tests. Build this **before** the first UAT run, so the licensing team can review
   chosen licences from the first run onwards.
9. `PearsScheduleMigrationValidationTest` against UAT PEARS, built on the comparison service. Iterate until differences are only
   known, accepted cases.
10. Rehearse on a copy of production data, then run in production with the `migration` profile.

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
   recurrence"). How many LMS rows a yearly rate becomes is still open: see Open question 7.
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
7. **Yearly-recurring rates in LMS.** Open, a decision to be made with the licensing and data
   warehouse owners. Option A: one LMS rate per phase/term, with the PEARS write-back rebuilding
   recurrence. Option B: one LMS rate row per year, with a one-to-one write-back. The arguments for
   both are in "Decision: how yearly-recurring rates are held in LMS". The plan assumes option A
   until decided.
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
   on LMS rates isn't needed for gaps. The licensing team still has to say which case applies.
   **Data warehouse (option A):** the write-back would put `RECURRANCES` counts on `P` and `PEDL`
   schedules, which have never had them. The data warehouse owners confirm their feed handles
   that, alongside the 0-rate rows of gap case (b).
8. **Expiry date convention.** Open. PEARS dates the expiry on the anniversary the licence ends
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
    starts, so PEARS may pay both in that year (AL10: 2001). LMS and the write-back drop the
    overlap ("Rates and yearly recurrence" step 5). The licensing team confirms the double payment
    was never intended.

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

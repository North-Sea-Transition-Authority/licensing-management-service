# Investigation: Migrating PEARS Licence Work Programmes into LMS

> **Status: First pass (28 October 2026), survey complete (29 October 2026, follow-ups WP-S1e to WP-S1g and WP-S4d on 3 November
> 2026; see the survey's Summary), design and delivery steps drafted (30 October 2026), revised
> against the built schedule migration (23 November 2026).** Nothing in this plan is implemented.
> The schedule migration it builds on **is built and on `develop`** (LMS1-787, merged 20 November
> 2026, package `migration/pears/schedule`); see "What the schedule migration provides". Decided so
> far: `DRAFT` programmes are excluded, the review audit is not migrated, migrated comments carry
> the PEARS author's name in the text with no WUA id, the work programme is **not written back to
> PEARS**, and **activity descriptions migrate now, into the LMS activity's short description, while
> legal text is migrated separately, later, into one central legal text area LMS builds first**
> (D14, "Follow-up: once the legal text area is built"). Decided with the licensing team (25
> November 2026): term- and phase-level dating is acceptable (Open question 11), the well-count
> matrix is not migrated (Open question 14), and every work programme reminder in its window goes
> out at go-live (Open question 15). This compares
> how PEARS holds a **licence work programme** with the LMS `WorkProgrammeActivity` model, and lists
> the differences a migration would have to resolve. It follows the format of
> [pears-schedule-migration-plan.md](pears-schedule-migration-plan.md), which lists LMS work
> programme activities as out of scope and is the base this builds on. The live data survey is
> complete: the queries and results are in
> the data survey (`pears-work-programme-migration-data-survey.md`), a migration document provided separately.
> The questions for the licensing team (survey WP-L1 to WP-L13) are in the licensing team review
> document, with the per-licence lists in the licensing items spreadsheet. Both are migration
> documents, provided separately.
>
> This covers the **licence** work programme only (`pedmgr.ped_work_programme` /
> `ped_work_programme_details`, Fox `PED009X`). It does not cover the retention/development area work
> programme (`ped_retention_areas.work_programme_xml`, `ped_work_programme_store`, `PED031L`/`PED033X`),
> which the schedule plan defers to a later phase, or the application work programme in
> `ped_licence_app_details` (LARRY).

## Problem

PEARS holds each licence's committed work (wells, seismic, other activities) in a versioned XML
document, maintained in Fox `PED009X`. LMS holds the same kind of information as
`WorkProgrammeActivity` schedule events. The schedule migration (built, on `develop`) does not
migrate any of this. Once the schedule is migrated, the LMS schedule for a PEARS licence has every
PEARS version, with terms, phases, rates, expiry, other events and area events, but no work
programme.

The two models differ structurally, not just in naming:

- **Container:** PEARS has a separate document per programme, with up to three per licence
  (INITIAL, FALLOW, EXTENSION) and its own version history. LMS has activities inside each
  `LicenceScheduleDetail` version.
- **Dates:** PEARS activities have **no dates**. At most they sit inside a phase. Every LMS activity
  needs a date option anchored to a term, a phase or another event.
- **Category and commitment:** PEARS packs both into one `ACTIVITY_TYPE` (`FIRM_WELL`,
  `SHOOT_2D_SEISMIC_CONTINGENT`). LMS holds them as two mandatory fields.
- **Statuses:** the two status lists only partly overlap.
- **Programme-level data:** PEARS holds data at programme level (legal text, breakpoint, well-count
  matrix, programme status, review audit). LMS has nowhere to put it, and some of it feeds the
  licence documents.

### Scope: programme statuses

- **Decided (29 October 2026): `DRAFT` programmes are not migrated.** On live these are only the 8
  unnumbered awards that haven't been issued for raise (WP-S1), so there is no LMS licence to put
  them on anyway.
- **`DRAFT_FINAL`: for the licensing team to decide (Open question 9).** Despite the name, this is the **final, agreed**
  award work programme, not a draft. PEARS sets it in two places:
  - when the award is issued for licence raise (`PED9010X` `api-set-work-programme-status`, an
    in-place update with no new version);
  - for every legacy award loaded by `IMPORT_APPLICATION_AWARDS.prc`.

  During raise, `PED017X` only allows the legal text to be edited or the programme rolled back to
  the awards screen while it is `DRAFT_FINAL` and no offer document exists. After the licence is
  raised, the status changes only when someone saves the programme in `PED009X` `maintain`
  (`action-update-wp-status` re-derives `NOT_STARTED` / `IN_PROGRESS` / `COMPLETE` from the
  activities).

  But `action-update-wp-status` only sets a status when every activity is `NOT_STARTED`, every
  one is `COMPLETE`/`FULL_WAIVER`, or something is under way and not all done. Activities that are
  only `NOT_STARTED`, `DROPPED` and `WAIVER_DECLINED`, with at least one of the last two, leave it
  at `DRAFT_FINAL` even after a maintain save. WP-S1e (live, 3 November 2026) lists 90 numbered
  licences (75 `P`, 15 `PEDL`) with one:
  - 36 raised and **maintained** (a dropped activity with its note), all surrendered or expired;
  - 31 raised and **never maintained** (all `NOT_STARTED`): 12 extant, 19 ended;
  - 23 awards not yet raised (WP-S10b, Open question 9).

  The programme status isn't migrated (D11), so technically all of these can migrate like any
  other. Whether they *should* is for the licensing team. The 5 `DRAFT_FINAL` programmes with **no
  licence type** are issued-for-raise awards whose licence isn't numbered yet: no LMS licence, so not
  migrated, the same as `DRAFT`.

## Source: how PEARS holds a licence work programme

### Tables

There is no `CREATE TABLE` for either table in the edu repo. The columns below are inferred from
the DML in `pedmgr.work_programme`.

| Object | Purpose |
|---|---|
| `pedmgr.ped_work_programme` | Header: `id`, `ped_lm_id` (→ `ped_licence_master.id`). Several per licence, one per programme type, shown as tabs in `PED009X`. Not linked to a licence position or a schedule version. |
| `pedmgr.ped_work_programme_details` | One row per **save**: `id`, `ped_wp_id`, `start_datetime`, `end_datetime`, `status_control` (`'C'` = current), `xml_data`, `created_by`, `created_datetime`, `record_status` (`ACTIVE`/`DELETED`), `deleted_by_wua`, `deleted_datetime`. |
| `xview_work_programme_current` | Current row per programme: id, title, type, status, legal text, `ped_lm_id`. |
| `xview_ped_work_prog_well_count` | Pivots the well-count matrix by status × firm/contingent/D&D. It leaves out `DROPPED`. |
| `VG_PWPD_*` (Viewify MVs) | Flattened copies of the XML, in refresh group `ped_refresh`. |

**Versioning** (`WORK_PROGRAMME.pkb`):
- `update_work_programme` end-dates the current row and inserts a new `'C'` row with the whole XML.
  This is the same pattern as `ped_licence_schedule_details`.
- `set_work_programme_status` updates the `'C'` row **in place** (`UPDATEXML`), without a new version.
- `delete_work_programme` is a soft delete: it sets `record_status = 'DELETED'` but **leaves
  `status_control = 'C'`**. Readers must also filter on `record_status`. `PED009X` does;
  `TEMPLATE_SCHEDULE`, `LICENCE_CREATE` and the raise reports do not.
- `ped_work_programme_act_seq` allocates `ACTIVITY_ID`, which is **stable across versions**.

### Work programme XML (`ped_work_programme_details.xml_data`)

```
WORK_PROGRAMME
  WORK_PROGRAMME_TYPE        -- INITIAL | FALLOW | EXTENSION
  TITLE                      -- max 100
  WORK_PROGRAMME_STATUS      -- DRAFT | DRAFT_FINAL | NOT_STARTED | IN_PROGRESS | COMPLETE
  BREAKPOINT?                -- drill-or-drop decision point
    YEARS, MONTHS, DAYS      -- offset from licence start
    CHANGED_DATETIME, CHANGED_BY_WUA
  WELL_COUNT_LIST/WELL_COUNT ×5
    WELL_STATUS              -- OBLIGATED | COMPLETED | DROPPED | WAIVED | WAIVER_DECLINED
    FIRM_COUNT, CONTINGENT_COUNT, DRILL_OR_DROP_COUNT   -- 0-999
  ACTIVITY_LIST/ACTIVITY*    -- "general organisation" rounds
    ACTIVITY_ID, ACTIVITY_TYPE, TITLE (max 200), ACTIVITY_STATUS, COMPETED_BLOCKS, DESCRIPTION
  PHASE_LIST/PHASE*          -- "phase organisation" rounds
    TYPE                     -- 'Phase A' | 'Phase B' | 'Phase C'
    TIMINGS                  -- whole years, 1-4; total across phases <= 9
    ACTIVITY_LIST/ACTIVITY*  -- as above
    SELECTED_FLAG            -- UI state, persisted
  TERM_LIST/TERM*
    ID (1/2/3), DESC, TIMINGS   -- 1 = sum of phases, 2 = 1-9 years, 3 = 18
  LEGAL_TEXT                 -- HTML, merged into the licence document
  NOTES_LIST/NOTE*
    DESCRIPTION, TYPE (GENERAL or the new status), ACTIVITY_ID, ACTIVITY_TITLE,
    ACTIVITY_DESCRIPTION, NEW_ACTIVITY_STATUS, DATETIME, CREATED_BY (full name)
  REVIEW_AUDIT_LIST/REVIEW_AUDIT*
    ACKNOWLEDGED_FLAG, ACKNOWLEDGED_DATETIME, ACKNOWLEDGED_BY_WUA, BOTTOM_ACKNOWLEDGED_FLAG
```

The round config decides which list a licence uses (`PED_ROUND_CONFIG.xsd`,
`AWARDS/WORK_PROGRAMME/USE_PHASE_ORGANISATION` vs `USE_GENERAL_ORGANISATION`):
- **Phase organisation:** offshore V2, V4 and V5, and restricted offshore V2 and V4.
- **Flat list:** offshore V1 and V3, onshore V1 and V2, and the other restricted versions.

### Activity types

Set by the "add activity" actions in `PED009X`:

| List | `ACTIVITY_TYPE` values |
|---|---|
| Flat | `FIRM_WELL`, `CONTINGENT_WELL`, `DRILL_OR_DROP_WELL`, `SEISMIC` (one type for every shoot/obtain/reprocess 2D/3D item; only `TITLE` tells them apart), `LEGACY`, `OTHER` |
| Phase | the three well types, `SHOOT_2D_SEISMIC`, `SHOOT_2D_SEISMIC_FIRM`, `SHOOT_2D_SEISMIC_CONTINGENT`, `SHOOT_3D_SEISMIC`, `SHOOT_3D_SEISMIC_FIRM`, `SHOOT_3D_SEISMIC_CONTINGENT`, `OBTAIN_2D_SEISMIC`, `OBTAIN_3D_SEISMIC`, `REPROCESS_2D_SEISMIC`, `REPROCESS_3D_SEISMIC`, `OTHER` |
| Bulk import (`IMPORT_APPLICATION_AWARDS.prc`) | `FIRM_WELL`, `CONTINGENT_WELL`, `DRILL_OR_DROP_WELL`, `SEISMIC`, `OTHER` |

Quantities (well depth, km, km²) are free text in `DESCRIPTION` only, from templates like
"One Firm well to \*\*\*m into the \*\*\*". `TITLE` defaults to "<type> <n>", e.g. "Firm Well 1".

### Activity statuses

The map-set depends on the type (`PED009X`):

| Types | `ACTIVITY_STATUS` values |
|---|---|
| Default | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETE`, `PART_WAIVER`, `FULL_WAIVER`, `WAIVER_DECLINED` |
| `DRILL_OR_DROP_WELL` | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETE`, `DROPPED` |
| `LEGACY` | default + `DROPPED` |

Every status change writes a `NOTE` with `NEW_ACTIVITY_STATUS` and `DATETIME`, so the status
history can be rebuilt from the notes.

### Lifecycle

1. **Award** (`PED9010X` → `PED009X` theme `create`): the user creates an INITIAL programme and
   can import items from the application (LARRY). Each application text field becomes one activity,
   and its free text is copied into `DESCRIPTION`. Phase timings are entered by hand.
   Status starts at `DRAFT`, then moves to `NOT_STARTED` or `DRAFT_FINAL`.
2. **Raise** (`PED017X` → theme `legal-text`): "Populate legal text" builds `LEGAL_TEXT` HTML from
   the activities, grouped by phase. `TEMPLATE_SCHEDULE.generate_schedule` reads the
   **phase timings** to build the template schedule (P, R and T events). Activities never become
   schedule events.
3. **Maintain** (PEDLM uref → theme `maintain`, privilege `PED_LIC_WORK_PROG_EDIT`): users change
   statuses, add notes, edit the well counts, set the breakpoint, add or soft-delete programmes, and
   acknowledge reviews.

## Target: the LMS work programme model

- `WorkProgrammeActivity` (`work_programme_activities`, a `ScheduleEvent` subclass, Envers-audited),
  one set per `LicenceScheduleDetail`:
  - `category`: `WorkProgrammeActivityCategory`. Production types (`SEAWARD_PRODUCTION`,
    `LANDWARD_PRODUCTION`) get `DRILL_WELL`, `DRILL_OR_DROP_WELL`, `NEW_SHOOT_2_D_SEISMIC_DATA`,
    `NEW_SHOOT_3_D_SEISMIC_DATA`, `OBTAIN_EXISTING_2_D_SEISMIC_DATA`,
    `OBTAIN_EXISTING_3_D_SEISMIC_DATA`, `REPROCESS_SEISMIC_DATA`, `WELL_INVESTMENT_ENGAGEMENT`,
    `WELL_TEST` and `OTHER_ACTIVITY`, which needs `otherCategoryName`.
  - `description`: **required**.
  - `commitment`: `FIRM` | `CONTINGENT` | `CONDITIONAL`, **required**.
  - `dateOption`: `WITHIN_A_TERM` (term) | `WITHIN_A_PHASE` (phase) | `RELATIVE_DATE` (another
    schedule event + `relativeDuration`), **required**. `dueDate` is calculated by
    `LicenceScheduleCalculationService`.
  - `originalEventId`: identity across schedule versions.
- `WorkProgrammeActivityStatus` (`work_programme_activity_statuses`): a status history per event,
  with `status` = `OPEN` | `IN_PROGRESS` | `COMPLETE` | `FULL_WAIVER` | `TRANSFERRED`,
  `appliedDatetime` and `licenceTransferredTo`.
- `EventComment`: comments on the activity.
- **Schedule comments (prerequisite A):** a general comments area above the main schedule, for
  comments that belong to the schedule as a whole rather than to one event. Designed in the LMS
  mockup `mockups/schedule/LicenceScheduleMockupController`. As built: `ScheduleComment`
  (`schedule_comments`), keyed on `licence_schedules`, not a version, with text, timestamp and an
  optional `authorWuaId`.
  - Shown to regulators only, on the licence's schedule tab and (read-only) on the draft timeline:
    author, date/time and text.
  - Schedule and work programme administrators can remove one from the schedule tab at any time.
  - There is no screen to add one; the migration writes them straight to `ScheduleCommentRepository`.
  - The mockup's optional **linked licence** ("View licence …") is not built yet. One mockup
    example is exactly a PEARS transfer case: "Alternative work programme for drill one
    exploration well … to be completed under licence P2500".
- Changes after licensing come through the schedule/work programme application journeys (amend
  work programme, record of decision), not through direct edits.
- **Precedent:** the carbon storage migration
  (`CarbonStorageLicenceMigrationService.buildWorkProgrammeActivity`) already builds activities,
  statuses and comments from an extract. It maps category and commitment by display name, and the
  term and date option from extract columns.

## Key structural differences

| # | Area | PEARS | LMS | Impact |
|---|---|---|---|---|
| D1 | Container and versioning | Its own document per programme (INITIAL/FALLOW/EXTENSION), versioned separately from the schedule | Activities belong to a `LicenceScheduleDetail` version | The two version histories have to be aligned (see "Version alignment"). |
| D2 | Several programmes per licence | Up to 3 typed programmes, each with a title and a status. Live also has **two current INITIAL programmes** on 6 licences: EXL253, PEDL120, PEDL133, PEDL180, PEDL181, PEDL225, all titled "Initial Work Programme" (WP-S1b). In each pair one is hand-made (2010–11) and the other holds a single `LEGACY` activity from a bulk job on 12/07/2011. Both are still saved, and they describe the **same commitment** but disagree on figures or statuses (WP-S1c, WP-S1d) | No programme grouping; one flat activity list per schedule version | The type and title would be lost, or need a new field. Merging the duplicates would count the commitment twice. **Proposed:** migrate neither programme on those 6 licences and report them, until the licensing team says which is authoritative (Open question 10). |
| D3 | Activity dates | None. At most the enclosing phase (phase organisation); nothing for flat lists | `dateOption` required; `dueDate` calculated | Every flat-list activity needs a default anchor (see M3). |
| D4 | Category vs commitment | Combined in `ACTIVITY_TYPE`; commitment only for wells and shoot seismic | Separate fields, both required | Category/commitment mapping table, plus a default commitment for types that have none. |
| D5 | Seismic granularity | Flat `SEISMIC` covers every seismic kind (only `TITLE` differs); 2D/3D reprocess are separate types | Shoot/obtain are split 2D/3D; `REPROCESS_SEISMIC_DATA` is a single type | Flat `SEISMIC` has to be classified from `TITLE`; reprocess loses 2D/3D (keep it in the description). |
| D6 | Title | `TITLE` and `DESCRIPTION` | `description` only (the category is the caption) | Merge the title into the description, or drop it. |
| D7 | `COMPETED_BLOCKS` | Free text per activity (flat list) | No field | Append to the description, or drop. |
| D8 | Activity statuses | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETE`, `PART_WAIVER`, `FULL_WAIVER`, `WAIVER_DECLINED`, `DROPPED` | `OPEN`, `IN_PROGRESS`, `COMPLETE`, `FULL_WAIVER`, `TRANSFERRED` | `PART_WAIVER`, `WAIVER_DECLINED` and `DROPPED` have no LMS value (see M5). `DROPPED` alone is 466 activities, 10% (WP-S3). |
| D9 | Status history | `NOTES_LIST` entries carrying `NEW_ACTIVITY_STATUS` + `DATETIME` | `WorkProgrammeActivityStatus` rows with `appliedDatetime` | Good fit: one status row per status note. |
| D10 | Notes | `NOTE` per activity or `GENERAL` (programme-level). Activity notes often point to a general one ("See General Note below", WP-S3b) | `EventComment` per event, plus **schedule comments** (prerequisite A) | Activity notes → event comments; general notes → schedule comments (M6). |
| D11 | Programme status | `DRAFT` … `COMPLETE`, derived on save | None | Not migrated; LMS can derive it from the activities if needed. Not reliable on phase-organised programmes: a maintain save always sets `COMPLETE`, because the rule only counts flat activities (survey WP-S1f). |
| D12 | Well-count matrix | 5 × 3 counts (obligated/completed/dropped/waived/waiver declined × firm/contingent/D&D), maintained **by hand**, separate from the activities | None | No home. **WP-S8:** where well activities exist, the matrix agrees with them on all but about 30 programmes, so it adds nothing there. But **872 programmes (844 `P`, 28 `PEDL`) count wells with no well activity at all**, almost certainly because the wells are inside a `LEGACY` summary. For those, the matrix is the only structured record of the well obligations and their outcomes. **Decided (25 November 2026): not migrated** (Open question 14, option C); the `LEGACY` summary text still carries the obligations. |
| D13 | Breakpoint | Y/M/D from licence start; feeds the documents' `DRILL_DROP_PERIOD`. On only 233 programmes, all `P`/`PEDL` INITIAL, and nearly all whole years (WP-S6) | None (a D&D well could have a `RELATIVE_DATE`) | Due date for D&D wells on 228 programmes (M3 rule 2). The 5 with no D&D activity go to a schedule comment. |
| D14 | Legal text | HTML merged into the licence documents (`LICENCE_CREATE` mail-merge) | None yet: **one central legal text area** for all the work programme's activities, to be built later | **Decided (23 November 2026): not part of this migration.** The activity descriptions migrate now, into the LMS activity's short description (M2). LMS builds legal text later, and PEARS legal text is then migrated **separately** into it. WP-S13 (live): 926 programmes on numbered licences hold legal text (838 `P`, 88 `PEDL`), so it can't be dropped, and PEARS keeps it (frozen, readable) until that migration. PEARS holds one HTML block per programme: generated from the activity descriptions ("Populate legal text"), then freely edited, with no link back to activities. **Current plan (25 November 2026): one central legal text area in LMS, not legal text per activity**, so the block migrates whole and is never split by matching text (Open question 5). This plan is updated once the area is built ("Follow-up: once the legal text area is built"). |
| D15 | Phase and term timings | Held in the work programme **and** in the schedule | The schedule only | Don't migrate them as data. **WP-S7:** 450 of 591 work programme phases (76%) end where the schedule's phase does. The other 140 were extended on the schedule only, so the work programme `TIMINGS` are stale award-time values. No work programme phase is missing from its schedule. |
| D16 | Review audit | `REVIEW_AUDIT_LIST` acknowledgements | None | **Decided: not migrated.** It's a copy of advisor sign-offs from the generic PEARS review process (`REVIEW_LIB`, `bpmmgr.review_advisor_items`): date, user and flag only, nothing specific to the work programme. WP-S13 (live): 5,383 entries on 1,590 programmes, the latest acknowledged 29 October 2026. |
| D17 | Activity identity | `ACTIVITY_ID`, stable across versions | `originalEventId` | Easier than the schedule migration: match on `ACTIVITY_ID`. WP-S12 (live): always set, unique within a version and across programmes, and its type never changes (43,388 activity rows). |
| D18 | Transfer | No status, but obligations **are** transferred: the notes record at least 9 (WP-S3b), as free text naming the target licence | `TRANSFERRED` + `licenceTransferredTo` | Candidates for `TRANSFERRED`, but the target licence must be supplied by hand (Q12). |
| D19 | Soft delete | `record_status = 'DELETED'` with `status_control = 'C'` still set | Detail status `DELETED` | Exclude deleted programmes. WP-S11 (live): none exist today; every programme has exactly one current row. |
| D20 | Comment author | Note `CREATED_BY` is a full name only. Each version's `created_by` is the saver's WUA id | `EventComment.authorWuaId`; with none, the author shows as "Unknown" | **Decided:** the author's name goes in the comment text and `authorWuaId` is left empty, for event comments and schedule comments. **LMS change needed:** a comment with no author currently shows "Unknown" (`EventCommentService.createViewFrom`). It should show a blank author instead, to avoid confusion. |
| D21 | Edit history | Every save is a full version with its author (`created_by`, `created_datetime`), back to 2010 | Envers audit from migration onwards; `LicenceScheduleDetail` has `createdInstant` only | Under Version alignment A only the current version migrates, so earlier descriptions, titles and who changed them are lost when PEARS is decommissioned (some time after LMS goes live; its history isn't kept). Status history and notes still carry over (M5, M6). To keep history, option B matches by time to the schedule versions; WP-S2c measures how much it keeps. |
| D22 | Data LMS has and PEARS doesn't | — | `CONDITIONAL` commitment, `RELATIVE_DATE`, `WELL_TEST` and `WELL_INVESTMENT_ENGAGEMENT` categories, `licenceTransferredTo` | Nothing to migrate into them, apart from D&D wells (`RELATIVE_DATE`, M3) and confirmed transfers (D18). They are filled going forward. |

## Draft mapping

### M1. Category and commitment

**WP-S3 (live, 29 October 2026), 4,787 activities:**
- Every type in live data has a row below.
- More than half (2,727) land in `OTHER_ACTIVITY`: `LEGACY` 1,782, `OTHER` 945.
- Only 246 (5%) state a commitment. 890 are D&D wells, and 3,651 (76%) have no commitment at all,
  so the default commitment (Q3) decides most of the migration.

| PEARS `ACTIVITY_TYPE` | LMS `category` | LMS `commitment` | Notes |
|---|---|---|---|
| `FIRM_WELL` | `DRILL_WELL` | `FIRM` | |
| `CONTINGENT_WELL` | `DRILL_WELL` | `CONTINGENT` | |
| `DRILL_OR_DROP_WELL` | `DRILL_OR_DROP_WELL` | **?** | PEARS has no commitment. `CONDITIONAL` fits the meaning; confirm (Q3). |
| `SHOOT_2D_SEISMIC_FIRM` / `_CONTINGENT` | `NEW_SHOOT_2_D_SEISMIC_DATA` | `FIRM` / `CONTINGENT` | |
| `SHOOT_3D_SEISMIC_FIRM` / `_CONTINGENT` | `NEW_SHOOT_3_D_SEISMIC_DATA` | `FIRM` / `CONTINGENT` | |
| `OBTAIN_2D_SEISMIC` / `OBTAIN_3D_SEISMIC` | `OBTAIN_EXISTING_2_D` / `_3_D_SEISMIC_DATA` | **default?** | |
| `REPROCESS_2D_SEISMIC` / `REPROCESS_3D_SEISMIC` | `REPROCESS_SEISMIC_DATA` | **default?** | 2D/3D kept in the description. |
| `SEISMIC` (flat list) | **By title** when it is a standard `PED009X` title ("Shoot 3D Seismic Data n" etc.): 518 of 663. **By description keywords** for the 145 generic "Seismic Data n" (WP-S4). **Tested (WP-S4b):** 144 of 145 classify. | **default?** | The one left is a seismic inversion → `OTHER_ACTIVITY` "Seismic inversion". 82 (12%) describe two operations (WP-S4d, 3 November 2026): 72 obtain and reprocess the same data, 8 only obtain data already reprocessed, and 2 (P1794, P2329) cover different data sets. The licensing team reviews them before migration (WP-L13): which to split, and which category the 72 get (today a standard title gives Obtain and a generic one Reprocess). Until answered: one activity under the title's category, or "reprocess" for generic titles. One title has a pasted Fox `javascript:ss('action-add-new-3d-seismic',…)` link; strip it and report it. |
| `LEGACY` | `OTHER_ACTIVITY`, `otherCategoryName` = "Legacy work programme" | **default?** | Created in bulk on 12/07/2011, one per pre-existing licence (WP-S1c): 1,782 activities, 37% (WP-S3). Onshore `LEGACY` activities have **no `TITLE`** (WP-S1d); on `P` they do, e.g. "Legacy Obligations: Original blocks 20/3f. Competed 1" (WP-S3b). The description is a **shorthand summary of the whole commitment**: onshore e.g. "DorD 3 yrs 1500m 30km new 2D O 500km 2D R 70km 2D", offshore e.g. "(T) D/D to 3505m to the shallower of Ettrick or Buzzard sst.". It spans several LMS categories and is too loose to split by rule, so it migrates as one activity, description as is. Where a title exists, prefix it to the description (M2). |
| `OTHER` | `OTHER_ACTIVITY`, `otherCategoryName` = a **fixed name**, e.g. "Other work programme activity" | **default?** | Not `TITLE`: 939 of 945 (99%) keep the default "Other Activity n" (WP-S4). The 6 hand-typed titles ("Geological Studies", "Rock Physics Study", "Seismic Inversion" and 3 others) could be used as the name instead. One ("Other Activity 3 - CONTINGENT COMMITMENT") is the only `OTHER` that states a commitment. |
| Anything else | — | — | Fail the run, as the schedule migration does for unknown event types. |

### M2. Description

**Decided (23 November 2026, confirmed 25 November 2026): the PEARS description migrates into the
LMS activity's short description** (`WorkProgrammeActivity.description`, the only text field the
LMS activity has today), as below. It is the only text carried for an activity; legal text is not
held per activity but in one central area, migrated later in a piece of work of its own (D14).

**"None" activities are not migrated (proposed, WP-S4b).** The application import turns every
application field into an activity, so fields the applicant left as "None" still became
activities, e.g. "None. We believe we already have all relevant data" or "None \*\* see other
work". They record no obligation. Skip any activity whose description, trimmed, starts with the
word "None", and list each one in the report. **WP-S5:** only 8 on live (4 seismic, 1 each
contingent, D&D, firm, `LEGACY`), leaving 4,779 activities.

`description` = `DESCRIPTION`, with the defaults left by the PEARS templates (`***`) kept as they
are and reported. Proposal: prefix `TITLE` when it isn't the auto-generated "<type> <n>", and append
`COMPETED_BLOCKS` when present. Default to `TITLE` when `DESCRIPTION` is empty, and to the category
name when both are empty, since LMS requires a description.

**WP-S5 (live, 29 October 2026):**
- **13 empty descriptions**, all `LEGACY`.
- **1 leftover `***`** placeholder (a D&D well).
- **9 competed blocks**, all `LEGACY`.
- **Longest description: 952 characters.** That fits the LMS column (`TEXT`, no form maximum).

**From WP-S1d:** descriptions are short (15–69 characters on the sample) but can contain line
breaks, which must survive into the LMS description. Onshore `LEGACY` activities have no title;
offshore ones do (WP-S3b). **From WP-S3b:** descriptions can be long (the contingent-well clause
runs past 300 characters), and one activity can describe several items ("Three Firm Wells;…").

**Title prefix rule (from WP-S4).** Prefix the title only when it adds information:

| Title | Activities | Prefix? |
|---|---|---|
| Default "<type> n" ("Other Activity 1", "Seismic Data 2", "Shoot 3D Seismic Data 1", "Firm Well 1") | most | No. The category already says it. |
| "Legacy Obligations: Original blocks … Competed n" | 882 | **Yes.** It carries the original block list. |
| Programme-level labels on `LEGACY` ("Initial Work Programme", "Legacy - Not Fallow 2010", one more, WP-S4c) | 567 | No. They describe the programme, not the activity. |
| Empty | 333 `LEGACY` | No title to use. |
| Hand-typed (e.g. "Geological Studies") | ~6 | Yes, or use it as `otherCategoryName` (M1). |

The `2D`/`3D` in a default reprocess title ("Reprocess 3D Seismic Data 1") is lost in
`REPROCESS_SEISMIC_DATA`. Keep it by prefixing "2D"/"3D" where the description doesn't already
say it.

### M3. Date option (the hard one)

PEARS gives at most a phase, and usually not even that. **WP-S2b (live, 29 October 2026):** only
282 programmes (all `P`) are phase-organised. The other 2,594 numbered programmes (2,161 `P` and
every onshore one) are flat lists with no structured timing. So rules 3 and 4 below cover about 90% of the
migration. Proposal, in order:

1. **Phase-organised activity** → `WITHIN_A_PHASE`, linked to the migrated `LicenceSchedulePhase`
   whose `PhaseType` matches `PHASE/TYPE` ('Phase A' → `PHASE_A`). If the schedule version has no
   such phase (the work programme and schedule disagree, D15): fall back to `WITHIN_A_TERM` Initial
   and report it. **WP-S7:** this never happens on live. Every work programme phase exists on the
   current schedule, so all 1,223 phase-list activities anchor to their phase. (The one exception,
   P2643, has no migrated schedule at all.)
2. **`DRILL_OR_DROP_WELL` with a `BREAKPOINT`** → `RELATIVE_DATE` against the Initial term,
   `relativeDuration` = breakpoint Y/M/D. That reproduces the drill-or-drop decision date. It needs
   the same PEARS-vs-LMS date arithmetic check as the schedule migration (days are added first in
   PEARS, months first in LMS).

   **WP-S6:** only 228 of the 885 programmes with a D&D well have a breakpoint (216 `P`, 12 `PEDL`).
   The other 657 fall to rules 3 and 4. Only 4 breakpoints have months or days, so the arithmetic
   check matters only for those. 5 `P` programmes have a breakpoint and no D&D activity; their
   breakpoint becomes a schedule comment ("Drill-or-drop breakpoint: n years from licence start")
   and is reported.

   **Revised after WP-S6b: the schedule's `DD` event comes first.** Of the 233 breakpoints:
   - **119 match the schedule's `DD` event;**
   - **17 differ, and the schedule `DD` is always later** (extensions recorded on the schedule but
     not on the work programme; 11 of them are one `PEDL` round with 5-, 8- or 14-year `DD`s
     against a 2-year breakpoint);
   - **97 have no `DD` event at all**, all `P`.

   The schedule is maintained and the breakpoint isn't, so:
   - **Schedule has a `DD` event (136):** date the D&D well from the schedule's `DD` offset, not
     the breakpoint. Report the 17 where the breakpoint disagrees.
   - **No `DD` event (97):** date it from the breakpoint, as above. Ignore P1647's `0y 0m 0d`.

   **Read the `DD` date from LMS, not PEARS.** The schedule migration has already brought the
   `DD` event in, and recorded it in `pears_schedule_event_migration_links`
   (`pears_event_mnem = 'DD'`) against its LMS event on the `ACTIVE` detail. So the mapper takes
   the `DD` date from the migrated schedule: the two then agree by construction, and no second
   PEARS query (or xview grant) is needed.

   **Timeline duplication:** as built, the schedule migration brings each `DD` event in as an
   `OTHER_ACTIVITY` schedule event named after the PEARS type's short title ("Drill n Drop" on
   dev), with its comments as the description (schedule plan, section 7, P1920). With this rule,
   136 licences would show the drill-or-drop decision twice on the same date: once as the D&D well
   activity and once as the schedule event. See Open question 13.
3. **Flat-list activity whose description names its phase** → `WITHIN_A_PHASE` for that phase.
   Some flat descriptions start with the phase and its length, e.g. "Phase A: 3 years (TOTAL
   PHASE A: 3 YEARS) ⏎ Obtain…" (WP-S4b). Use the `Phase [ABC]:` prefix only, at the start of the
   description, and only if the schedule version has that phase; otherwise fall through to the
   next rule. **WP-S5:** 108 flat activities start with a phase: 54 Phase A, 25 B and 29 C.
   Studies and seismic sit in A or B, and D&D and firm wells in C. That's only 3% of the 3,564
   flat activities. No flat description mentions a phase anywhere else, so the prefix rule misses
   nothing. **No inheritance (WP-S5b):** in these programmes each activity carries its own prefix,
   with only 7 `OTHER` activities without one, so the prefix isn't a heading over the activities
   below it. The 108 sit in about 25 `P` licences around P2309–P2312: one round that wrote
   phase-style programmes in the flat list.
4. **Flat-list activity** → `WITHIN_A_TERM` Initial. Under PEARS the work programme is the
   initial-term commitment, but for FALLOW/EXTENSION programmes the right term is unclear (Q2).
5. A schedule with no Initial term (schedule survey patterns A and G, which start at the Second
   term) → the first term present, reported.

### M4. Programme types

- `INITIAL` → activities on the schedule, as above.
- `FALLOW` and `EXTENSION`: no LMS equivalent grouping. In LMS, extension work comes in through the
  amend-work-programme application. Options:
  1. Migrate them as activities on the term they relate to (an extension programme probably relates
     to the Second term).
  2. Leave them out and report them.

  **WP-S1 (live, 29 October 2026):** they are rare. There are 27 `P` FALLOW, 2 `P` EXTENSION and
  1 `PEDL` EXTENSION programmes, 30 in all, so they can be handled one by one rather than by rule.
  At least 23 of the `P` licences concerned have no INITIAL programme at all.

  **WP-S1g (live, 3 November 2026):** 23 of the 30 are records of the 2010–11 fallow review (one
  `LEGACY` or `OTHER` activity whose description is the outcome, e.g. "Not Fallow - Block is
  Field"), not obligations. Only 7 hold wells or seismic, and 4 of those are dropped D&D wells. 14
  are on extant licences, some saved in 2026. 14 were created after the schedule's last term had
  ended, so no LMS term was in force. A third option:
  3. Keep the text as a schedule comment (M6's comments area) rather than an activity.

  Proposed: option 3 for the review records, option 1 for the 7 well and seismic activities, each
  confirmed by the licensing team on the "WP-M4 Fallow and extension" sheet. P251 and P252 can't
  migrate either way: their schedules have no terms.

### M5. Status

| PEARS | LMS | Notes |
|---|---|---|
| `NOT_STARTED` | `OPEN` | |
| `IN_PROGRESS` | `IN_PROGRESS` | |
| `COMPLETE` | `COMPLETE` | |
| `FULL_WAIVER` | `FULL_WAIVER` | |
| `PART_WAIVER` | **?** | 16 activities, all on surrendered licences. The notes show transfers, part waivers and errors (WP-S3b). Licensing team: Q12. |
| `WAIVER_DECLINED` | **?** | 16 activities: 1 extant (P1483), and 6 are really transfers to another licence (→ `TRANSFERRED`?). Licensing team: Q12. |
| `DROPPED` (D&D, legacy) | **?** | **466 activities (10%)**: 411 D&D wells (the normal outcome of 411 of 890) and 55 `LEGACY`. Too common for a comment: needs an LMS status, e.g. a new `DROPPED` (Q4). |

Current statuses on live (WP-S3): `COMPLETE` 2,577, `NOT_STARTED` 1,498, `DROPPED` 466,
`FULL_WAIVER` 170, `IN_PROGRESS` 44, `PART_WAIVER` 16, `WAIVER_DECLINED` 16.

History: one `WorkProgrammeActivityStatus` row per status note, with `appliedDatetime` = the note
`DATETIME`. If there is no note, one row for the current status, dated from the **first work
programme version in which the activity had that status**: a better date than the current
version's `start_datetime`, which is often just a later review save. That matches the carbon
storage precedent of one row per status.

**WP-S9b (live, 29 October 2026):**
- **Notes cover every non-`NOT_STARTED` status except on `P`.** Every `DROPPED`, `FULL_WAIVER`,
  `PART_WAIVER` and `WAIVER_DECLINED` activity has a note, as does every onshore and `PEDL`
  `COMPLETE`.
- **On `P`, 741 `COMPLETE` (30%) and 17 `IN_PROGRESS` have no note.** **WP-S9e:** all 758 are
  `LEGACY` activities that the 12/07/2011 bulk job created with the status already set, carried
  over from pre-2010 records. None was changed later without a note, so the history has no hidden
  gaps. Their fallback date is therefore 12/07/2011, the import date, not when the work was done.
  So each gets an `EventComment` alongside its status row: "Status carried over from legacy
  records on 12/07/2011; completion date not held in PEARS".
- 14 `NOT_STARTED` activities were changed and changed back, so they have notes and more than one
  history row.

### M6. Notes → comments

PEARS notes aren't always about one activity, so they go to two places.

**WP-S9 (live, 29 October 2026):** 4,973 notes, all with a date, an author and text.
- **2,090 are `GENERAL`** (never with an activity ID) → schedule comments.
- **2,883 are status changes** (always with an activity ID) → event comments and status history.

**Decided (29 October 2026): system-authored notes are not migrated.** The `LEGACY` bulk job of
12/07/2011 wrote general notes authored **"Migration Process"** (WP-S9d). They aren't empty
boilerplate:
- "Start Date: …", the licence start date;
- "[[MIGRATED DATA]] - Required to drill 1 drill or drop well(s). 1 … completed", a text form of
  the well counts;
- short pre-2010 remarks such as "DROPPED", "REL 2009", "Rep. Recd.".

All of them are still left out, along with any similar system-authored notes (WP-S9f lists the
authors). The rule is **by author, not by repeated text**: people also wrote the same note on many
licences, e.g. "Licence is determining with outstanding work commitments. Decision made by Director
on 15 July 2024" on 28 licences, and those migrate. Excluded notes are only counted in the report.
Notes written by people, general or activity, migrate as above.

**WP-S9f:** "Migration Process" is the **only** system author. Its 1,293 general notes are all
dated 12/07/2011. The other 19 authors are people. So the rule is exactly
`CREATED_BY = 'Migration Process'`, and the migration carries:
- **797 general notes → schedule comments;**
- **2,883 status notes → event comments.**

That's **3,680 of 4,973**. Authors are kept as the full name PEARS stored at the time, so one
person can appear under two names (e.g. "Mrs Jen Brzozowska" / "Jen Brzozowska").

**Activity notes** (`ACTIVITY_ID` set, matching a migrated activity) become an `EventComment`
`PUBLISHED`, with `timestamp = DATETIME`. The text includes `CREATED_BY`, because the PEARS author
is a full name, not a WUA id. **Decided: `authorWuaId` is left empty** (D20). LMS shows a blank
author for these, not "Unknown".

**Everything else** goes to the **schedule comments** area above the schedule (see Target):
- `GENERAL` notes;
- activity notes whose activity isn't migrated, e.g. on the duplicate-programme licences (Q10);
- activity notes whose `ACTIVITY_ID` matches no activity in the current version. **WP-S9c: none on
  live.** Every status note's activity still exists. The route is kept as a safeguard only.

Same author/date handling. A note about an activity that no longer exists is prefixed with the
note's `ACTIVITY_TITLE` (held on the note itself), so it still reads sensibly.

**Linked licence:** schedule comments can carry a linked licence. For the transfer cases (Q12, WP-S3b)
the target licence could be set there, but only where the licensing team confirms it. The PEARS
note names it in free text only, and it isn't parsed automatically.

**Dependency:** schedule comments are not built yet. The migration needs them first, or general
notes have nowhere to go. Open points for the LMS design:
- whether schedule comments belong to the schedule or to each schedule version (the migration
  attaches them once, to the `ACTIVE` detail's schedule, under Version alignment A);
- whether a migrated comment can be removed like a user-entered one.

## Version alignment

PEARS saves the schedule and the work programme **independently**. LMS activities live on a
schedule detail. Options:

- **A. Current only:** attach the activities of the current work programme version to the
  `ACTIVE` detail only; earlier schedule versions get no activities. Simple. Status history still
  arrives through the notes, but replaced LMS versions show no work programme.
- **B. Interleave:** for each migrated schedule version, attach the work programme version that was
  current at that schedule version's `start_datetime`. History is faithful, but it is more work.
  Work programme saves that happened between schedule saves don't get their own LMS version.
- **C. Interleave and split:** create extra LMS schedule details for work-programme-only saves.
  This breaks the schedule plan's "one LMS version per PEARS edit" rule, so not recommended.

**For the licensing team to decide (Open question 6, survey WP-L7), leaning A:** the current work
programme on the latest (`ACTIVE`) schedule version only (WP-S2, WP-S2c, live 29 October 2026).
PEARS is decommissioned some time after LMS goes live and its history isn't kept, so whatever
isn't migrated is lost. There are 17,416 work programme versions
(15,285 on `P`, up to 61 on one programme), against 10,834 schedule versions. Many are not content
changes: a review acknowledgement re-versions every programme on the licence, while status changes
through `set_work_programme_status` create no version at all. The status history migrates from
the notes (M5) instead.

**B is a point-in-time lookup.** Both tables have `start_datetime`, and, as built, each migrated
schedule version keeps its PEARS `start_datetime` as `createdInstant` and `appliedInstant`, and its
events' `pears_schedule_event_migration_links` rows name the PEARS version (`pears_plsd_id`) it came
from. So B is mechanical. The review-only
saves don't hurt it: B just picks whichever version was current, and identical content repeats.
What B can't show is:
- a work programme change superseded before the next schedule save. No LMS version holds it, so
  it is lost when PEARS is decommissioned;
- a change after the last schedule save. The `ACTIVE` detail needs the current work programme
  whatever its time, as in A;
- the first versions, made at award, before the schedule existed. The template schedule is
  generated from them at raise, so the first schedule version should pick them up.

Also, `set_work_programme_status` edits the current row in place, so a version's statuses can be
newer than its `start_datetime`. Statuses come from the notes either way.

**WP-S2c (live):** B would keep 1,161 of the 2,096 past content states (55%) and lose 935; 851
programmes changed after their last schedule save. See Open question 6.
Saves continue daily (latest 29/10/2026), but the migration and the `PED009X` redirect go live
together, so no re-run or delta is needed (see "Decision: no write-back to PEARS").

## Downstream consumers of the PEARS work programme

- **Licence documents:** `LICENCE_CREATE.document_data_get_mm_data` / `document_licence_get_mm_data`
  merge `LEGAL_TEXT`, `BREAKPOINT` and `DRILL_DROP_*` into the `PED_PRODUCTION_LICENCE*` templates.
  This matters for new licences raised in PEARS; existing licences' documents are already issued.
  `LEGAL_TEXT` stays in PEARS, unchanged, until the separate legal text migration (D14) reads it.
- **Template schedule generation:** `TEMPLATE_SCHEDULE.generate_schedule` reads phase/term timings
  from the work programme. This interacts with the schedule plan's "New production licences" section.
- **Raise screens and reports:** `PED017X` (readiness gating), `PED9010X`, and
  `PED_LICENCE_RAISE_REPORTS` / `PED_LICENCE_RAISE_DETAILS_REPORTS`.
- **Data warehouse:** `PED_WORK_PROGRAMME` is a protected object; the `VG_PWPD_*` MVs and the two
  XViews.
- **Not affected:** the LARRY extract and the ESRI work-programme-blocks views, which read the
  **application** work programme.

### Decision: no write-back to PEARS

**Decided (29 October 2026): the work programme is not written back to PEARS**, unlike the
schedule. Once a licence's work programme is migrated, LMS is its only maintained copy. The PEARS
`ped_work_programme_details` rows stay as they were at migration until PEARS is decommissioned,
some time after LMS goes live. Its history isn't kept after that.

This matches the schedule plan's write-back section, which already leaves LMS work programme
activities out. The schedule plan's reason ("no PEARS equivalent") is inaccurate: the equivalent is
`ped_work_programme_details`. But the outcome is the same, so nothing in the schedule write-back
changes.

Consequences:
- **`PED009X` redirects to LMS (decided).** The PEARS link to the work programme (the PEDLM uref
  `maintain` theme) redirects to the licence's LMS work programme, as the schedule plan does for
  `PED012X`. So PEARS can't edit a migrated work programme.
- **Data warehouse: no impact (confirmed).** The data warehouse doesn't currently report on work
  programmes, so the frozen `VG_PWPD_*` views, the two XViews and the protected
  `PED_WORK_PROGRAMME` object affect nothing today.
- **Other PEARS consumers see frozen data, which doesn't matter for migrated licences:**
  - **Raise reports** (`PED_LICENCE_RAISE_REPORTS`): only show licences still being raised, which
    haven't been migrated.
  - **Licence documents and template schedule generation:** both run at award/raise, before a
    licence is migrated. They matter only for new licences (Open question 7).
- **Schedule write-back is unaffected:** the `RD`/`PA` area events and the schedule XML don't carry
  the licence work programme.
- **Cutover: no gap (confirmed).** The migration and the `PED009X` redirect go live together, so
  no PEARS edit can be made between a licence's migration and its lockdown. No re-run or delta is
  needed for the work programme.

## Event reminders after migration

LMS reminds licensees of work programme activities as well as schedule deadlines
(`EventReminderJob`; `licence-schedule-reminders-data-model.md`). A migrated activity feeds it like
any LMS activity:

- **`WORK_PROGRAMME_ACTIVITY`, 6 months' notice**, on the activity's effective due date: its
  `due_date` for a `RELATIVE_DATE` (the D&D wells, M3 rule 2), otherwise the end of the term or
  phase it sits in. So under M3 rule 4 a flat activity is reminded on as the Initial term end
  approaches.
- **Only open activities.** An activity whose latest status is `COMPLETE`, `FULL_WAIVER` or
  `TRANSFERRED` is never reminded on (`WorkProgrammeActivityDeadlineServiceEvent.CLOSED_STATUSES`).
  **So a new `DROPPED` status (WP-L8) must be added to `CLOSED_STATUSES`**, or the 411 dropped D&D
  wells would be reminded on as if still owed. The same goes for any waiver status WP-L4 adds.
- Only the `ACTIVE` detail, only extant licences, only deadlines from today to 6 months ahead, and
  a deadline on the expiry date is covered by the expiry reminder instead. Past due dates, and
  every activity on an ended licence, send nothing.

**At go-live.** The schedule migration decided (its Open question 15, 23 November 2026) that every
reminder in its window goes out on the first run after migration, with nothing written to hold
them back. The work programme migration runs straight after it, so the same first run sends the
work programme reminders too: **one per open activity** on each extant licence whose Initial term
(or phase, or D&D date) ends in the next 6 months. That is likely to include the 12 extant
never-maintained `DRAFT_FINAL` licences (WP-L1), whose activities are all `NOT_STARTED`.
**Decided (25 November 2026): acceptable**, the same as for schedules (Open question 15). They all
go out on go-live, with nothing held back; the UAT rehearsal counts them so the volume is known.

## Findings in PEARS (worth knowing, not migration decisions)

- **LARRY import swaps seismic fields:**
  - `qry-get-lary-work-programmes` (`PED009X`) reads `OBTAIN_2D_SEISMIC` from `NEW_SHOOT_3D` and
    `SHOOT_3D_SEISMIC` from `OBTAIN_2D`.
  - The "-new" variant has no `OBTAIN_2D_SEISMIC`, but the Phase C import reads it.

  Imported activities may be miscategorised, so the migration should not trust the type alone for
  seismic.
- **Removing a status note doesn't revert phase-list activities:** `action-remove-note`
  (`PED009X`) restores the original status only on the flat `ACTIVITY_LIST`. On a phase-list
  activity the new status stays, with no note.
- **Programme status derivation ignores `DROPPED`:** a programme whose activities are all
  `DROPPED` matches no rule and keeps its previous status. PEDL225's `LEGACY` programme is
  `NOT_STARTED` with its only activity `DROPPED` (WP-S1c).
- **Programme status derivation** counts only the top-level `ACTIVITY_LIST`. A phase-organised
  programme with no flat activities is derived as `COMPLETE`. On live, no programme mixes the two
  lists (WP-S2b), so this affects **all 282 phase-organised `P` programmes** once they are saved in
  maintain.
- `TEMPLATE_SCHEDULE.generate_schedule` does not filter by programme type or by `record_status`, so
  a deleted or FALLOW programme's phases can feed the template schedule.
- `PED910X` calls `PED009X` themes `prepare` and `update`, which don't exist.

## Data survey needed

The queries are in the data survey (`pears-work-programme-migration-data-survey.md`, a migration document provided
separately). All
were run on live on 29 October 2026, except WP-S4c (optional) and WP-S8b (left for the licensing
team). WP-S1e, WP-S1f, WP-S1g and WP-S4d were added and run on 3 November 2026. Each item feeds
these parts of the plan:

| # | Survey item | Feeds |
|---|---|---|
| WP-S1 | Programmes by licence type, programme type and status | D2, M4, Q2 |
| WP-S1e | The `DRAFT_FINAL` programmes, one row per licence | Scope, Q9 |
| WP-S1f | Activity statuses on P2614 and P2626 | D11 |
| WP-S1g | The `FALLOW` and `EXTENSION` programmes, one row per programme | M4 |
| WP-S2 | Versions per programme; flat vs phase organisation | D1, Version alignment, Q6 |
| WP-S2c | Work programme changes vs schedule versions (option B) | D21, Version alignment, Q6 |
| WP-S3 | Activities by type, status and list | D4, D8, M1, M5 |
| WP-S4 | Titles of `SEISMIC`, `LEGACY` and `OTHER` activities; proposed seismic classification | D5, D6, M1 |
| WP-S4d | The seismic activities that describe two operations | M1 |
| WP-S5 | Descriptions: empty, template text, competed blocks | D6, D7, M2 |
| WP-S6 | Breakpoints and drill-or-drop wells; breakpoint vs the schedule's `DD` event | D13, M3 |
| WP-S7 | Phase timings vs the current schedule's phases | D15, M3 |
| WP-S8 | Well-count matrix vs activities | D12, Q5 |
| WP-S9 | Notes: status history and general notes | D9, D10, M5, M6, Q5 |
| WP-S10 | Work programme vs schedule coverage, by licence status | Scope |
| WP-S10b | Work programme licences with no current status | Scope |
| WP-S11 | Soft-deleted and multiple-current rows | D19 |
| WP-S12 | `ACTIVITY_ID` uniqueness and stability across versions | D17 |
| WP-S13 | Legal text and review audit | D14, D16, Q5 (legal text: for the later legal text migration) |

## Design

Built on the schedule migration as built on `develop` (LMS1-787), which it runs after: the same
`migration/pears` reading pattern, the same background run, cleardown, dry run, comparison report
and validation sweep. The building of activities, statuses and comments follows
`CarbonStorageLicenceMigrationService` (`buildWorkProgrammeActivity`,
`buildWorkProgrammeActivityStatuses`, `buildWorkProgrammeActivityComments`), and the writing
follows `PearsScheduleWriter`. Everything assumes Version alignment A (current work programme on
the `ACTIVE` detail only); if the licensing team chooses B (WP-L7), steps 2 and 3 repeat per
migrated schedule version.

### What the schedule migration provides

What is on `develop` in `migration/pears/schedule` (schedule plan, "Design", sections 1–7), and how
this migration uses it:

| Built | Used here for |
|---|---|
| `PearsScheduleService`: SQL from `src/main/resources/pears-migration/` through the `sql(name)` helper, one licence per query (`licence-schedule.sql`), a licence list query (`schedule-licence-references.sql`), and `checkReadable()`, which throws `MigrationPreconditionException` naming every table it can't read | The same shape for `PearsWorkProgrammeService` (step 1) |
| `PearsScheduleMigrationService.migrateLicence(licenceReference, eventTypes, runId)`: reads and maps PEARS **before** opening the LMS transaction (`TransactionOperations`), then one transaction per licence, returning `MIGRATED`, `SKIPPED` or `FAILED` | The same split in `PearsWorkProgrammeMigrationService` (step 3) |
| `pears_schedule_migrated_licences` (manual script `manualscripts/pearsschedulemigration/0001_…`): one row per schedule the migration created, with its run | **The prerequisite check.** A licence's work programme migrates only onto a schedule listed here, never onto a schedule LMS created itself |
| `pears_schedule_event_migration_links`: the PEARS version (`pears_plsd_id`), type and position of every migrated schedule event | Finding the migrated `DD` event (M3 rule 2), and each LMS detail's PEARS version under option B |
| Migrated details: `createdInstant` and `appliedInstant` = the PEARS `start_datetime`, `createdByWuaId` and `appliedByWuaId` = `MigrationUser.WUA_ID` | Nothing new: activities go onto the existing `ACTIVE` detail, which keeps these |
| Run and queue tables (manual script `0002_…`), with `kind` = `MIGRATE`, `CLEARDOWN` or `VALIDATE`, one active run at a time; `PearsScheduleMigrationJob` (ShedLock), `PearsScheduleMigrationRunService`, `PearsScheduleMigrationController` (`/migration/pears-schedules`, start, `validate`, `cleardown`, status, CSV `report`, `retry-failed`, `cancel`) | Generalised to run the work programme too (step 4) |
| `PearsScheduleCleardownService`: deletes a migrated schedule and everything hanging off it, **including** `work_programme_activities`, `work_programme_activity_statuses`, `event_comments`, reminders and the event cache, with their audit rows | Clearing a schedule clears its work programme too, but the new work programme link table must be added to it (step 4a) |
| `PearsScheduleComparisonService` / `Controller` (HTML, CSV, `licences=all`, `issues=only`, `notMigrated=only`, `versions=all`), and `PearsScheduleValidationService` (the `VALIDATE` dry run, always rolled back) | The model for steps 5 and 6 |
| `PearsScheduleMigrationValidationTest` (`pearsMigrationTest`, report in `build/reports/pears-migration/`) | The model for step 5's sweep |
| `documentation/development/pears-schedule-migration-local-run.md` | Extended with the work programme run |

**Which licences have a migrated schedule.** Only those the schedule migration migrates, so the
work programme of a licence it fails or skips is skipped too, with that reason:
- schedules with **no terms** (pattern E, about 40 on live, including P251 and P252 from M4);
- licences with a **bespoke milestone**, until prerequisite A (`BESPOKE_MILESTONE`) lands. They
  migrate on a later schedule run, and their work programme with them;
- licences not in LMS (never issued, as on dev's dry run).

On dev PEARS the schedule migration migrated 892 licences, all matching PEARS (schedule plan,
section 7). Those are the population for local testing of this migration.

**Prerequisite:** the licence's schedule has been migrated by the schedule migration (a
`pears_schedule_migrated_licences` row). The work programme migration reads the LMS schedule it
produced (terms, phases, start date, the `DD` event) and never creates a schedule itself.

### 1. Read PEARS: `PearsWorkProgrammeService` (`migration/pears/workprogramme`)

- `@Service @ConditionalOnPearsDataSource`, on the existing read-only `pearsDataSource`, like
  `PearsLicenceService` and the schedule migration's `PearsScheduleService`.
- SQL in `src/main/resources/pears-migration/`, read one licence at a time as the schedule
  migration does:
  - `work-programme-licence-references.sql`: every numbered licence with a current, non-`DRAFT`
    programme, for queuing a run.
  - `work-programmes.sql`: for one licence, the current, non-`DRAFT` row of each programme, with
    `ped_wp_id`, `ped_lm_id`, licence type and number, and `xml_data`. The filters are the
    survey's "current" convention: `status_control = 'C' AND record_status = 'ACTIVE'`, and
    `WORK_PROGRAMME_STATUS != 'DRAFT'`.
  - `work-programme-activities.sql`: `XMLTABLE` over `/WORK_PROGRAMME//ACTIVITY` (flat and phase
    lists) with `ACTIVITY_ID`, `ACTIVITY_TYPE`, `TITLE`, `ACTIVITY_STATUS`, `COMPETED_BLOCKS`,
    `DESCRIPTION` (as a CLOB; descriptions keep their line breaks), the list kind and the enclosing
    `PHASE/TYPE`.
  - `work-programme-notes.sql`: `XMLTABLE` over `NOTES_LIST/NOTE`, excluding `CREATED_BY =
    'Migration Process'` (decided, M6).
  - `work-programme-status-dates.sql`: for each activity, the first version in which it had its
    current status (the M5 fallback date), from all `ACTIVE` versions.
  - Programme-level values: `WORK_PROGRAMME_TYPE` and `BREAKPOINT` Y/M/D. **The well-count matrix
    is not read** (D12, decided) and **`LEGAL_TEXT` is not read** (D14).
  - The `DD` date isn't read from PEARS: it comes from the migrated LMS schedule (M3 rule 2).
- Return immutable records: `PearsWorkProgramme(licenceReference, pedWpId, pedWpdId, type,
  breakpoint, wellCounts, List<PearsActivity>, List<PearsNote>)`.
- Grant check (`checkReadable()`), as for the schedule migration: the migration user needs `SELECT` on
  `ped_work_programme`, `ped_work_programme_details` and the schedule xview.

### 2. Map: `PearsWorkProgrammeMapper`

A pure function from `PearsWorkProgramme` + the licence's migrated LMS schedule (the `ACTIVE`
detail's terms and phases by type, start date and `DD` event, exactly as the schedule migration
wrote them: see "Run straight after the schedule migration" below) to a `MigratedWorkProgramme` value: unsaved activities, status rows, event
comments and schedule comments, plus `MigrationNotes`. No Spring and no database, so it
is unit-tested from fixture XML, as `PearsOperationMapper` is. Per activity, in this order:

1. **Skip** "None" activities (M2) and record them.
2. **Category and commitment** by `ACTIVITY_TYPE` (M1), with the seismic title and description
   classification (WP-S4b), the WP-L13 answers (split, or the agreed category for "obtain and
   reprocess") and the defaults from WP-L9. An unknown `ACTIVITY_TYPE` fails the
   licence.
3. **Description** (M2): `DESCRIPTION`, the title prefix rule, the 2D/3D prefix for reprocess, and
   `COMPETED_BLOCKS` appended; the title, then the category name, when it's empty. Strip the one
   pasted `javascript:` link and record it.
4. **Date option** (M3), rules 1 to 5 in order. Rule 2 uses the schedule's `DD` date where there
   is one (the `ACTIVE` detail's event linked to `DD` in `pears_schedule_event_migration_links`),
   otherwise the breakpoint, as `RELATIVE_DATE` on the Initial term. LMS calculates the due
   date as the term start plus `relativeDuration` (`LicenceScheduleCalculationService`), so the
   offset is converted from licence start to Initial term start (the same date on a normal
   schedule).
5. **Identity:** a new `originalEventId` per `ACTIVITY_ID`.
6. **Status history** (M5): one `WorkProgrammeActivityStatus` per status note, oldest first;
   otherwise one row for the current status, dated by the fallback. The 758 bulk-job `LEGACY`
   statuses add the "Status carried over from legacy records …" comment. `DROPPED`,
   `PART_WAIVER` and `WAIVER_DECLINED` follow WP-L8 and WP-L4.
7. **Comments** (M6): each status note becomes a `PUBLISHED` `EventComment`, with `timestamp` =
   `DATETIME`, `authorWuaId` empty, and the author's name in the text (D20). Suggested text:
   `<DESCRIPTION>` followed by "(PEARS note by `<CREATED_BY>`)".

Per programme:

- General notes become schedule comments (M6), with the same author and date handling.
- A breakpoint with no D&D well follows D13. Well counts are not migrated (D12). Legal text is
  left in PEARS for its own migration (D14).
- A cleared `FALLOW`/`EXTENSION` programme follows its WP-L12 answer: activities on the chosen
  term (M4 option 1), one schedule comment with the activity text (option 3), or left out and
  reported.

**Run straight after the schedule migration.** The schedule migration is completed first, and
this migration runs immediately after it, before LMS users are told they can edit schedules
(release step 18). So every migrated schedule is still exactly as the schedule migration wrote it:
its `ACTIVE` detail is the migrated current PEARS version. **The LMS database holds no `DRAFT`
schedule details and no work programme activities** on PEARS licences at that point, so the
migration doesn't have to merge with anything, and doesn't check for drafts.

Per licence, before mapping, the licence is **skipped with a reason** when:
- it has no schedule from the schedule migration (no `pears_schedule_migrated_licences` row): the
  1,111 work-programme-only licences (WP-S10), and those the schedule migration failed or skipped
  ("What the schedule migration provides");
- it is one of the 6 duplicate-INITIAL licences, until WP-L2 is answered;
- it has a `FALLOW` or `EXTENSION` programme not yet cleared by the licensing team (M4, WP-L12);
- its `ACTIVE` LMS detail already has migrated activities (idempotency, below).

### 3. Write: `PearsWorkProgrammeMigrationService`

- `migrateLicence(licenceReference, runId)`: reads and maps PEARS **before** the LMS transaction
  opens (`TransactionOperations`), then writes one licence per transaction, returning `MIGRATED`,
  `SKIPPED` with a reason, or `FAILED` with notes, as `PearsScheduleMigrationService` does.
  `licencesToMigrate()` checks the grants and that PEARS holds work programmes, and lists the
  licences.
- Writing in a `PearsWorkProgrammeWriter`, as `PearsScheduleWriter` does for schedules. Save through
  the existing services, in the carbon storage order:
  `workProgrammeActivityService.saveWorkProgrammeActivities`,
  `workProgrammeActivityStatusRepository.saveAll`, `eventCommentRepository.saveAll`, then the
  schedule comments once they are built. Activities are added to the existing `ACTIVE` detail. No
  new schedule version is created, so nothing is published to PEARS (the write-back carries no
  work programme anyway).
- Then `licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(activeDetail)` to set
  the `RELATIVE_DATE` due dates, and `crossLicenceEventTrackerService.refreshScheduleCache(activeDetail)`.
- **Link table**, in the same transaction, one row per migrated activity. It drives idempotency,
  the comparison report and the cleardown. It is migration-only, so, like the schedule migration's
  tables, it is created by a **manual script**
  (`manualscripts/pearsworkprogrammemigration/0001_pears_work_programme_migration_tables.sql`),
  run by hand in each environment before the migration, not by Flyway. Integration tests create
  it from a copy in `src/test/resources/migration`.

  ```sql
  CREATE TABLE lms.pears_work_programme_activity_migration_links (
    schedule_event_id UUID PRIMARY KEY,
    pears_wp_id       BIGINT NOT NULL,   -- ped_work_programme.id
    pears_wpd_id      BIGINT NOT NULL,   -- ped_work_programme_details.id migrated from
    pears_activity_id BIGINT NOT NULL,   -- ACTIVITY_ID
    pears_type        TEXT   NOT NULL,   -- ACTIVITY_TYPE
    run_id            UUID   NOT NULL,   -- no foreign key, so clearing runs leaves it
    CONSTRAINT pears_work_programme_activity_migration_links_event_fk FOREIGN KEY (schedule_event_id)
        REFERENCES lms.schedule_events (id)
  );
  ```
- **Idempotent:** a licence whose `ACTIVE` detail already has link rows is skipped.
- Throws `MigrationPreconditionException` if PEARS returns no work programmes at all, or no schedule
  has been migrated, so an empty run can't look like success.

### 4. Run in the background

Reuse the schedule migration's background run (schedule plan, section 4) rather than a second copy
of it: its run and queue tables, `PearsScheduleMigrationJob` (ShedLock), the run service and the
same endpoints. A manual script, `0002_pears_work_programme_migration_runs.sql`, adds a
`migration` column (`SCHEDULE`, the default for existing rows, or `WORK_PROGRAMME`) to
`pears_schedule_migration_runs`. The job hands each batch to the schedule or the work programme
service by `migration` and `kind`. The one-active-run index stays as it is, so a work programme run
can't start while a schedule run is active, which is the order wanted. The classes and tables
keep their schedule names, to leave the built code alone; renaming them to a general
`PearsMigrationRun…` is optional, in a PR of its own. The work programme run is far smaller (1,747
licences, about 4,800 activities and 3,700 comments, current version only).

Controller (`@Profile("migration")`, `/migration/pears-work-programmes`), as for schedules: start
(GET or POST, optional `licenceReference`), `validate` (a dry run, step 5), `cleardown` (step 4a),
`{runId}` status, `{runId}/report` (CSV), `{runId}/retry-failed` and `{runId}/cancel`. Starting a
run returns 409 if PEARS has no work programmes, no schedule has been migrated, or a run is already
`QUEUED` or `RUNNING`.

### 4a. Clear down, so the migration can be run again

As for schedules (schedule plan, section 4a): **never in production**, behind
`@Profile("migration")` and `pears.work-programme-migration.cleardown-enabled` (default `false`),
as a `CLEARDOWN` run.

- **Work programme only.** For each licence with link rows, one transaction deletes the migrated
  activities, their status rows, comments, reminders, missing-contact rows and event cache rows,
  the schedule comments it wrote, the link rows, and their audit rows. The schedule stays, so the
  work programme can be migrated onto it again.
- **The schedule cleardown must clear the link rows too.** `PearsScheduleCleardownService` already
  deletes a schedule's `work_programme_activities`, statuses and comments, but the new link
  table's foreign key would stop it. So it also deletes
  `pears_work_programme_activity_migration_links` by its events, next to
  `pears_schedule_event_migration_links`, and a schedule cleardown still clears everything.

### 5. Validate: `PearsWorkProgrammeMigrationValidationTest` and a dry run

Modelled on the schedule migration's validation as built (schedule plan, section 5).

**Gradle sweep:** `@Tag("pears-migration")`, run by the `pearsMigrationTest` Gradle task when
`PEARS_DATASOURCE_URL` is set, narrowed with `PEARS_MIGRATION_LICENCES`. For every in-scope licence,
in parallel and in its own rolled-back transaction, it migrates the schedule (through
`PearsScheduleMigrationService`) and then the work programme into the test database, compares the
result through the comparison service (step 6), and writes
`build/reports/pears-migration/work-programme-comparison.txt`. A licence with an issue fails; one
the migration skips or fails for its PEARS data is skipped and counted. It checks:

- **Counts:** PEARS activities = LMS activities + skipped "None" activities; PEARS human notes =
  event comments + schedule comments; status rows = status notes, or 1 where there are none.
- **Mapping:** each activity's category, commitment and current status against the M1 and M5
  tables, through the link table.
- **Dates:** every LMS due date is set. Phase activities fall on their phase's end date, term
  activities on the term's end date, and D&D wells on the schedule's `DD` date (or the breakpoint
  date where there's no `DD`).
- **Text:** each LMS description contains the PEARS description. Legal text isn't compared (D14).

**Dry run, from an endpoint:** `GET` or `POST /migration/pears-work-programmes/validate` queues a
`VALIDATE` run, as for schedules. `PearsWorkProgrammeValidationService` migrates each licence's
work programme onto its migrated schedule in a transaction that is always rolled back, compares
it, and records `MATCHED`, `DIFFERS` (with the first issues), `SKIPPED` or `FAILED` on the queue
row. The run's CSV report lists every licence. This is the check for UAT, with no PEARS
credentials on a developer's machine.

### 6. Compare selected licences: `PearsWorkProgrammeComparisonController`

For the licensing team to check chosen licences side by side, in any environment, as for schedules:

```
/migration/pears-work-programmes/compare?licences=P1234,P5678          -> HTML table
/migration/pears-work-programmes/compare?licences=P1234&format=csv     -> CSV download
/migration/pears-work-programmes/compare?licences=all                  -> every licence the migration wrote
/migration/pears-work-programmes/compare?licences=all&issues=only      -> only the licences with issues
/migration/pears-work-programmes/compare?licences=all&notMigrated=only -> only the licences with activities skipped
```

The options are the ones the schedule comparison has as built, and combine the same way.

One table per licence, one row per PEARS activity, paired with its LMS activity through the link
table:

| PEARS type | PEARS title / description | PEARS status | LMS category / commitment | LMS description | LMS date option and due date | LMS status | Comments | Result |
|---|---|---|---|---|---|---|---|---|

`Result` is `Match`, `Mapped` (an accepted mapping such as `NOT_STARTED` → `OPEN`), `Skipped` (with
the reason), `Missing in LMS` or `LMS only`. Rows to look into are shaded red and skipped ones
yellow, as for schedules. Below the table: the general notes against the schedule comments, and
the well counts and breakpoint against wherever LMS holds them. The validation test and the dry
run build the same comparison model, so none of them can disagree.

## Migration size (live, 29 October 2026)

- **Licences:** 1,747 with a schedule and a work programme, 386 extant (WP-S10), less the 6
  duplicate-INITIAL licences (WP-L2), any `FALLOW`/`EXTENSION` licences not yet cleared (M4), and
  those whose schedule the schedule migration doesn't migrate (no terms; a bespoke milestone until
  prerequisite A lands).
- **Activities:** 4,787 current across all programmes, 4,779 after the 8 "None" skips (WP-S3,
  WP-S5). Those on licences without a schedule aren't migrated.
- **Comments:** 2,883 event comments and 797 schedule comments (WP-S9c), plus the 758 bulk-job
  status comments (M5).
- **Status rows:** one per status note, plus one per activity with no note.
- Current version only, so some tens of thousands of LMS rows: a single short run.

## Delivery and release steps

### Before coding

1. **Data survey: done** (live, 29 October 2026; WP-S1e to WP-S1g and WP-S4d on 3 November 2026).
   Re-run WP-S1, WP-S1e, WP-S1g, WP-S3, WP-S4d, WP-S9c and WP-S10 on the day, once the redirects
   are on (release step 18.2), and compare the lists with the answered workbook sheets, since PEARS
   keeps changing until then.
2. **Licensing team review:** send
   the licensing team review document with the licensing items spreadsheet (migration documents,
   provided separately; per-licence sheets for WP-L1/WP-L11, WP-L2, WP-L4, WP-L5, WP-L12 and WP-L13; re-run WP-S1e just
   before the team answers WP-L1), and record the answers to WP-L1 to WP-L13 in this plan. WP-L4, WP-L8 and WP-L9 decide LMS
   changes below, and WP-L7 decides whether steps 2 and 3 of the design repeat per version.
   WP-L10 (legal text) is no longer needed for this migration: legal text goes in one central area,
   built later (D14, Open question 5). **Already answered (25 November 2026):** WP-L3 (term- and
   phase-level dating is acceptable), WP-L6 (the well-count matrix is not migrated) and the
   go-live reminders (Open question 15). Drop them from the review document if it hasn't gone.

### LMS changes ahead of the migration (each its own change)

3. **Schedule comments area** (planned, mockup): needed before any general note can migrate (M6).
4. **`DROPPED` status** in `WorkProgrammeStatus` (WP-L8), with form, timeline and tests, and any
   waiver statuses WP-L4 adds. Each is also added to the reminder job's closed statuses
   (`WorkProgrammeActivityDeadlineServiceEvent.CLOSED_STATUSES`, "Event reminders after migration").
5. **Legal text: not needed for this migration** (decided 23 November 2026, D14). LMS builds legal
   text later as a feature of its own: one central legal text area for all the activities (Open
   question 5). PEARS legal text is migrated separately after it, which also settles how licence
   documents read it. See "Follow-up: once the legal text area is built".
6. **Blank author** for comments with no `authorWuaId` (D20), instead of "Unknown"
   (`EventCommentService.createViewFrom`, still "Unknown" on `develop`).
7. **Commitment** default or optional (WP-L9), if LMS validation changes.

The schedule migration's own prerequisite A (`BESPOKE_MILESTONE`) isn't needed here, but until it
lands the licences with a bespoke milestone have no migrated schedule, so their work programmes
wait too.

### Build

8. `PearsWorkProgrammeService` + SQL resources + grant check.
9. `PearsWorkProgrammeMapper` + unit tests from fixture XML. Include: a flat and a phase-organised
   programme; a `LEGACY` activity with a "Legacy Obligations" title; each seismic title pattern and
   the generic "Seismic Data n" cases; a "Phase A:" flat description; a D&D well with a migrated
   `DD` event and one with only a breakpoint; a status history from notes and one with no notes; a
   "None" activity; general and system-authored notes.
10. `PearsWorkProgrammeMigrationService` + `PearsWorkProgrammeWriter` + link table (manual script)
    + integration test (Testcontainers Postgres, on a schedule migrated by `PearsScheduleWriter`).
11. Background run: the `migration` column on the schedule migration's run table, the work
    programme path in the job and the controller, with tests. Cleardown (step 4a), including the
    change to `PearsScheduleCleardownService`.
12. `PearsWorkProgrammeComparisonService` + controller (HTML + CSV) + tests. Build it **before** the
    first UAT run, so the licensing team can check licences from the first run.
13. `PearsWorkProgrammeMigrationValidationTest` and the `VALIDATE` dry run, built on the comparison
    service.

### Pull requests

As for the schedule migration (schedule plan, "Pull requests"), the code (steps 8–13) is delivered
as a **stack of pull requests**, each small enough to review on its own, with nothing reaching
`develop` until the whole migration is ready. The stack works the same way:

- **PR 1** branches from `develop` and targets it, and collects the rest of the stack.
- **Each later PR** branches from the one before and targets it, so its diff shows only its own
  change. When approved, it is added to its parent with "Rebase and merge", and the next PR is
  retargeted to that parent.
- **Each PR builds and passes its tests on its own.** Nothing is reachable until PR 6 adds the
  endpoints, and everything is under `@Profile("migration")`.
- **The stack is rebased, never merged:** PR 1 onto `develop` regularly, each PR above onto the
  rebased one below (`git rebase --onto`), force-pushed with `--force-with-lease`.
- **No Flyway:** the migration's tables are manual scripts, so there are no version numbers to clash
  with `develop`. Any Flyway change belongs to the LMS changes (steps 3–7), not the stack.

Titles follow the schedule stack's convention: "#N Work programme migration: …", numbered by
position in the stack.

**Before the stack (straight to `develop`, each its own PR):** LMS features in their own right,
which the write (PR 5) needs. The mapper (PRs 2–4) doesn't: it holds PEARS's statuses and the
general notes as its own records, and PR 5 turns them into entities.

| PR | Delivery step | Contents |
|---|---|---|
| A | 3 | **Schedule comments area**, from the mockup (`scheduleCommentsSection`): `schedule_comments` (V140), shown to regulators with author, date and text, removable by schedule and work programme administrators. No add screen; the linked licence is left for later. |
| B | 4 | **`DROPPED`** (and any waiver statuses WP-L4 adds) in `WorkProgrammeStatus`: form, timeline, reminder closed statuses, tests. Needs WP-L8 and WP-L4 answered. |
| C | 6 | **Blank author** for comments with no `authorWuaId`, instead of "Unknown". |
| D | 7 | **Commitment** default or optional, if WP-L9 changes LMS validation. |

**The migration stack:**

| PR | Delivery step | Contents | Done when |
|---|---|---|---|
| 1 | 8 | **Read PEARS.** `PearsWorkProgrammeService` (`migration/pears/workprogramme`), `work-programme-licence-references.sql`, `work-programmes.sql`, `work-programme-activities.sql`, `work-programme-notes.sql`, `work-programme-status-dates.sql`, the immutable records (`PearsWorkProgramme`, `PearsActivity`, `PearsNote`) and `checkReadable()`. | Unit tests over mocked JDBC, as for `PearsScheduleService`; fails clearly on a missing grant. The queries run by hand against dev or UAT PEARS for a flat, a phase-organised and a `LEGACY` licence. |
| 2 | 9 | **Mapper: activities.** "None" skips, category and commitment (M1, with the seismic classification and the WP-L13 and WP-L9 answers), description (M2), identity, `MigrationNotes`. | Unit tests from fixture XML: every `ACTIVITY_TYPE`, each seismic title pattern and the generic "Seismic Data n" cases, a "Legacy Obligations" title, an empty description, the pasted `javascript:` link, an unknown type failing the licence. |
| 3 | 9 | **Mapper: dates.** M3 rules 1–5 against the migrated schedule: phase, the migrated `DD` event or the breakpoint as `RELATIVE_DATE`, the "Phase A:" prefix, Initial term, the first term present; the `FALLOW`/`EXTENSION` answers (WP-L12). | Unit tests: a phase-organised programme, a D&D well with a `DD` event and one with only a breakpoint (including one with months), a "Phase A:" description, a schedule with no Initial term (patterns A and G). |
| 4 | 9 | **Mapper: statuses and notes.** Status history from notes, the fallback date and the bulk-job `LEGACY` comment (M5), the WP-L4 and WP-L8 mappings, status notes as event comments, general notes as schedule comments, "Migration Process" notes left out (M6), and a breakpoint with no D&D well (D13). | Unit tests: a history from notes, one with no notes, a changed-and-changed-back `NOT_STARTED`, a `GENERAL` note, a system-authored note, a note on an activity not migrated. |
| 5 | 10 | **Write.** `PearsWorkProgrammeMigrationService.migrateLicence` (PEARS read before the transaction, one licence per transaction, idempotent, skip reasons), `PearsWorkProgrammeWriter` onto the `ACTIVE` detail, date calculation, cross-licence cache refresh, and the link table (manual script). Needs A, B and C on `develop`. | Integration test (Testcontainers Postgres, on a schedule migrated by `PearsScheduleWriter`, mapper input stubbed): migrate, re-run skips, failure rolls back the whole licence, a licence with no migrated schedule is skipped. |
| 6 | 11 | **Background run and cleardown.** The `migration` column (manual script), the job's work programme path, `/migration/pears-work-programmes` (start, status, report, retry-failed, cancel, cleardown), the work programme cleardown behind its property, and the link rows in `PearsScheduleCleardownService`. | Tests: a work programme run after a schedule run, refused while one is active; cancel, retry, restart mid-run; cleardown then migrate again with the same result, and a schedule cleardown still clearing everything once work programmes are migrated; 404 without the property. |
| 7 | 12 | **Comparison.** `PearsWorkProgrammeComparisonService`, `PearsWorkProgrammeComparisonController` (HTML, CSV, `licences=all`, `issues=only`, `notMigrated=only`). | Tests over a migrated fixture licence: every result type. |
| 8 | 13 | **Validation.** `PearsWorkProgrammeValidationService` and the `VALIDATE` run (dry run, rolled back), and `PearsWorkProgrammeMigrationValidationTest` (`pearsMigrationTest`), both on the comparison service. | Runs against dev PEARS on top of the schedule sweep; differences are only known, accepted cases. |

When PR 8 is approved and in PR 1, PR 1 is reviewed as a whole once more and goes into `develop`.
That is the code the UAT run (step 14) uses, with the comparison report available from the first
run. The local-run guide (`documentation/development/pears-schedule-migration-local-run.md`) gains
the work programme steps in PR 6.

### Validate in UAT

14. Run the schedule migration to completion, then the work programme migration straight after it,
    against UAT PEARS, as on the day. Locally, do the same against dev PEARS first, on the licences
    the schedule plan's section 7 lists, and a dry run of every licence.
15. Run the validation test and a `VALIDATE` dry run across the estate. Iterate until every
    difference is known and accepted. Run the reminder job against the migrated data, with email to
    a test inbox, to count the work programme reminders go-live will send (Open question 15:
    decided they all go out, so the count is for awareness, not a decision).
16. **Licensing team sign-off:** the team checks chosen licences in the comparison report, at least
    one per pattern: flat, phase, `LEGACY`, D&D with and without a `DD` event, transfers and
    waivers (WP-L4), and an extant licence with recent notes. They also review the skipped list.
17. Rehearse both runs on a copy of production data, timing them for the go-live window.

### Release

18. **Order on the day.** This slots into the schedule plan's go-live order ("Write-back transport",
    "Go-live order"), which the work programme steps are marked against:
    1. **Redirects on (schedule step 1).** Switch the PEARS schedule screen (`PED012X`) **and** the
       work programme screen (`PED009X`) to redirect to LMS for migrated licences, in one PEARS
       release. From here no schedule or work programme edit can be made in PEARS, so the redirect
       is the freeze ("Decision: no write-back to PEARS").
    2. **Survey checks.** Re-run the step 1 queries now that PEARS can't change, and compare with
       the rehearsal.
    3. **Schedule migration (schedule step 2):** final run `COMPLETE`, with failures reviewed or
       accepted, the `RD`/`PA` refresh done and the `ped_lms_schedule_sync` rows created.
    4. **Work programme migration (new, between schedule steps 2 and 3):** started once the
       schedule run is `COMPLETE`, and run straight after it to `COMPLETE`, with failures reviewed
       or accepted. Nobody edits a schedule in between, and LMS holds no drafts or work programmes
       on these licences, so it finds every schedule as the schedule migration wrote it (design
       step 2). The reminder job's next run sends the work programme reminders with the schedule
       ones ("Event reminders after migration").
    5. **Write-back on (schedule step 3):** switch on the LMS → PEARS schedule write-back consumer.
       The work programme isn't part of the write-back, so this step doesn't depend on step 4's
       content, only on it having finished.
    6. **Area messages on (schedule step 4):** `LICENSING_RETENTION_AREAS` publishes for migrated
       licences, and LMS's subscription is switched on.
    7. **Open LMS editing:** tell users they can edit schedules and work programmes in LMS.
19. **After go-live:** spot-check licences in the comparison report, and go through the skipped
    list with the licensing team (licences with no schedule, review document 2.1). The fallow and
    extension programmes, duplicate-INITIAL licences, two-operation seismic activities and D&D date
    disagreements are settled before the run, on the workbook sheets (step 2). Any not cleared by
    go-live are migrated later with single-licence runs.
20. **Rollback:** until LMS editing opens (step 18.7), a licence's migrated work programme can be
    removed through its link rows and re-run. The cleardown (design step 4a) does that outside
    production; in production it would be a hand-run delete of the same rows. After that, LMS holds live data and fixes are made in
    LMS, not by re-running. PEARS keeps its rows until it is decommissioned, but nothing reads them
    once the redirect is on.

## Open questions

1. ~~**Ownership:** does LMS become the owner of migrated licences' work programmes, or is this a
   read-only copy?~~ **Resolved (29 October 2026):** LMS owns them, with no write-back to PEARS.
   See "Decision: no write-back to PEARS". The PEARS link redirects to LMS, the data warehouse
   doesn't report on work programmes, and migration and lockdown happen together, so nothing is
   left open.
2. **Term for flat-list and FALLOW/EXTENSION activities:** default `WITHIN_A_TERM` Initial, or
   something else? **Flat-list: resolved with Open question 11 (25 November 2026),** the
   `WITHIN_A_TERM` Initial default stands. Still open: the 30 FALLOW/EXTENSION programmes are
   reviewed one by one before migration (M4, WP-L12).
3. **For the licensing team (WP-L9): default commitment** for D&D wells (`CONDITIONAL`?) and for
   seismic/other items PEARS holds no commitment for. Or should LMS make commitment optional for
   migrated rows?
4. **Statuses with no LMS value:** `PART_WAIVER`, `WAIVER_DECLINED` and `DROPPED`. Add them to
   `WorkProgrammeStatus`, or map them with a comment?
   - **`DROPPED`** (466, mostly D&D wells) is common enough to need its own status. That is a
     design point; see M5. Put to the licensing team as WP-L8.
   - **The two waiver types are for the licensing team:** see Open question 12.
5. **Programme-level data:** legal text, breakpoint, well-count matrix and review audit. Which of
   these must LMS hold? Legal text and breakpoint feed licence documents. **General notes:
   resolved:** they go to the schedule comments area (M6, prerequisite A). **Review audit: decided, not
   migrated.** **Well-count matrix: decided, not migrated** (Open question 14). **Legal text: decided (23 November 2026), migrated separately, later.** This
   migration carries the activity descriptions only. LMS builds legal text first, then PEARS legal
   text is migrated in a piece of work of its own. WP-S13: legal text is on 926 programmes on
   numbered licences (34% of `P`, 25% of `PEDL`), so it needs a home in LMS.

   **Current plan (25 November 2026): one central legal text area covering all the work
   programme's activities**, not legal text per activity (WP-L10). Each activity keeps only its
   short description, migrated from the PEARS description as the LMS activity holds it today (M2).
   This was option (a). Option (b), a copy of the block on every activity, is not being pursued. It
   matches PEARS, which holds one HTML block per programme with no link to its activities, so:
   - the PEARS block would migrate once, unchanged, with its phase and activity-type headings, and
     never be split by matching text;
   - the licence document would use it as one block, as PEARS's mail-merge does today.

   LMS builds the area later; this plan is updated then ("Follow-up: once the legal text area is
   built"). Left for the legal text migration then: a licence with more than one programme (an INITIAL plus a
   `FALLOW` or `EXTENSION`, M4) can have a block on each, so the single LMS area either takes them
   in turn, each headed with its programme, or takes the INITIAL's only. The legal text design
   decides which.
6. **For the licensing team (WP-L7): version alignment, A (current only) or B (interleave)?** **Leaning
   A:** the current work programme on the latest (`ACTIVE`) schedule version only. **WP-S2c (live, 29 October 2026):**
   the 1,761 programmes have 3,857 real content states. Besides the current one, B would show
   1,161 past states (55%) on some schedule version, each dated to the next schedule save rather
   than the work programme save. The other 935 (45%) were replaced before any schedule save and
   would be lost. 851 programmes (48%) changed after the last schedule save, so under B the
   `ACTIVE` detail still has to take the current work programme as in A. **PEARS won't keep the
   history:** it is decommissioned some time after LMS goes live. So under A, past work programme
   content (earlier descriptions, titles and activities, and who changed them) is lost; only the
   status history and notes survive. Under B, 55% of past states survive, dated approximately.
7. **New licences:** PEARS creates the work programme at award and uses its phase timings to
   generate the template schedule. If LMS owns schedules for new production licences (schedule plan,
   "New production licences"), where is the work programme created? This also covers the 23 awards
   not yet raised (Open question 9, WP-L11).
8. **Licences with a work programme but no schedule (WP-S1, live 29 October 2026):** 2,437 `P` and
   340 `PEDL` licences have a work programme, but only 1,525 `P` and 220 `PEDL` have a schedule
   (schedule survey S2). The gap is roughly 900 `P` and 120 `PEDL`, plus a few onshore (`EXL` 37 vs
   12). LMS activities need a schedule to sit on. **WP-S10 (live, 29 October 2026): none is
   extant.** The 1,111 work-programme-only licences are 1,025 surrendered, 2 revoked and 84 with no
   current status, so they are left out and reported. The migration population is the 1,747
   licences with both (386 extant), and every extant licence has a schedule. WP-S10b: none of the
   107 with no current status has any data points. The 84 without a schedule are out of scope. The
   23 with one are all `DRAFT_FINAL` awards not yet raised (see 9). Separately, 34 extant licences
   (33 `P`, 1 `CE`) have a schedule but no work programme; that's not a migration issue.
9. **For the licensing team (WP-L1): `DRAFT_FINAL` programmes (WP-S1e, live 3 November
   2026).** 90 numbered licences (75 `P`, 15 `PEDL`) have a work programme still at `DRAFT_FINAL`, the status
   PEARS sets when an award is issued for raise (and on legacy import). WP-S1e (3 November 2026, 90
   licences) shows that 36 of the raised ones have been maintained and only kept the status because
   PEARS can't derive one from a dropped activity; they are all surrendered or expired. The other
   31 raised were never maintained and still have every activity `NOT_STARTED`, and **12 of those
   are extant** (11 awarded in 2023–24, plus PEDL279 from 2015). Should these be migrated as the
   licence's work programme, as they are (per licence on the "WP-L1 DRAFT_FINAL" sheet)?
   Or are they stale and need reviewing (or leaving out) first? See "Scope: programme statuses".
   The other 23 (18 `P`, 5 `PEDL`, WP-S10b) are on awards not yet
   raised, which have a template schedule but no licence data. Those 23 migrate only if the
   schedule migration creates an LMS licence for them; otherwise the programme is created in LMS
   at raise (see 7).
10. **For the licensing team (WP-L2): licences with two INITIAL programmes (WP-S1c, WP-S1d).** EXL253,
    PEDL120, PEDL133, PEDL180, PEDL181 and PEDL225 each have a hand-made programme and a one-activity
    `LEGACY` programme recording the same commitment. They disagree:
    - PEDL180's seismic figures differ;
    - PEDL225's `LEGACY` item is `DROPPED` while its hand-made items are `NOT_STARTED`;
    - PEDL180 and PEDL181 are `COMPLETE` vs `IN_PROGRESS`.

    Which is authoritative: the hand-made one, the `LEGACY` one, or neither (enter it fresh in LMS)?
    Proposed until they answer: neither is migrated, and the licences are reported.
11. ~~**For the licensing team (WP-L3): is term- or phase-level dating acceptable for migrated
    activities?**~~ **Resolved (25 November 2026): yes, as expected.** Activities migrate with the
    dating below (M3), and the team adds specific due dates in LMS afterwards where it wants them.
    PEARS activities have no dates (see M3). After migration:
    - **Phase level:** the 282 phase-organised `P` programmes (10%) sit `WITHIN_A_PHASE` and follow
      that phase's dates, extensions included.
    - **Term level:** the other 2,594 (90%) sit `WITHIN_A_TERM` on the Initial term. That is an
      assumption, not PEARS data, and it is wrong for licences with no Initial term (schedule
      patterns A and G), for the 30 FALLOW/EXTENSION programmes, and arguably for `LEGACY` summaries.
    - **A real date:** only drill-or-drop wells with a breakpoint get one (`RELATIVE_DATE`).
    - Timing written in descriptions ("DorD 3 yrs", "decision by end of 3rd year") stays as text.

    On the LMS timeline, most migrated activities would show against the end of the Initial term.
    Is that acceptable, or does the team want to add specific due dates after migration (and for
    which licences, e.g. extant only)?
12. **For the licensing team (WP-L4): the waiver statuses LMS doesn't have (WP-S3, WP-S3b).** PEARS has
    three waiver outcomes; LMS has only `FULL_WAIVER` (170 activities, mapped directly).

    | PEARS status | Meaning in PEARS | Activities (live) |
    |---|---|---|
    | `PART_WAIVER` | Part of the obligation was waived; the rest still applies | 16: contingent well 4, `LEGACY` 4, seismic 4, firm well 2, other 2 |
    | `WAIVER_DECLINED` | A waiver was asked for and refused; the obligation stands | 16: firm well 7, other 4, `LEGACY` 3, seismic 2 |

    All are on `P` or `PEDL`. **Only P1483 is on an extant licence**; the other 31 are surrendered
    (30) or expired (1), so for them the mapping only affects history (WP-S3b).

    **The notes show the PEARS status rarely means what it says** (WP-S3b):

    | What the notes record | Activities | LMS fit |
    |---|---|---|
    | Obligation **transferred to another licence** (e.g. P1973 → P2216, P1995 → P1774, P2130 → P1731) | 9 | **`TRANSFERRED`**, which LMS already has, with `licenceTransferredTo`. The target licence is free text in the note, so the team would supply it. |
    | Unmet obligation left on a surrendered licence (some carried by the licensee after relinquishment) | 8 | `OPEN`, with comment |
    | Genuine part waiver, or part of the obligation done | 7 | `IN_PROGRESS` / `COMPLETE`, with comment |
    | Status set in error or unclear (e.g. P1817 "pressed wrong button… complete") | 7 | Case by case |
    | Still live: P1483, well moved to another prospect on the same licence | 1 | `OPEN`, with comment |

    Questions:
    - Does LMS need part-waiver or waiver-declined as **statuses** going forward (new values in
      `WorkProgrammeStatus`)? Or are they only history?
    - For the 31 historical activities, accept a default mapping with the PEARS status and every
      PEARS note kept as comments? Proposed:
      - `PART_WAIVER` → `IN_PROGRESS`;
      - `WAIVER_DECLINED` → `OPEN`.
    - Should the 9 transfers migrate as `TRANSFERRED`? That needs the team to confirm each target
      licence.
    - What is P1483's current position?
13. **For the licensing team (WP-L5): the drill-or-drop decision shown twice (WP-S6b).** PEARS records the D&D decision date in
    two places: the schedule's `DD` event, and the work programme's D&D well activity, via the
    breakpoint. Both migrate to LMS: the `DD` event as an `OTHER_ACTIVITY` schedule event named
    after the PEARS type's short title (built, schedule plan), and the well as a `DRILL_OR_DROP_WELL` work programme activity. On 136
    licences both carry the date, so it would appear twice on the timeline. Options:
    - **A. Keep both.** The schedule event is the decision deadline; the activity is the well
      obligation, with its own status (`DROPPED`, `COMPLETE` …) and the same due date.
    - **B. Keep both, but don't date the activity.** Leave the D&D well on the term/phase default
      and let the schedule event carry the date.
    - **C. Merge** into the work programme activity, and don't migrate `DD` as a schedule event
      where a D&D well activity exists. This changes the schedule migration, which is now built
      and on `develop` (the mapper, the comparison report and its validation), and its write-back
      (`DD` is written back to PEARS).

    A or B keep the schedule migration unchanged. Under A, an extant licence with an open D&D well
    is also reminded twice about the same date: once for the `DD` schedule event and once for the
    activity (see "Event reminders after migration"). Licensing team and LMS design to decide.
14. ~~**For the licensing team (WP-L6): the well-count matrix on `LEGACY` licences (WP-S8).**~~
    **Resolved (25 November 2026): C, the matrix is not migrated.** The `LEGACY` summary text still
    carries the obligations; the counts stay in PEARS until it is decommissioned. 872 programmes count wells
    (obligated, completed, dropped, waived, waiver declined × firm/contingent/D&D) but have no well
    activities. The wells are described only in the `LEGACY` summary text. LMS has no matrix.
    Options:
    - **A. Schedule comment:** summarise the counts in one schedule comment per licence, e.g.
      "PEARS well counts: 2 firm obligated, 1 completed; 1 D&D obligated, 1 dropped".
    - **B. Generate well activities from the counts:** one `DRILL_WELL` / `DRILL_OR_DROP_WELL` per
      obligated well, with its status from the counts. This gives structured wells in LMS (the
      cross-licence tracker would see them), but they would duplicate the `LEGACY` summary and
      have no description of their own beyond "Well n of m".
    - **C. Don't migrate the matrix.** The `LEGACY` text still carries the obligation.

    A was proposed; C was chosen.
15. ~~**Work programme reminders at go-live.**~~ **Resolved (25 November 2026): acceptable**, they
    all go out on go-live. The first reminder run after the migration sends a
    `WORK_PROGRAMME_ACTIVITY` reminder for every open migrated activity on an extant licence that
    falls due in the next 6 months, some with less than the usual notice ("Event reminders after
    migration"). Because most activities default to the Initial term end (M3 rule 4), a licence
    near the end of its Initial term gets one email per open activity, and under Open question 13
    option A a D&D well is reminded on alongside its `DD` event. **Proposed:** the same as the
    schedule migration's decision (its Open question 15): let them all go out on go-live, with
    licence contacts filled in beforehand, and the volume counted in the UAT rehearsal (step 15)
    for awareness.

## Follow-up: once the legal text area is built

LMS will hold legal text in **one central area per licence work programme**, covering all its
activities, not per activity (D14, Open question 5). It is built later, as a feature of its own.
This migration doesn't wait for it: activities migrate with their short description only (M2), and
PEARS `LEGAL_TEXT` stays in PEARS, frozen and readable. **Once the area is built, update this plan**
(or write the legal text migration plan from it) to cover:

1. **Where it lives:** the LMS entity and table, and whether it is versioned with the schedule
   detail like the activities or held once per licence. That decides whether each PEARS block maps
   to the `ACTIVE` detail only (as Version alignment A) or to earlier versions too.
2. **Read:** add `LEGAL_TEXT` to `work-programmes.sql` (or a query of its own), as a CLOB, for the
   1,761 programmes in scope (926 on numbered licences hold some, WP-S13).
3. **Map:** the HTML block migrates whole, unchanged, with its phase and activity-type headings. Decide
   whether LMS stores HTML or needs it converted, and what happens to markup it doesn't support.
4. **Several programmes on one licence** (an INITIAL plus a `FALLOW` or `EXTENSION`, M4): each in
   turn under its programme's heading, or the INITIAL's only.
5. **Licence documents:** how they read the LMS area instead of PEARS `LEGAL_TEXT`
   (`LICENCE_CREATE` mail-merge, "Downstream consumers"), and new licences raised after go-live.
6. **Run:** a separate run on the same framework (a third `migration` value, its own link or
   marker so it is idempotent, cleardown, comparison and a validation dry run), or a step added to
   the work programme run if the area exists before that runs.
7. **Remove** legal text from "Out of scope" and close Open question 5.

## Out of scope

- Retention/development/pre-producing **area** work programmes (`ped_retention_areas.work_programme_xml`,
  `ped_work_programme_store`); the schedule plan already defers these to a later phase.
- The **application** work programme (`ped_licence_app_details`, LARRY) and the ESRI
  work-programme-blocks views.
- The free-text `LICENCE_DETAILS/WORK_PROGRAMME` licence operation attribute.
- Fixing the PEARS defects listed under "Findings in PEARS".
- **Legal text** (`LEGAL_TEXT`): migrated separately into one central area, once LMS has built it
  (D14, Open question 5, "Follow-up: once the legal text area is built").
- The **well-count matrix** (D12, Open question 14).

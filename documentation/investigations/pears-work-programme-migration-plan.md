# Investigation: Migrating PEARS Licence Work Programmes into LMS

> **Status: First pass (28 October 2026), survey complete (29 October 2026; see the survey's
> Summary).** Nothing is
> implemented. Decided so far: `DRAFT` programmes are excluded, the review audit is not migrated,
> migrated comments carry the PEARS author's name in the text with no WUA id, and the work
> programme is **not written back to PEARS**. This compares
> how PEARS holds a **licence work programme** with the LMS `WorkProgrammeActivity` model, and lists
> the differences a migration would have to resolve. It follows the format of
> [pears-schedule-migration-plan.md](pears-schedule-migration-plan.md), which lists LMS work
> programme activities as out of scope and is the base this builds on. The live data survey is
> under way: the queries and results are in
> [pears-work-programme-migration-data-survey.md](pears-work-programme-migration-data-survey.md).
>
> This covers the **licence** work programme only (`pedmgr.ped_work_programme` /
> `ped_work_programme_details`, Fox `PED009X`). It does not cover the retention/development area work
> programme (`ped_retention_areas.work_programme_xml`, `ped_work_programme_store`, `PED031L`/`PED033X`),
> which the schedule plan defers to a later phase, or the application work programme in
> `ped_licence_app_details` (LARRY).

## Problem

PEARS holds each licence's committed work (wells, seismic, other activities) in a versioned XML
document, maintained in Fox `PED009X`. LMS holds the same kind of information as
`WorkProgrammeActivity` schedule events. The schedule migration plan does not migrate any of this.
Once the schedule is migrated, the LMS schedule for a PEARS licence has terms, phases, rates and
other events, but no work programme.

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

  So `DRAFT_FINAL` on a raised licence (76 `P`, 15 `PEDL`) means **nobody has saved it in maintain
  since raise**, and its activities still hold their award-time statuses. The programme status isn't
  migrated (D11), so technically these can migrate like any other. Whether they *should* is for the
  licensing team. The 5 `DRAFT_FINAL` programmes with **no
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
- **Schedule comments (planned, not built yet):** a general comments area above the main schedule,
  for comments that belong to the schedule as a whole rather than to one event. It is designed in
  the LMS mockup `mockups/schedule/LicenceScheduleMockupController` (template
  `lms/mockups/schedule/licenceScheduleTimeline.ftl`, macro `scheduleCommentsSection`).
  - Each comment has text, author, date/time and a remove link.
  - A comment can also carry an optional **linked licence** (`ScheduleCommentView.linkedLicenceReference`
    + URL), shown as a "View licence …" link.
  - One mockup example is exactly a PEARS transfer case: "Alternative work programme for drill one
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
| D10 | Notes | `NOTE` per activity or `GENERAL` (programme-level). Activity notes often point to a general one ("See General Note below", WP-S3b) | `EventComment` per event, plus the planned **schedule comments** area (mockup) | Activity notes → event comments; general notes → schedule comments (M6). |
| D11 | Programme status | `DRAFT` … `COMPLETE`, derived on save | None | Not migrated; LMS can derive it from the activities if needed. |
| D12 | Well-count matrix | 5 × 3 counts (obligated/completed/dropped/waived/waiver declined × firm/contingent/D&D), maintained **by hand**, separate from the activities | None | No home. **WP-S8:** where well activities exist, the matrix agrees with them on all but about 30 programmes, so it adds nothing there. But **872 programmes (844 `P`, 28 `PEDL`) count wells with no well activity at all**, almost certainly because the wells are inside a `LEGACY` summary. For those, the matrix is the only structured record of the well obligations and their outcomes. Open question 14. |
| D13 | Breakpoint | Y/M/D from licence start; feeds the documents' `DRILL_DROP_PERIOD`. On only 233 programmes, all `P`/`PEDL` INITIAL, and nearly all whole years (WP-S6) | None (a D&D well could have a `RELATIVE_DATE`) | Due date for D&D wells on 228 programmes (M3 rule 2). The 5 with no D&D activity go to a schedule comment. |
| D14 | Legal text | HTML merged into the licence documents (`LICENCE_CREATE` mail-merge) | None | Documents for migrated licences need a source (see "Downstream consumers"). WP-S13 (live): 927 programmes hold legal text (838 `P`, 88 `PEDL`), so it can't be dropped. LMS plans legal text per activity, but PEARS holds one HTML block per programme: generated from the activity descriptions ("Populate legal text"), then freely edited, with no link back to activities. **Not split by matching text.** Options: (a) one programme-level legal text area, like the general notes, or (b) copy the whole block to every activity (Q5). |
| D15 | Phase and term timings | Held in the work programme **and** in the schedule | The schedule only | Don't migrate them as data. **WP-S7:** 450 of 591 work programme phases (76%) end where the schedule's phase does. The other 140 were extended on the schedule only, so the work programme `TIMINGS` are stale award-time values. No work programme phase is missing from its schedule. |
| D16 | Review audit | `REVIEW_AUDIT_LIST` acknowledgements | None | **Decided: not migrated.** It's a copy of advisor sign-offs from the generic PEARS review process (`REVIEW_LIB`, `bpmmgr.review_advisor_items`): date, user and flag only, nothing specific to the work programme. WP-S13 (live): 5,383 entries on 1,590 programmes, the latest acknowledged 29 October 2026. |
| D17 | Activity identity | `ACTIVITY_ID`, stable across versions | `originalEventId` | Easier than the schedule migration: match on `ACTIVITY_ID`. WP-S12 (live): always set, unique within a version and across programmes, and its type never changes (43,388 activity rows). |
| D18 | Transfer | No status, but obligations **are** transferred: the notes record at least 9 (WP-S3b), as free text naming the target licence | `TRANSFERRED` + `licenceTransferredTo` | Candidates for `TRANSFERRED`, but the target licence must be supplied by hand (Q12). WP-S9 can search all notes for transfers. |
| D19 | Soft delete | `record_status = 'DELETED'` with `status_control = 'C'` still set | Detail status `DELETED` | Exclude deleted programmes. WP-S11 (live): none exist today; every programme has exactly one current row. |
| D20 | Comment author | Note `CREATED_BY` is a full name only. Each version's `created_by` is the saver's WUA id | `EventComment.authorWuaId`; with none, the author shows as "Unknown" | **Decided:** the author's name goes in the comment text and `authorWuaId` is left empty, for event comments and schedule comments. **LMS change needed:** a comment with no author currently shows "Unknown" (`EventCommentService.createViewFrom`). It should show a blank author instead, to avoid confusion. |
| D21 | Edit history | Every save is a full version with its author (`created_by`, `created_datetime`), back to 2010 | Envers audit from migration onwards; `LicenceScheduleDetail` has `createdInstant` only | Under Version alignment A only the current version migrates, so earlier descriptions, titles and who changed them stay in PEARS. Status history and notes still carry over (M5, M6). To keep history, option B matches by time to the schedule versions; WP-S2c measures how much it keeps. |
| D22 | Data LMS has and PEARS doesn't | — | `CONDITIONAL` commitment, `RELATIVE_DATE`, `WELL_TEST` and `WELL_INVESTMENT_ENGAGEMENT` categories, `licenceTransferredTo`, per-activity legal text (planned) | Nothing to migrate into them, apart from D&D wells (`RELATIVE_DATE`, M3) and confirmed transfers (D18). They are filled going forward. |

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
| `SEISMIC` (flat list) | **By title** when it is a standard `PED009X` title ("Shoot 3D Seismic Data n" etc.): 518 of 663. **By description keywords** for the 145 generic "Seismic Data n" (WP-S4). **Tested (WP-S4b):** 144 of 145 classify. | **default?** | The one left is a seismic inversion → `OTHER_ACTIVITY` "Seismic inversion". 82 (12%) describe two operations (mostly obtain + reprocess). They migrate as one activity under the title's category, or "reprocess" for generic titles, and are reported. One title has a pasted Fox `javascript:ss('action-add-new-3d-seismic',…)` link; strip it and report it. |
| `LEGACY` | `OTHER_ACTIVITY`, `otherCategoryName` = "Legacy work programme" | **default?** | Probably created in bulk on 12/07/2011, one per pre-existing licence (WP-S1c), so possibly a large group (WP-S3 will size it). Onshore `LEGACY` activities have **no `TITLE`** (WP-S1d); on `P` they do, e.g. "Legacy Obligations: Original blocks 20/3f. Competed 1" (WP-S3b). The description is a **shorthand summary of the whole commitment**: onshore e.g. "DorD 3 yrs 1500m 30km new 2D O 500km 2D R 70km 2D", offshore e.g. "(T) D/D to 3505m to the shallower of Ettrick or Buzzard sst.". It spans several LMS categories and is too loose to split by rule, so it migrates as one activity, description as is. Where a title exists, prefix it to the description (M2). |
| `OTHER` | `OTHER_ACTIVITY`, `otherCategoryName` = a **fixed name**, e.g. "Other work programme activity" | **default?** | Not `TITLE`: 939 of 945 (99%) keep the default "Other Activity n" (WP-S4). The 6 hand-typed titles ("Geological Studies", "Rock Physics Study", "Seismic Inversion" and 3 others) could be used as the name instead. One ("Other Activity 3 - CONTINGENT COMMITMENT") is the only `OTHER` that states a commitment. |
| Anything else | — | — | Fail the run, as the schedule migration does for unknown event types. |

### M2. Description

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

   **Timeline duplication:** the schedule migration also brings each `DD` event in as an
   `OTHER_ACTIVITY` "Drill-or-drop" schedule event. With this rule, 136 licences would show the
   drill-or-drop decision twice on the same date: once as the D&D well activity and once as the
   schedule event. See Open question 13.
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
5. A schedule with no Initial term (schedule-plan patterns A and F, which start at the Second
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

**Everything else** goes to the planned **schedule comments** area above the schedule (see Target):
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
programme on the latest (`ACTIVE`) schedule version only (WP-S2, WP-S2c, live 29 October 2026). There are 17,416 work programme versions
(15,285 on `P`, up to 61 on one programme), against 10,834 schedule versions. Many are not content
changes: a review acknowledgement re-versions every programme on the licence, while status changes
through `set_work_programme_status` create no version at all. The status history migrates from
the notes (M5) instead.

**B is a point-in-time lookup.** Both tables have `start_datetime`, and each migrated schedule
version keeps its PEARS `start_datetime` as `createdInstant`, so B is mechanical. The review-only
saves don't hurt it: B just picks whichever version was current, and identical content repeats.
What B can't show is:
- a work programme change superseded before the next schedule save. It's in PEARS, but no LMS
  version holds it;
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
`ped_work_programme_details` rows stay as they were at migration, as a frozen historical record.

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

The queries are in
[pears-work-programme-migration-data-survey.md](pears-work-programme-migration-data-survey.md). None
has been run yet. Each item feeds these parts of the plan:

| # | Survey item | Feeds |
|---|---|---|
| WP-S1 | Programmes by licence type, programme type and status | D2, M4, Q2 |
| WP-S2 | Versions per programme; flat vs phase organisation | D1, Version alignment, Q6 |
| WP-S3 | Activities by type, status and list | D4, D8, M1, M5 |
| WP-S4 | Titles of `SEISMIC`, `LEGACY` and `OTHER` activities; proposed seismic classification | D5, D6, M1 |
| WP-S5 | Descriptions: empty, template text, competed blocks | D6, D7, M2 |
| WP-S6 | Breakpoints and drill-or-drop wells; breakpoint vs the schedule's `DD` event | D13, M3 |
| WP-S7 | Phase timings vs the current schedule's phases | D15, M3 |
| WP-S8 | Well-count matrix vs activities | D12, Q5 |
| WP-S9 | Notes: status history and general notes | D9, D10, M5, M6, Q5 |
| WP-S10 | Work programme vs schedule coverage, by licence status | Scope |
| WP-S10b | Work programme licences with no current status | Scope |
| WP-S11 | Soft-deleted and multiple-current rows | D19 |
| WP-S12 | `ACTIVITY_ID` uniqueness and stability across versions | D17 |
| WP-S13 | Legal text and review audit | D14, D16, Q5 |

## Open questions

1. ~~**Ownership:** does LMS become the owner of migrated licences' work programmes, or is this a
   read-only copy?~~ **Resolved (29 October 2026):** LMS owns them, with no write-back to PEARS.
   See "Decision: no write-back to PEARS". The PEARS link redirects to LMS, the data warehouse
   doesn't report on work programmes, and migration and lockdown happen together, so nothing is
   left open.
2. **Term for flat-list and FALLOW/EXTENSION activities:** default `WITHIN_A_TERM` Initial, or
   something else?
3. **Default commitment** for D&D wells (`CONDITIONAL`?) and for seismic/other items PEARS holds no
   commitment for. Or should LMS make commitment optional for migrated rows?
4. **Statuses with no LMS value:** `PART_WAIVER`, `WAIVER_DECLINED` and `DROPPED`. Add them to
   `WorkProgrammeStatus`, or map them with a comment?
   - **`DROPPED`** (466, mostly D&D wells) is common enough to need its own status. That is a
     design point; see M5.
   - **The two waiver types are for the licensing team:** see Open question 12.
5. **Programme-level data:** legal text, breakpoint, well-count matrix and review audit. Which of
   these must LMS hold? Legal text and breakpoint feed licence documents. **General notes:
   resolved:** they go to the planned schedule comments area (M6). **Review audit: decided, not
   migrated.** WP-S13: legal text is on 927 programmes (34% of `P`, 25% of `PEDL`), so it needs a
   home in LMS. It won't be split per activity by matching the text. **Legal text: decide between:**
   - **(a) A programme-level legal text area**, like the general notes area above the schedule. The
     block migrates once, unchanged, with its phase and activity-type headings, and the licence
     document carries on using it as one block. The LMS per-activity legal text field is left empty
     on migrated activities and used going forward.
   - **(b) Copy the whole block to every activity.** It fits the per-activity model, but each
     activity's legal text would describe the whole programme, not that activity, with one copy
     per activity. A document built from per-activity legal text would repeat it
     unless it is de-duplicated, and an edit to one copy leaves the others out of step.

   Both options are open. Either way, a licence with more than one programme (an INITIAL plus a
   `FALLOW` or `EXTENSION`) can have a separate block on each:
   - **(a)** needs to hold one block per programme, or label which programme each came from.
   - **(b)** copies each programme's block only to that programme's activities, so `FALLOW` and
     `EXTENSION` activities carry their own programme's text, not the INITIAL's (see M4 for where
     those activities land).
6. **For the licensing team: version alignment, A (current only) or B (interleave)?** **Leaning
   A:** the current work programme on the latest (`ACTIVE`) schedule version only. **WP-S2c (live, 29 October 2026):**
   the 1,761 programmes have 3,857 real content states. Besides the current one, B would show
   1,161 past states (55%) on some schedule version, each dated to the next schedule save rather
   than the work programme save. The other 935 (45%) were replaced before any schedule save and
   would be lost. 851 programmes (48%) changed after the last schedule save, so under B the
   `ACTIVE` detail still has to take the current work programme as in A. PEARS keeps the full
   history either way.
7. **New licences:** PEARS creates the work programme at award and uses its phase timings to
   generate the template schedule. If LMS owns schedules for new production licences (schedule plan,
   "New production licences"), where is the work programme created?
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
9. **For the licensing team: `DRAFT_FINAL` programmes on raised licences (WP-S1, live 29 October
   2026).** 76 `P` and 15 `PEDL` licences have a work programme still at `DRAFT_FINAL`, the status
   PEARS sets when an award is issued for raise (and on legacy import). It means nobody has saved
   the programme in `PED009X` maintain since the licence was raised, so its activity statuses may
   never have been updated. Should these be migrated as the licence's work programme, as they are?
   Or are they stale and need reviewing (or leaving out) first? See "Scope: programme statuses".
   WP-S10b splits the 91 into 68 on raised licences and 23 (18 `P`, 5 `PEDL`) on awards not yet
   raised, which have a template schedule but no licence data. Those 23 migrate only if the
   schedule migration creates an LMS licence for them; otherwise the programme is created in LMS
   at raise (see 7).
10. **For the licensing team: licences with two INITIAL programmes (WP-S1c, WP-S1d).** EXL253,
    PEDL120, PEDL133, PEDL180, PEDL181 and PEDL225 each have a hand-made programme and a one-activity
    `LEGACY` programme recording the same commitment. They disagree:
    - PEDL180's seismic figures differ;
    - PEDL225's `LEGACY` item is `DROPPED` while its hand-made items are `NOT_STARTED`;
    - PEDL180 and PEDL181 are `COMPLETE` vs `IN_PROGRESS`.

    Which is authoritative: the hand-made one, the `LEGACY` one, or neither (enter it fresh in LMS)?
    Proposed until they answer: neither is migrated, and the licences are reported.
11. **For the licensing team: is term- or phase-level dating acceptable for migrated activities?**
    PEARS activities have no dates (see M3). After migration:
    - **Phase level:** the 282 phase-organised `P` programmes (10%) sit `WITHIN_A_PHASE` and follow
      that phase's dates, extensions included.
    - **Term level:** the other 2,594 (90%) sit `WITHIN_A_TERM` on the Initial term. That is an
      assumption, not PEARS data, and it is wrong for licences with no Initial term (schedule
      pattern A), for the 30 FALLOW/EXTENSION programmes, and arguably for `LEGACY` summaries.
    - **A real date:** only drill-or-drop wells with a breakpoint get one (`RELATIVE_DATE`).
    - Timing written in descriptions ("DorD 3 yrs", "decision by end of 3rd year") stays as text.

    On the LMS timeline, most migrated activities would show against the end of the Initial term.
    Is that acceptable, or does the team want to add specific due dates after migration (and for
    which licences, e.g. extant only)?
12. **For the licensing team: the waiver statuses LMS doesn't have (WP-S3, WP-S3b).** PEARS has
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
13. **The drill-or-drop decision shown twice (WP-S6b).** PEARS records the D&D decision date in
    two places: the schedule's `DD` event, and the work programme's D&D well activity, via the
    breakpoint. Both migrate to LMS: the `DD` event as an `OTHER_ACTIVITY` "Drill-or-drop" schedule
    event (schedule plan), and the well as a `DRILL_OR_DROP_WELL` work programme activity. On 136
    licences both carry the date, so it would appear twice on the timeline. Options:
    - **A. Keep both.** The schedule event is the decision deadline; the activity is the well
      obligation, with its own status (`DROPPED`, `COMPLETE` …) and the same due date.
    - **B. Keep both, but don't date the activity.** Leave the D&D well on the term/phase default
      and let the schedule event carry the date.
    - **C. Merge** into the work programme activity, and don't migrate `DD` as a schedule event
      where a D&D well activity exists. This changes the schedule migration and its write-back
      (`DD` is written back to PEARS).

    A or B keep the schedule migration unchanged. Licensing team and LMS design to decide.
14. **For the licensing team: the well-count matrix on `LEGACY` licences (WP-S8, WP-L6).** 872 programmes count wells
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

    Proposed: **A**. It is cheap, loses nothing, and creates no duplicates. WP-S8b shows what the
    counts hold, to inform the choice.

## Out of scope

- Retention/development/pre-producing **area** work programmes (`ped_retention_areas.work_programme_xml`,
  `ped_work_programme_store`); the schedule plan already defers these to a later phase.
- The **application** work programme (`ped_licence_app_details`, LARRY) and the ESRI
  work-programme-blocks views.
- The free-text `LICENCE_DETAILS/WORK_PROGRAMME` licence operation attribute.
- Fixing the PEARS defects listed under "Findings in PEARS".

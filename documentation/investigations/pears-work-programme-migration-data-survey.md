# PEARS Work Programme Migration: Data Survey

> Companion to [pears-work-programme-migration-plan.md](pears-work-programme-migration-plan.md).
> This document holds the read-only survey queries to run against PEARS, and their results once
> they have been run. The plan holds the decisions that come from them, and links here for the
> evidence. Same conventions as
> [pears-schedule-migration-data-survey.md](pears-schedule-migration-data-survey.md).

## Conventions

- **Run against live**, read-only, unless a result says otherwise. Record the date each was run.
- **Licence work programme only:** `pedmgr.ped_work_programme` (header) and
  `pedmgr.ped_work_programme_details` (one row per save, `xml_data` root `WORK_PROGRAMME`). Not the
  retention-area work programme or the application work programme.
- **"Current"** means the details row with `status_control = 'C'` **and** `record_status = 'ACTIVE'`.
  `delete_work_programme` leaves `status_control = 'C'` on soft-deleted rows, so both filters are
  needed (WP-S11 counts the difference). "All versions" means every `ACTIVE` row, history included.
- **`DRAFT` programmes are excluded** (decided 29 October 2026, plan "Scope: programme statuses").
  From WP-S1b onwards, every query that reads current versions also filters out
  `WORK_PROGRAMME_STATUS = 'DRAFT'` (the `... != 'DRAFT'` line after the `record_status` filter).
  WP-S2, WP-S11 and WP-S12 count every version, so they aren't filtered; the `DRAFT` rows there are
  all on unnumbered awards. WP-S1 was run
  before this decision and includes the 8 `DRAFT` rows. `DRAFT_FINAL` stays in the queries, so its
  numbers stay visible. Whether it is migrated is a question for the licensing team (WP-L1).
- **Activities** come from two lists, and every query reads both:
  - the flat `/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY` ("general organisation" rounds);
  - `/WORK_PROGRAMME/PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY` ("phase organisation" rounds).

  `list_kind` is `FLAT` or `PHASE`.
- **Licence status** is the same lookup as the schedule survey's S8d: the latest live data point
  (`ped_data_points`, `ped_sim_id = 0`) through `xview_ped_ld_current.licence_status`. The codes:
  `E` Extant, `S` Surrendered, `C` Conditional, `R` Revoked, `P` Provisional, `X` Expired.
- The queries were written from the XSD
  (`DatabaseSource/Viewify/PEDMGR/Schemas/PED_WORK_PROGRAMME_DETAILS.xsd`) and `PED009X`, and
  have all been run on live as recorded below (fixes made along the way are in the queries).
- `TO_NUMBER(... DEFAULT NULL ON CONVERSION ERROR)` needs Oracle 12.2 or later. Numeric XML fields
  can hold empty or template text.

## Status

| # | Survey item | Feeds plan | Status | Section |
|---|---|---|---|---|
| WP-S1 | Programmes by licence type, programme type and status | D2, M4, Q2 | ✅ Live, 29 Oct 2026 (with WP-S1b, WP-S1c, WP-S1d) | [WP-S1](#wp-s1-programmes-by-licence-type-programme-type-and-status) |
| WP-S2 | Versions per programme, and flat vs phase organisation | D1, Version alignment, Q6 | ✅ Live, 29 Oct 2026 (with WP-S2b) | [WP-S2](#wp-s2-versions-and-organisation) |
| WP-S2c | Work programme changes vs schedule versions (option B) | D21, Version alignment | ✅ Live, 29 Oct 2026 | [WP-S2c](#wp-s2c-work-programme-changes-vs-schedule-versions-option-b) |
| WP-S3 | Activities by type, status and list | D4, D8, M1, M5 | ✅ Live, 29 Oct 2026 (with WP-S3b) | [WP-S3](#wp-s3-activities-by-type-status-and-list) |
| WP-S4 | Titles of `SEISMIC`, `LEGACY` and `OTHER` activities | D5, D6, M1 | ✅ Live, 29 Oct 2026 (with WP-S4b; WP-S4c not run) | [WP-S4](#wp-s4-titles-of-seismic-legacy-and-other-activities) |
| WP-S5 | Descriptions: empty, template text, competed blocks, "None", phase prefixes | D6, D7, M2, M3 | ✅ Live, 29 Oct 2026 (with WP-S5b) | [WP-S5](#wp-s5-descriptions) |
| WP-S6 | Breakpoints and drill-or-drop wells | D13, M3 | ✅ Live, 29 Oct 2026 (with WP-S6b) | [WP-S6](#wp-s6-breakpoints-and-drill-or-drop-wells) |
| WP-S7 | Phase timings vs the current schedule's phases | D15, M3 | ✅ Live, 29 Oct 2026 | [WP-S7](#wp-s7-phase-timings-vs-the-schedule) |
| WP-S8 | Well-count matrix vs activities | D12, Q5 | ✅ Live, 29 Oct 2026 (WP-S8b not run) | [WP-S8](#wp-s8-well-count-matrix-vs-activities) |
| WP-S9 | Notes: status history, general notes, destination in LMS (WP-S9c), boilerplate (WP-S9d) | D9, D10, M5, M6, Q5 | ✅ Live, 29 Oct 2026 (with WP-S9b–WP-S9f) | [WP-S9](#wp-s9-notes) |
| WP-S10 | Work programme vs schedule coverage, by status | Scope | ✅ Live, 29 Oct 2026 | [WP-S10](#wp-s10-work-programme-vs-schedule-coverage) |
| WP-S10b | Work programme licences with no current status | Scope | ✅ Live, 29 Oct 2026 | [WP-S10b](#wp-s10b-licences-with-a-work-programme-but-no-current-status) |
| WP-S11 | Soft-deleted and multiple-current rows | D19 | ✅ Live, 29 Oct 2026 | [WP-S11](#wp-s11-soft-deleted-and-multiple-current-rows) |
| WP-S12 | `ACTIVITY_ID` uniqueness and stability across versions | D17 | ✅ Live, 29 Oct 2026 | [WP-S12](#wp-s12-activity_id-uniqueness-and-stability) |
| WP-S13 | Legal text and review audit | D14, D16, Q5 | ✅ Live, 29 Oct 2026 | [WP-S13](#wp-s13-legal-text-and-review-audit) |

## Summary

All queries were run on live on 29 October 2026, apart from WP-S4c (optional) and WP-S8b (left
for the licensing team).

**Scope**
- **1,747 licences migrate:** they have both a work programme and a PEARS schedule. That's `P`
  1,474, `PEDL` 220, `PL` 26, `EXL` 12, `ML` 7, `DL` 5 and `AL` 3, and 386 of them are extant
  (WP-S10). Every extant licence has a schedule.
- **1,111 licences with a work programme but no schedule are left out and reported.** None is
  extant: 1,025 surrendered, 2 revoked and 84 with no licence data at all (WP-S10, WP-S10b).
- **23 unraised awards** (18 `P`, 5 `PEDL`) have a `DRAFT_FINAL` programme and a template schedule
  but no licence data yet. They migrate only if the schedule migration creates an LMS licence for
  them (WP-S10b, plan Q7, Q9).
- **Excluded:** the 8 `DRAFT` programmes, all on unnumbered awards, and the 6 licences with
  duplicate INITIAL programmes until the licensing team chooses (WP-S1c, WP-L2). The `A`, `B`,
  `NA` and `XL` licence types are bare master rows with no data (WP-S10).

**Data quality: better than the schedule**
- Every programme has exactly one current row, and there are no soft-deleted rows (WP-S11).
- `ACTIVITY_ID` is always set, unique, never shared between programmes, and never changes type
  across 43,388 activity rows. Activities and notes can be matched on it safely (WP-S12).
- Every status note's activity still exists (WP-S9c).

**Activities** (4,787 current: WP-S3)
- **Category:** `SEISMIC` is classified from the title (518) or, failing that, the description
  (144 of the other 145). `LEGACY` and `OTHER` become `OTHER_ACTIVITY` with a fixed name, and 8
  "None" activities are skipped (WP-S4, WP-S5, plan M1).
- **Dates:** PEARS activities have none. 90% of programmes are flat lists, so their activities
  can only be placed within the Initial term. Phase-organised programmes always have a matching
  schedule phase (WP-S7), 108 flat activities name a phase in their description (WP-S5), and D&D
  wells use the schedule's `DD` event or the breakpoint (WP-S6, WP-S6b). The licensing team is
  asked whether this is acceptable (WP-L3, WP-L5).
- **Statuses:** `DROPPED` (466, 10%) has no LMS status and needs one. `PART_WAIVER` and
  `WAIVER_DECLINED` (16 each, only P1483 extant) go to the licensing team (WP-S3, WP-S3b, WP-L4).
- **Status history** is complete in the notes, except for 758 `LEGACY` statuses set by the
  12/07/2011 bulk job. Those are dated to that day and carry an explanatory comment (WP-S9b,
  WP-S9e).

**Notes**
- Of 4,973 notes, 3,680 migrate: 2,883 status notes become event comments and 797 general notes
  go to the planned schedule comments area. The 1,293 notes written by the bulk job ("Migration
  Process", the only system author) are excluded (WP-S9–WP-S9f).
- Authors are kept as names in the comment text, with no WUA id (decided).

**Programme-level data**
- **Legal text** is on 927 programmes (34% of `P`, 25% of `PEDL`) and feeds the licence documents,
  so it can't be dropped. It's one block per programme. Either it goes into a programme-level area
  like the general notes, or it's copied to every activity (WP-S13, plan Q5).
- **Review audit:** in active use but not migrated (decided). It's a copy of advisor sign-offs from
  the PEARS review process (WP-S13).
- **Well counts** on 872 `LEGACY` programmes have no matching well activities. How LMS holds them
  is for the licensing team (WP-S8, WP-L6).
- **Breakpoint** on 233 programmes: D&D wells are dated from the schedule's `DD` event where there
  is one (136) and from the breakpoint otherwise (97). The 5 with no D&D well become a schedule
  comment (WP-S6, WP-S6b).

**History**
- The 17,416 versions hold only 3,857 real content states, as most saves are review-only (WP-S2,
  WP-S2c). Matching them to past schedule versions (option B) would keep 55% of past states,
  each dated to the next schedule save. The plan leans towards current only on the latest schedule
  version, for the licensing team to confirm (WP-L7).

**Open before the migration can be built**
- The licensing team questions WP-L1 to WP-L11, sent as
  [pears-work-programme-migration-licensing-review.docx](pears-work-programme-migration-licensing-review.docx).
- LMS changes:
  - a status for `DROPPED`;
  - the schedule comments area;
  - a home for legal text;
  - a blank author, not "Unknown", on migrated comments.

## For the licensing team

Items that need a confirmation or decision from the licensing team. Each links to its evidence.
They are put to the team in
[pears-work-programme-migration-licensing-review.docx](pears-work-programme-migration-licensing-review.docx),
which numbers them differently (the "Review" column).

| # | Question | Licences | Background | Section | Review |
|---|---|---|---|---|---|
| WP-L1 | **`DRAFT_FINAL` work programmes on raised licences: migrate as they are, or review first?** | 91 licences (76 `P`, 15 `PEDL`): 68 raised, 23 not yet raised (WP-L11) | PEARS sets `DRAFT_FINAL` when an award is issued for raise (and on legacy import). Only a save in `PED009X` maintain moves it on, so these programmes haven't been saved since raise, and their activity statuses may never have been updated. | [WP-S1](#wp-s1-programmes-by-licence-type-programme-type-and-status); plan Open question 9 | 1.1 |
| WP-L2 | **Two INITIAL programmes on one licence: which is authoritative?** | EXL253, PEDL120, PEDL133, PEDL180, PEDL181, PEDL225 | Each has a hand-made programme (2010–11) and a one-activity `LEGACY` programme from a 12/07/2011 bulk job. Both describe the same commitment, but they disagree on figures (PEDL180) or statuses (PEDL225 `DROPPED` vs `NOT_STARTED`; PEDL180/181 `COMPLETE` vs `IN_PROGRESS`). Options: keep the hand-made one, keep the `LEGACY` one, or enter it fresh in LMS. Proposed until they answer: neither is migrated, and the licences are reported. | [WP-S1c](#wp-s1c-the-duplicate-initial-programmes-side-by-side), [WP-S1d](#wp-s1d-the-activities-on-the-duplicate-programme-licences); plan Open question 10 | 1.2, 2.3 |
| WP-L3 | **Is term- or phase-level dating acceptable for migrated activities?** | All 2,876 numbered programmes | PEARS activities have no dates. The 282 phase-organised `P` programmes can sit within their phase. The other 2,594 (90%) can only be placed within the Initial term, which is an assumption (wrong for licences with no Initial term and for FALLOW/EXTENSION). Only D&D wells with a breakpoint get a real date. Most migrated activities would show against the end of the Initial term on the LMS timeline. Accept that, or add specific dates after migration (for which licences)? | [WP-S2b](#wp-s2b-flat-vs-phase-organisation-current-versions); plan M3, Open question 11 | 1.3 |
| WP-L4 | **Waiver statuses LMS doesn't have: `PART_WAIVER` and `WAIVER_DECLINED`** | 32 activities on `P`/`PEDL` (16 each). **Only P1483 is extant**; the other 31 are surrendered or expired. | LMS has only `FULL_WAIVER`. The notes show the PEARS status rarely means what it says. Of the 32: 9 are obligations **transferred** to another licence (LMS has `TRANSFERRED`), 8 are unmet obligations left on surrendered licences, 7 are genuine part waivers or part done, 7 are errors or unclear, and 1 (P1483) still stands. Questions: (1) Does LMS need part-waiver / waiver-declined as statuses going forward? (2) For the 31 historical ones, accept a default mapping, with the PEARS status and notes kept as comments? (3) Should transfers migrate as `TRANSFERRED`, with the team supplying the target licence? (4) What is P1483's current position? | [WP-S3](#wp-s3-activities-by-type-status-and-list), [WP-S3b](#wp-s3b-activities-with-a-waiver-status-lms-doesnt-have); plan Open question 12 | 1.5 |
| WP-L5 | **Drill-or-drop decision shown twice** | 136 licences with both a breakpoint-dated D&D well and a schedule `DD` event | Keep both (the schedule event as the deadline, the activity as the obligation), keep both with the activity undated, or merge? 17 of the 136 currently disagree, and the schedule date is always later. | [WP-S6b](#wp-s6b-breakpoint-vs-the-schedules-dd-event); plan M3 rule 2, Open question 13 | 1.4 |
| WP-L6 | **Well counts on `LEGACY` licences: how should LMS hold them?** | 872 programmes (844 `P`, 28 `PEDL`) | These count wells (obligated, completed, dropped, waived, waiver declined × firm/contingent/D&D) but have no well activities; the wells are only in the `LEGACY` summary text. LMS has no well-count table. Options: (A) one schedule comment per licence summarising the counts (proposed); (B) generate a well activity per obligated well; (C) don't migrate them. WP-S8b gives the full counts if the team wants to see them first. | [WP-S8](#wp-s8-well-count-matrix-vs-activities), [WP-S8b](#wp-s8b-well-counts-on-programmes-with-no-well-activities); plan Open question 14 | 1.8 |
| WP-L7 | **Work programme history: current only, or matched to past schedule versions?** | 1,761 programmes on licences with a schedule | **Leaning A:** migrate the current work programme onto the latest (`ACTIVE`) schedule version only; earlier LMS schedule versions show no work programme. PEARS is decommissioned some time after LMS goes live and its history isn't kept, so past work programme content is lost. B would attach the work programme current at each past schedule save, but keeps only 1,161 of 2,096 past content states (55%), each dated to the next schedule save rather than the work programme edit. Status history and notes migrate either way. | [WP-S2c](#wp-s2c-work-programme-changes-vs-schedule-versions-option-b); plan Version alignment, Open question 6 | 1.10 |
| WP-L8 | **Should LMS have a `DROPPED` status?** | 466 activities (10%): 411 D&D wells, 55 `LEGACY` | LMS has `OPEN`, `IN_PROGRESS`, `COMPLETE`, `FULL_WAIVER` and `TRANSFERRED`. `DROPPED` is the normal outcome for 411 of the 890 D&D wells, so it is too common to hold only as a comment. Proposed: add it to LMS. | [WP-S3](#wp-s3-activities-by-type-status-and-list); plan M5, Open question 4 | 1.6 |
| WP-L9 | **What commitment should migrated activities have where PEARS records none?** | 4,541 activities: 890 D&D wells and 3,651 others (76%) | LMS requires `FIRM`, `CONTINGENT` or `CONDITIONAL`. PEARS records a commitment only for wells and new-shoot seismic (246 activities). `CONDITIONAL` fits D&D wells. Options: a default per type, or make commitment optional for migrated activities. | [WP-S3](#wp-s3-activities-by-type-status-and-list); plan M1, Open question 3 | 1.7 |
| WP-L10 | **Where should the legal text go in LMS?** | 927 programmes (838 `P`, 88 `PEDL`) | One HTML block per programme, built from the activity descriptions and then edited freely; it feeds the licence documents and won't be split per activity. Options: (a) a programme-level legal text area, like the general comments; (b) copy the whole block to every activity. A licence with a FALLOW or EXTENSION programme can have a block on each. | [WP-S13](#wp-s13-legal-text-and-review-audit); plan D14, Open question 5 | 1.9 |
| WP-L11 | **Work programmes on awards not yet raised** | 23 awards (18 `P`, 5 `PEDL`) | Issued for raise but not raised: a `DRAFT_FINAL` programme and a template schedule, no licence data. They migrate only if the schedule migration creates an LMS licence for them; otherwise the programme is created in LMS at raise. | [WP-S10b](#wp-s10b-licences-with-a-work-programme-but-no-current-status); plan Open questions 7, 9 | 1.11 |

## Shared building blocks

Most queries start from these CTEs. They are repeated in full in each query, so each one can be
copied and run on its own.

```sql
-- Current programme versions, with licence reference
cur_wp AS (
  SELECT wp.id wp_id, wp.ped_lm_id
       , plm.licence_type, plm.licence_type || plm.licence_no licence_reference
       , wpd.id wpd_id, wpd.start_datetime, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C'
  AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
-- Every activity in the current versions, flat and phase lists
, acts AS (
  SELECT c.wpd_id, c.ped_lm_id, c.licence_type, c.licence_reference
       , 'FLAT' list_kind, CAST(NULL AS VARCHAR2(20)) phase_type
       , a.activity_id, a.activity_type, a.activity_status, a.title
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_id     VARCHAR2(20)   PATH 'ACTIVITY_ID'
               , activity_type   VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , activity_status VARCHAR2(40)   PATH 'ACTIVITY_STATUS'
               , title           VARCHAR2(4000) PATH 'TITLE') a
  UNION ALL
  SELECT c.wpd_id, c.ped_lm_id, c.licence_type, c.licence_reference
       , 'PHASE', p.phase_type
       , a.activity_id, a.activity_type, a.activity_status, a.title
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/PHASE_LIST/PHASE' PASSING c.xml_data
         COLUMNS phase_type VARCHAR2(20) PATH 'TYPE'
               , act_list   XMLTYPE      PATH 'ACTIVITY_LIST') p
     , XMLTABLE('/ACTIVITY_LIST/ACTIVITY' PASSING p.act_list
         COLUMNS activity_id     VARCHAR2(20)   PATH 'ACTIVITY_ID'
               , activity_type   VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , activity_status VARCHAR2(40)   PATH 'ACTIVITY_STATUS'
               , title           VARCHAR2(4000) PATH 'TITLE') a
)
-- Licence status (as the schedule survey's S8d)
, latest_dp AS (
  SELECT licence_type, licence_no, ped_dp_id
  FROM (
    SELECT pdp.licence_type, pdp.licence_no, pdp.id ped_dp_id
         , ROW_NUMBER() OVER (PARTITION BY pdp.licence_type, pdp.licence_no
                              ORDER BY pdp.position_datetime DESC, pdp.position_sequence DESC) rn
    FROM pedmgr.ped_data_points pdp
    WHERE pdp.ped_sim_id = 0
  )
  WHERE rn = 1
)
, current_status AS (
  SELECT plm.id ped_lm_id, xplc.licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
)
```

## WP-S1. Programmes by licence type, programme type and status

How many licences have a work programme, how many have the FALLOW and EXTENSION types (plan M4),
and where the programme status sits (plan D11).

```sql
WITH cur_wp AS (
  SELECT wp.id wp_id, wp.ped_lm_id, plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
)
SELECT c.licence_type
     , x.wp_type
     , x.wp_status
     , COUNT(DISTINCT c.ped_lm_id) licences
     , COUNT(*)                    programmes
FROM cur_wp c
   , XMLTABLE('/WORK_PROGRAMME' PASSING c.xml_data
       COLUMNS wp_type   VARCHAR2(20) PATH 'WORK_PROGRAMME_TYPE'
             , wp_status VARCHAR2(20) PATH 'WORK_PROGRAMME_STATUS') x
GROUP BY ROLLUP(c.licence_type, x.wp_type, x.wp_status)
ORDER BY 1 NULLS LAST, 2 NULLS LAST, 3 NULLS LAST;
```

**Result (live, 29 October 2026):** detail rows only; the `ROLLUP` subtotals are folded into the
Total columns.

| LICENCE_TYPE | WP_TYPE | COMPLETE | IN_PROGRESS | NOT_STARTED | DRAFT_FINAL | DRAFT | Total programmes | Licences |
|---|---|---|---|---|---|---|---|---|
| AL | INITIAL | 2 | | 2 | | | 4 | 4 |
| DL | INITIAL | 2 | | 3 | | | 5 | 5 |
| EXL | INITIAL | 12 | | 26 | | | **38** | **37** |
| ML | INITIAL | 2 | | 5 | | | 7 | 7 |
| **P** | INITIAL | 1,369 | 420 | 549 | 76 | | 2,414 | 2,414 |
| **P** | FALLOW | 15 | 2 | 10 | | | 27 | 27 |
| **P** | EXTENSION | 2 | | | | | 2 | 2 |
| **P** | *all* | | | | | | **2,443** | **2,437** |
| **PEDL** | INITIAL | 97 (96 licences) | 21 | 212 (210 licences) | 15 | | **345** | **340** |
| **PEDL** | EXTENSION | | | 1 | | | 1 | 1 |
| **PEDL** | *all* | | | | | | **346** | **340** |
| PL | INITIAL | 18 | | 10 | | | 28 | 28 |
| (no type) | INITIAL | | | | 5 | 8 | 13 | 13 |
| **ALL** | | | | | | | **2,884** | **2,871** |

**Reading:**

- **Size:** 2,884 current programmes on 2,871 licences. `P` and `PEDL` make up 2,789 of them
  (2,777 licences). `CE` has none.
- **Far more licences have a work programme than have a schedule.** Compared with the schedule
  survey's S2:

  | Type | Licences with a work programme | Licences with a schedule (S2) |
  |---|---|---|
  | `P` | 2,437 | 1,525 |
  | `PEDL` | 340 | 220 |
  | `EXL` | 37 | 12 |
  | `PL` | 28 | 26 |
  | `AL` | 4 | 3 |
  | `ML` | 7 | 7 |
  | `DL` | 5 | 5 |

  Roughly **900 `P` and 120 `PEDL` licences** have a work programme but no PEARS schedule (the
  schedule survey's S17 found 1,114 `P` and 124 `PEDL` licences with no schedule, none of them
  extant). A work programme can only be migrated onto a migrated schedule, so this coverage gap is
  the main scoping question. WP-S10 settles it: none of the 1,111 work-programme-only licences
  is extant, so they are left out and reported.
- **No licence type (13):** unnumbered awards, either not yet issued for raise (`DRAFT`, 8) or
  issued but not yet numbered (`DRAFT_FINAL`, 5). There is no LMS licence for them, so they are out
  of scope. `DRAFT` is excluded everywhere (decided).
- **FALLOW and EXTENSION are rare:** 27 `P` FALLOW, 2 `P` EXTENSION and 1 `PEDL` EXTENSION, so 30
  programmes in all. Plan M4 can treat them individually rather than by rule.
  - `P` has 2,437 licences but only 2,414 INITIAL programmes, so **at least 23 `P` licences have a
    FALLOW or EXTENSION programme and no INITIAL one**. Their activities have no initial-term
    programme to sit beside.
  - The single `PEDL` EXTENSION programme is on a licence that also has an INITIAL programme (all
    340 `PEDL` licences have one).
- **More than one INITIAL programme on a licence:** `PEDL` has 345 INITIAL programmes on 340
  licences (one extra `COMPLETE`, two extra `NOT_STARTED`, and two across different statuses), and
  `EXL` has 38 on 37. The plan assumes one programme per type. **WP-S1b** lists these licences.
- **Programme status** (not migrated, plan D11):
  - `DRAFT` only appears on the unraised awards.
  - `DRAFT_FINAL` on 76 `P` and 15 `PEDL` raised licences is the final award programme, never saved
    in `maintain` since raise (PEARS sets `DRAFT_FINAL` on issue for raise and on legacy import;
    only a maintain save re-derives the status). Its activities probably still hold their
    award-time statuses, mostly `NOT_STARTED`; WP-S3 will show.
  - 420 `P` programmes are `IN_PROGRESS`, so activity statuses are actively used on `P`.
  - Onshore types other than `PEDL` are only ever `COMPLETE` or `NOT_STARTED`.

### WP-S1b. Licences with more than one programme of the same type

```sql
WITH cur_wp AS (
  SELECT wp.id wp_id, wp.ped_lm_id, plm.licence_type || plm.licence_no licence_reference, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT c.licence_reference, x.wp_type, COUNT(*) programmes
     , LISTAGG(x.title, ' | ') WITHIN GROUP (ORDER BY c.wp_id) titles
FROM cur_wp c
   , XMLTABLE('/WORK_PROGRAMME' PASSING c.xml_data
       COLUMNS wp_type VARCHAR2(20)  PATH 'WORK_PROGRAMME_TYPE'
             , title   VARCHAR2(200) PATH 'TITLE') x
GROUP BY c.licence_reference, x.wp_type
HAVING COUNT(*) > 1
ORDER BY 1, 2;
```

**Result (live, 29 October 2026):**

| LICENCE_REFERENCE | WP_TYPE | PROGRAMMES | TITLES |
|---|---|---|---|
| EXL253 | INITIAL | 2 | Initial Work Programme ×2 |
| PEDL120 | INITIAL | 2 | Initial Work Programme ×2 |
| PEDL133 | INITIAL | 2 | Initial Work Programme ×2 |
| PEDL180 | INITIAL | 2 | Initial Work Programme ×2 |
| PEDL181 | INITIAL | 2 | Initial Work Programme ×2 |
| PEDL225 | INITIAL | 2 | Initial Work Programme ×2 |
| (empty) | INITIAL | 5 | Initial Work Programme ×5 |

**Reading:**

- **6 licences have two current INITIAL programmes:** EXL253, PEDL120, PEDL133, PEDL180, PEDL181
  and PEDL225, all onshore. That matches WP-S1 (5 extra `PEDL`, 1 extra `EXL`). No licence has
  duplicate FALLOW or EXTENSION programmes, and none has more than two.
- The `(empty)` row is **not a duplicate**. It is the 5 unnumbered `DRAFT_FINAL` awards (WP-S1),
  grouped together because their licence reference is null. They are out of scope.
- Every title is the default "Initial Work Programme", so the titles can't tell a deliberate split
  (e.g. one programme per block set) from an accidental second programme. **WP-S1c** compares the
  two programmes on each licence.

### WP-S1c. The duplicate INITIAL programmes, side by side

One row per programme on the licences from WP-S1b. It shows each programme's status, history and
content summary, so the pairs can be compared.

```sql
WITH dup AS (
  SELECT wp.ped_lm_id
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND plm.licence_no IS NOT NULL
  AND XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_TYPE/text()' PASSING wpd.xml_data RETURNING CONTENT)
              AS VARCHAR2(20)) = 'INITIAL'
  GROUP BY wp.ped_lm_id
  HAVING COUNT(*) > 1
)
SELECT plm.licence_type || plm.licence_no licence_reference
     , wp.id                                  wp_id
     , x.wp_status
     , (SELECT COUNT(*) FROM pedmgr.ped_work_programme_details h
        WHERE h.ped_wp_id = wp.id AND h.record_status = 'ACTIVE')            versions
     , (SELECT MIN(h.start_datetime) FROM pedmgr.ped_work_programme_details h
        WHERE h.ped_wp_id = wp.id AND h.record_status = 'ACTIVE')            first_saved
     , wpd.start_datetime                                                     last_saved
     , x.flat_acts
     , x.phases
     , x.phase_acts
     , x.notes
     , x.legal_text_length
     , NVL2(x.bp_years, x.bp_years || 'y ' || x.bp_months || 'm ' || x.bp_days || 'd', NULL) breakpoint
     , x.activities
FROM dup
JOIN pedmgr.ped_licence_master plm ON plm.id = dup.ped_lm_id
JOIN pedmgr.ped_work_programme wp ON wp.ped_lm_id = dup.ped_lm_id
JOIN pedmgr.ped_work_programme_details wpd
  ON wpd.ped_wp_id = wp.id AND wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
   , XMLTABLE('/WORK_PROGRAMME' PASSING wpd.xml_data
       COLUMNS wp_type           VARCHAR2(20)   PATH 'WORK_PROGRAMME_TYPE'
             , wp_status         VARCHAR2(20)   PATH 'WORK_PROGRAMME_STATUS'
             , flat_acts         NUMBER         PATH 'count(ACTIVITY_LIST/ACTIVITY)'
             , phases            NUMBER         PATH 'count(PHASE_LIST/PHASE)'
             , phase_acts        NUMBER         PATH 'count(PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY)'
             , notes             NUMBER         PATH 'count(NOTES_LIST/NOTE)'
             , legal_text_length NUMBER         PATH 'string-length(string(LEGAL_TEXT))'
             , bp_years          VARCHAR2(10)   PATH 'BREAKPOINT/YEARS'
             , bp_months         VARCHAR2(10)   PATH 'BREAKPOINT/MONTHS'
             , bp_days           VARCHAR2(10)   PATH 'BREAKPOINT/DAYS'
             , activities        VARCHAR2(4000) PATH 'string-join(for $a in .//ACTIVITY
                                                        return concat($a/ACTIVITY_TYPE, ":", $a/ACTIVITY_STATUS), ", ")') x
WHERE x.wp_type = 'INITIAL'
ORDER BY 1, 2;
```

What to look for in each pair:
- **One empty or never touched** (no activities, or 1 version and no notes, while the other has
  history): an accidental second programme. Migrate the other one and report the empty one.
- **Same activities in both:** a copy. Migrate the one with the later `last_saved` or more history,
  and report the other.
- **Different activities in both:** a deliberate split. Migrate both into the one LMS activity list
  and report it.

**Result (live, 29 October 2026):** no programme has phases, legal text or a breakpoint, so those
columns are left out.

| Licence | WP_ID | Status | Versions | First saved | Last saved | Flat acts | Notes | Activities |
|---|---|---|---|---|---|---|---|---|
| EXL253 | 8 | NOT_STARTED | 5 | 20/10/2010 16:20:49 | 28/04/2023 09:39:08 | 1 | 0 | OTHER:NOT_STARTED |
| EXL253 | 2582 | COMPLETE | 4 | **12/07/2011 17:47:27** | 28/04/2023 09:39:08 | 1 | 2 | **LEGACY**:COMPLETE |
| PEDL120 | 2500 | NOT_STARTED | 5 | 26/05/2011 07:31:13 | 27/01/2025 15:16:05 | 1 | 0 | OTHER:NOT_STARTED |
| PEDL120 | 2714 | NOT_STARTED | 4 | **12/07/2011 17:47:29** | 27/01/2025 15:16:05 | 1 | 2 | **LEGACY**:NOT_STARTED |
| PEDL133 | 2368 | COMPLETE | 6 | 14/12/2010 09:51:35 | 29/04/2015 12:50:40 | 1 | 1 | DRILL_OR_DROP_WELL:COMPLETE |
| PEDL133 | 2727 | COMPLETE | 4 | **12/07/2011 17:47:29** | 29/04/2015 12:50:40 | 1 | 2 | **LEGACY**:COMPLETE |
| PEDL180 | 2419 | IN_PROGRESS | 12 | 11/02/2011 10:52:44 | 26/06/2025 14:05:42 | 1 | 1 | DRILL_OR_DROP_WELL:IN_PROGRESS |
| PEDL180 | 2774 | COMPLETE | 10 | **12/07/2011 17:47:29** | 26/06/2025 14:05:41 | 1 | 3 | **LEGACY**:COMPLETE |
| PEDL181 | 2418 | IN_PROGRESS | 9 | 11/02/2011 10:48:16 | 09/05/2023 06:51:03 | 1 | 1 | DRILL_OR_DROP_WELL:IN_PROGRESS |
| PEDL181 | 2775 | COMPLETE | 7 | **12/07/2011 17:47:29** | 09/05/2023 06:51:03 | 1 | 2 | **LEGACY**:COMPLETE |
| PEDL225 | 2513 | NOT_STARTED | 4 | 18/06/2011 09:48:32 | 03/09/2015 14:23:41 | 2 | 1 | DRILL_OR_DROP_WELL:NOT_STARTED, SEISMIC:NOT_STARTED |
| PEDL225 | 2819 | NOT_STARTED | 3 | **12/07/2011 17:47:30** | 03/09/2015 14:23:40 | 1 | 2 | **LEGACY**:DROPPED |

**Reading:**

- **Every pair has the same shape:**
  - one programme created by hand, between 20/10/2010 and 18/06/2011, holding `OTHER`,
    `DRILL_OR_DROP_WELL` or `SEISMIC` activities;
  - one created by a **bulk job on 12/07/2011 between 17:47:27 and 17:47:30**, holding exactly
    **one `LEGACY` activity**.

  The bulk job probably gave a `LEGACY` programme to licences that existed before the work
  programme screen did, and these 6 already had a hand-made programme by then. So they are **not
  copies and not splits**: they hold different activities.
- **Both programmes in each pair are live.** Their `last_saved` times match to within a second, so
  they are saved together. That fits `PED009X` re-versioning every other programme on the licence
  when a review is acknowledged (plan, Lifecycle), so both tabs are under review together. Several
  have saves in 2023–2025.
- **The `LEGACY` activity often has a different status from the hand-made one** (PEDL180 and PEDL181:
  the legacy item is `COMPLETE` while the D&D well is `IN_PROGRESS`). It is a separate obligation,
  or the old commitment text as a whole, not a duplicate of the other activities.
- **PEARS status anomaly:** PEDL225's `LEGACY` programme is `NOT_STARTED` although its only activity is
  `DROPPED`. `action-update-wp-status` has no rule for `DROPPED`, so the programme keeps whatever
  status it had before.
- **Wider question:** if the 12/07/2011 job ran for every licence of that era, there may be many
  single-`LEGACY` programmes. That makes the `LEGACY` mapping in plan M1 matter far more than the 6
  duplicates do. WP-S3 (types) and WP-S4 (titles) will size it. **WP-S1d** shows what these 12
  activities actually say.
- ~~Proposed rule: migrate both programmes into the one LMS activity list.~~ **Withdrawn after
  WP-S1d:** the two programmes describe the same commitment, so merging them would count it twice.
  See WP-S1d.

### WP-S1d. The activities on the duplicate-programme licences

The descriptions of the 12 activities from WP-S1c, to see what a `LEGACY` activity holds compared
with the hand-made ones.

```sql
SELECT plm.licence_type || plm.licence_no              licence_reference
     , wp.id                                            wp_id
     , a.activity_id
     , a.activity_type
     , a.activity_status
     , a.title
     , DBMS_LOB.GETLENGTH(a.description)                description_length
     , DBMS_LOB.SUBSTR(a.description, 400, 1)           description_start
FROM pedmgr.ped_licence_master plm
JOIN pedmgr.ped_work_programme wp ON wp.ped_lm_id = plm.id
JOIN pedmgr.ped_work_programme_details wpd
  ON wpd.ped_wp_id = wp.id AND wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
   , XMLTABLE('/WORK_PROGRAMME//ACTIVITY' PASSING wpd.xml_data
       COLUMNS activity_id     VARCHAR2(20)  PATH 'ACTIVITY_ID'
             , activity_type   VARCHAR2(40)  PATH 'ACTIVITY_TYPE'
             , activity_status VARCHAR2(40)  PATH 'ACTIVITY_STATUS'
             , title           VARCHAR2(200) PATH 'TITLE'
             , description     CLOB          PATH 'DESCRIPTION') a
WHERE plm.licence_type || plm.licence_no IN ('EXL253', 'PEDL120', 'PEDL133', 'PEDL180', 'PEDL181', 'PEDL225')
ORDER BY 1, 2, 3;
```

**Result (live, 29 October 2026):** each pair is shown hand-made first, then `LEGACY`.

| Licence | WP | Type | Status | Title | Len | Description |
|---|---|---|---|---|---|---|
| EXL253 | 8 | OTHER | NOT_STARTED | Other Activity 1 | 41 | Old Licence No outstanding Work Programme |
| EXL253 | 2582 | LEGACY | COMPLETE | *(empty)* | 30 | Appraisal of minegas potential |
| PEDL120 | 2500 | OTHER | NOT_STARTED | Initial commitments met | 66 | 470 km existing 2D 2 DorD 3 yrs - Drilled Eberston South discovery |
| PEDL120 | 2714 | LEGACY | NOT_STARTED | *(empty)* | 31 | 470 km existing 2D 2 DorD 3 yrs |
| PEDL133 | 2368 | DRILL_OR_DROP_WELL | COMPLETE | Drill or Drop Well 1 | 15 | One inseam well |
| PEDL133 | 2727 | LEGACY | COMPLETE | *(empty)* | 21 | DorD in seam drilling |
| PEDL180 | 2419 | DRILL_OR_DROP_WELL | IN_PROGRESS | Drill or Drop Well 1 | 47 | DorD 3 yrs 1500m 20km new 2D O all 2D R 40km 2D |
| PEDL180 | 2774 | LEGACY | COMPLETE | *(empty)* | 49 | DorD 3 yrs 1500m **30km** new 2D O **500km** 2D R **70km** 2D |
| PEDL181 | 2418 | DRILL_OR_DROP_WELL | IN_PROGRESS | Drill or Drop Well 1 | 69 | DorD 3 yr 70km new 2D O 500km 2D R 130km 2D ⏎ 13th Rnd award 01/07/2008 |
| PEDL181 | 2775 | LEGACY | COMPLETE | *(empty)* | 43 | DorD 3 yr 70km new 2D O 500km 2D R 130km 2D |
| PEDL225 | 2513 | DRILL_OR_DROP_WELL | NOT_STARTED | Drill or Drop Well 1 | 45 | One well to 1000m, decisio by end of 3rd year |
| PEDL225 | 2513 | SEISMIC | NOT_STARTED | Shoot 2D Seismic Data 1 | 29 | Shoot 65 kms 2D seismic data. |
| PEDL225 | 2819 | LEGACY | **DROPPED** | *(empty)* | 27 | DorD 1000m 3 yr 65km new 2D |

**Reading:**

- **A `LEGACY` activity is a shorthand summary of the whole commitment, not one item.** Examples:
  - "DorD 3 yrs 1500m 30km new 2D O 500km 2D R 70km 2D" means a drill-or-drop well to 1,500 m
    within 3 years, shoot 30 km of new 2D, obtain 500 km of existing 2D, and reprocess 70 km of 2D.
    That one activity spans four LMS categories (`DRILL_OR_DROP_WELL`, `NEW_SHOOT_2_D_SEISMIC_DATA`,
    `OBTAIN_EXISTING_2_D_SEISMIC_DATA`, `REPROCESS_SEISMIC_DATA`).
  - The shorthand (`DorD`, `O` = obtain, `R` = reprocess) is too loose to split reliably by rule.
- **`LEGACY` activities have no `TITLE`.** M1's "`otherCategoryName` = `TITLE`" doesn't work for
  them, so they need a fixed name, e.g. "Legacy work programme".
- **The hand-made programme restates the same commitment.** It is near-identical on PEDL120 and
  PEDL181. On PEDL133 and PEDL225 it is the same commitment broken into structured items. So each
  pair is **one obligation recorded twice**, and merging them would duplicate it on the LMS timeline.
- **The two records disagree in places:**
  - **PEDL180:** the figures differ: 20 km vs 30 km new 2D, "all" vs 500 km obtained, 40 km vs 70 km
    reprocessed. One of them was edited, and nothing says which is right.
  - **PEDL225:** `LEGACY` says `DROPPED` (the drill-or-drop decision was to drop), but the hand-made
    D&D well and seismic are still `NOT_STARTED`.
  - **PEDL180 and PEDL181:** `LEGACY` is `COMPLETE` but the D&D well is `IN_PROGRESS`.
  - **PEDL120:** titled "Initial commitments met" ("Drilled Eberston South discovery") but
    `NOT_STARTED`, and so is its `LEGACY` twin.
  - **EXL253:** two unrelated texts. The hand-made one says "Old Licence No outstanding Work
    Programme" (`NOT_STARTED`); the `LEGACY` one says "Appraisal of minegas potential" (`COMPLETE`).
- **Activity statuses on these licences are not reliable.** The status the licensing team actually
  maintained seems to be split between the two programmes. The 2023–2025 saves are probably review
  acknowledgements (both tabs re-versioned together), not status edits. WP-S9's notes would confirm.
- **Descriptions are short (15–69 characters)** and can contain line breaks (PEDL181), so the
  description needs to allow multi-line text.
- **Rule:** not decidable from the data. Which record is authoritative is a licensing team question
  (**WP-L2**). Until they answer, migrate neither programme on these 6 licences: report them as
  "two INITIAL programmes in PEARS" and let the licensing team enter the work programme in LMS.

## WP-S2. Versions and organisation

How much history there is (plan "Version alignment": options A and B only differ for programmes
with more than one version), and whether each programme uses the flat list, phases or both.

```sql
WITH per_wp AS (
  SELECT wp.id wp_id, plm.licence_type
       , COUNT(*) versions
       , MIN(wpd.start_datetime) first_saved
       , MAX(wpd.start_datetime) last_saved
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.record_status = 'ACTIVE'
  GROUP BY wp.id, plm.licence_type
)
SELECT CASE WHEN GROUPING(licence_type) = 1 THEN 'ALL' ELSE licence_type END lic_type
     , COUNT(*)                                        programmes
     , SUM(versions)                                   versions
     , SUM(CASE WHEN versions = 1 THEN 1 ELSE 0 END)   single_version
     , ROUND(AVG(versions), 1)                         avg_versions
     , MAX(versions)                                   max_versions
     , MIN(first_saved)                                earliest
     , MAX(last_saved)                                 latest
FROM per_wp
GROUP BY ROLLUP(licence_type)
ORDER BY GROUPING(licence_type), 1;
```

**Result (live, 29 October 2026):**

| LIC_TYPE | PROGRAMMES | VERSIONS | SINGLE VERSION | AVG | MAX | EARLIEST | LATEST |
|---|---|---|---|---|---|---|---|
| AL | 4 | 13 | 1 | 3.3 | 6 | **12/07/2011 17:47:26** | 29/04/2026 13:10:58 |
| DL | 5 | 15 | 2 | 3 | 5 | **12/07/2011 17:47:27** | 03/07/2024 10:46:32 |
| EXL | 38 | 89 | 25 | 2.3 | 11 | 20/10/2010 16:20:49 | 28/03/2025 14:57:56 |
| ML | 7 | 21 | 1 | 3 | 5 | **12/07/2011 17:47:27** | 16/10/2026 15:29:07 |
| **P** | **2,443** | **15,285** | 1,017 | 6.3 | **61** | 18/10/2010 11:47:00 | 29/10/2026 10:18:09 |
| **PEDL** | **346** | **1,803** | 129 | 5.2 | 23 | 14/12/2010 09:51:35 | 11/08/2026 16:54:45 |
| PL | 28 | 124 | 5 | 4.4 | 13 | **12/07/2011 17:47:27** | 19/10/2026 16:28:29 |
| (no type) | 13 | 66 | 3 | 5.1 | 11 | 18/08/2015 10:33:57 | 28/08/2024 14:07:07 |
| **ALL** | **2,884** | **17,416** | **1,183** | 6 | 61 | 18/10/2010 11:47:00 | 29/10/2026 10:18:09 |

**Reading:**

- **17,416 versions** across 2,884 programmes, 15,285 of them on `P`, with the largest history
  at 61. That's more than the schedule's 10,834 versions (schedule survey S2), on fewer licences
  with a schedule.
- **1,183 programmes (41%) have only one version.** They were created and never saved again. Most
  are `P` (1,017), and they include 25 of the 38 `EXL`.
- **The work programme screen went live in October 2010** (earliest save 18/10/2010).
- **`AL`, `DL`, `ML` and `PL` all start at 12/07/2011 17:47:26–27**, the same bulk job as the
  `LEGACY` programmes in WP-S1c. So every programme on those four types was most likely created by
  that job, and is probably a single `LEGACY` activity. WP-S3 will confirm. `P`, `PEDL` and `EXL`
  have hand-made programmes from before the job.
- **Still in daily use:** the latest saves are this month (`P` today, 29/10/2026; `ML` and `PL` in
  October 2026). No cutover gap, though: the migration and the `PED009X` redirect to LMS go live
  together (plan, "Decision: no write-back to PEARS").
- **Not every version is a content change.**
  - Acknowledging a review in `PED009X` re-versions every other programme on the licence, just to
    append `REVIEW_AUDIT` (plan, Lifecycle). The matching `last_saved` times in WP-S1c show this
    happening.
  - Status changes made through `set_work_programme_status` create **no** version at all.

  So version count measures review activity as much as edits.
- **Version alignment (plan):** the review-only saves don't by themselves rule out interleaving
  (option B), because B only picks the version current at each schedule save. WP-S2c measures what
  B would keep. Status history comes from the notes (WP-S9), not from the versions.
- The 13 unnumbered awards (66 versions) are out of scope (WP-S1).

### WP-S2b. Flat vs phase organisation (current versions)

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT licence_type
     , CASE WHEN flat_acts > 0 AND phase_acts > 0 THEN 'BOTH'
            WHEN flat_acts > 0                   THEN 'FLAT'
            WHEN phase_acts > 0                  THEN 'PHASE'
            WHEN phases > 0                      THEN 'PHASES, NO ACTIVITIES'
            ELSE 'EMPTY' END organisation
     , COUNT(*) programmes
FROM (
  SELECT c.licence_type
       , XMLCAST(XMLQUERY('count(/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY)'
                          PASSING c.xml_data RETURNING CONTENT) AS NUMBER)                    flat_acts
       , XMLCAST(XMLQUERY('count(/WORK_PROGRAMME/PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY)'
                          PASSING c.xml_data RETURNING CONTENT) AS NUMBER)                    phase_acts
       , XMLCAST(XMLQUERY('count(/WORK_PROGRAMME/PHASE_LIST/PHASE)'
                          PASSING c.xml_data RETURNING CONTENT) AS NUMBER)                    phases
  FROM cur_wp c
)
GROUP BY licence_type
       , CASE WHEN flat_acts > 0 AND phase_acts > 0 THEN 'BOTH'
              WHEN flat_acts > 0                   THEN 'FLAT'
              WHEN phase_acts > 0                  THEN 'PHASE'
              WHEN phases > 0                      THEN 'PHASES, NO ACTIVITIES'
              ELSE 'EMPTY' END
ORDER BY 1, 2;
```

`PHASES, NO ACTIVITIES` and `PHASE` programmes are the ones PEARS's status derivation marks
`COMPLETE` regardless of their activities (plan, "Findings in PEARS"). Compare with WP-S1's status
column.

**Result (live, 29 October 2026):** `DRAFT` excluded.

| LICENCE_TYPE | ORGANISATION | PROGRAMMES |
|---|---|---|
| AL | FLAT | 4 |
| DL | FLAT | 5 |
| EXL | FLAT | 38 |
| ML | FLAT | 7 |
| **P** | **FLAT** | **2,161** |
| **P** | **PHASE** | **282** |
| PEDL | FLAT | 346 |
| PL | FLAT | 28 |
| (no type) | PHASE | 5 |

**Reading:**

- **Total 2,876** = WP-S1's 2,884 less the 8 `DRAFT`, so the filter works.
- **Every programme has activities.** There are no `EMPTY`, no `PHASES, NO ACTIVITIES` and no
  `BOTH`, so a programme uses one list or the other, never both.
- **Phase organisation is only on 282 `P` programmes**, the offshore rounds configured with
  `USE_PHASE_ORGANISATION`, plus the 5 unnumbered `DRAFT_FINAL` awards, which are recent rounds and
  out of scope. **Everything else is flat:**
  - 2,161 `P`;
  - every onshore programme (`PEDL`, `EXL`, `PL`, `ML`, `DL`, `AL`).
- **Impact on dates (plan M3):**
  - Only the 282 phase programmes carry any timing (the phase each activity sits in).
  - The other **2,594 numbered programmes (90%)** have no date information at all, so their
    activities get the default anchor (`WITHIN_A_TERM` Initial, or the D&D breakpoint).

  The default anchor is the main rule, not a fall-back.
- **PEARS status derivation:** all 282 phase programmes have an empty flat list. Saving one in
  maintain therefore passes both the "all `NOT_STARTED`" and "all `COMPLETE`" tests (0 = 0), and
  the programme ends up `COMPLETE` whatever its activities say. Programme status isn't migrated
  (plan D11), but PEARS reports that show it (`PED_LICENCE_RAISE_REPORTS`) are wrong for these.
  WP-S3 shows the real activity statuses.

### WP-S2c. Work programme changes vs schedule versions (option B)

Option B attaches, to each migrated schedule version, the work programme version that was current
at that schedule version's `start_datetime`. This checks how much real change that keeps.

- **Content changes only:** a version counts only if its activities differ from the previous
  version's. It uses a fingerprint of each activity's ID, type, status and title/description
  lengths, so review-only re-saves drop out.
- **Captured:** a schedule version starts on or after the change and before the programme's next
  content change, so some LMS version shows it.
- **Lost:** the change was superseded before the next schedule save, so no LMS version shows it
  under B.
- **After the last schedule save:** the change is newer than the current schedule version. Under
  B these need the current work programme attached to the `ACTIVE` detail anyway, as in A.

Only licences with a schedule, and programmes whose current version isn't `DRAFT`.

```sql
WITH wpv AS (
  SELECT wp.ped_lm_id, wpd.ped_wp_id, wpd.start_datetime
       , ORA_HASH(XMLCAST(XMLQUERY(
           'string-join(for $a in /WORK_PROGRAMME//ACTIVITY
                        return concat($a/ACTIVITY_ID, ":", $a/ACTIVITY_TYPE, ":", $a/ACTIVITY_STATUS, ":",
                                      string-length($a/TITLE), ":", string-length($a/DESCRIPTION)), ",")'
           PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(4000))) fp
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.record_status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = wp.ped_lm_id)
  AND EXISTS (SELECT 1 FROM pedmgr.ped_work_programme_details cur
              WHERE cur.ped_wp_id = wp.id AND cur.status_control = 'C' AND cur.record_status = 'ACTIVE'
              AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING cur.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT')
)
, changes AS (
  SELECT ped_lm_id, ped_wp_id, start_datetime
  FROM (
    SELECT w.*, LAG(fp) OVER (PARTITION BY ped_wp_id ORDER BY start_datetime) prev_fp
    FROM wpv w
  )
  WHERE prev_fp IS NULL OR fp != prev_fp
)
, changes2 AS (
  SELECT c.*
       , LEAD(start_datetime) OVER (PARTITION BY ped_wp_id ORDER BY start_datetime) next_change
       , ROW_NUMBER() OVER (PARTITION BY ped_wp_id ORDER BY start_datetime) change_no
  FROM changes c
)
, classified AS (
  SELECT c.*
       , CASE WHEN EXISTS (SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd
                           WHERE plsd.ped_lm_id = c.ped_lm_id
                           AND plsd.start_datetime >= c.start_datetime
                           AND (c.next_change IS NULL OR plsd.start_datetime < c.next_change))
              THEN '1 Captured'
              WHEN c.next_change IS NULL
              THEN '3 After the last schedule save'
              ELSE '2 Lost'
         END outcome
  FROM changes2 c
)
SELECT outcome
     , CASE WHEN change_no = 1 THEN 'First version' ELSE 'Later change' END change_kind
     , COUNT(*)                   changes
     , COUNT(DISTINCT ped_wp_id)  programmes
     , COUNT(DISTINCT ped_lm_id)  licences
FROM classified
GROUP BY outcome, CASE WHEN change_no = 1 THEN 'First version' ELSE 'Later change' END
ORDER BY 1, 2;
```

`string-join` over a large programme could pass 4,000 characters. If Oracle raises ORA-19011 or a
buffer error, drop the title length from the fingerprint.

**Result (live, 29 October 2026):**

| Outcome | Change kind | Changes | Programmes | Licences |
|---|---|---:|---:|---:|
| Captured | First version | 1,174 | 1,174 | 1,172 |
| Captured | Later change | 897 | 720 | 717 |
| Lost | First version | 428 | 428 | 426 |
| Lost | Later change | 507 | 315 | 315 |
| After the last schedule save | First version | 159 | 159 | 157 |
| After the last schedule save | Later change | 692 | 692 | 692 |
| **Total** | | **3,857** | | |

**Reading:**

- **Real content changes are far fewer than versions.** The 1,761 programmes in scope have 3,857
  content states, against 17,416 versions in WP-S2, so most saves are review-only.
- **851 programmes (48%) changed after their licence's last schedule save.** That's the 692 whose
  last change is later and the 159 created after it, e.g. a programme added to a licence whose
  schedule hasn't been saved since. Under B, as under A, the `ACTIVE` detail has to take the
  current work programme regardless of time.
- **B's extra history over A.** A migrates the current state of each programme. The other 2,096
  states are past ones:
  - **1,161 (55%) captured:** some schedule version would show them. That's the 2,071 captured
    less the 910 that are programmes' current state.
  - **935 (45%) lost:** replaced before the next schedule save, so no LMS version holds them.
    428 of these are first versions, mostly award-time content edited again before the schedule
    was next saved. The other 507, on 315 programmes, are later edits.
- **The dates would be the schedule's, not the work programme's.** Under B a captured state shows
  on the schedule version saved after it, so LMS would date the change to that schedule save.
- The fingerprint ignores edits that keep the same text length, so "lost" may be slightly
  under-counted.

So B keeps just over half of the past work programme states, each dated to a later schedule save.
PEARS is decommissioned some time after LMS goes live and its history isn't kept, so under A
the past states are lost; only the status history and notes survive.

## WP-S3. Activities by type, status and list

The mapping tables M1 (category and commitment) and M5 (status) depend on which combinations
actually exist. The run fails on any type M1 has no row for, so every value must appear here first.

```sql
WITH cur_wp AS (
  SELECT wp.ped_lm_id, plm.licence_type, wpd.id wpd_id, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, acts AS (
  SELECT c.licence_type, 'FLAT' list_kind, a.activity_type, a.activity_status
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type   VARCHAR2(40) PATH 'ACTIVITY_TYPE'
               , activity_status VARCHAR2(40) PATH 'ACTIVITY_STATUS') a
  UNION ALL
  SELECT c.licence_type, 'PHASE', a.activity_type, a.activity_status
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type   VARCHAR2(40) PATH 'ACTIVITY_TYPE'
               , activity_status VARCHAR2(40) PATH 'ACTIVITY_STATUS') a
)
SELECT list_kind
     , NVL(activity_type, '(empty)')   activity_type
     , NVL(activity_status, '(empty)') activity_status
     , COUNT(*)                        activities
     , LISTAGG(DISTINCT licence_type, ',') WITHIN GROUP (ORDER BY licence_type) licence_types
FROM acts
GROUP BY list_kind, activity_type, activity_status
ORDER BY 1, 2, 3;
```

`LISTAGG(DISTINCT ...)` needs Oracle 19c. On older versions, drop the column.

**Result (live, 29 October 2026):** `DRAFT` excluded. Pivoted by status here; the query returns one
row per combination. `LISTAGG(DISTINCT)` skips nulls, so the few activities on unnumbered awards
are hidden in these rows, except one `SHOOT_2D_SEISMIC_FIRM` row whose only licence type is empty.

| List | Type | NOT_STARTED | IN_PROGRESS | COMPLETE | FULL_WAIVER | PART_WAIVER | WAIVER_DECLINED | DROPPED | **Total** | Licence types |
|---|---|---|---|---|---|---|---|---|---|---|
| FLAT | `CONTINGENT_WELL` | 4 | | 11 | 45 | 4 | | | **64** | P |
| FLAT | `DRILL_OR_DROP_WELL` | 129 | 4 | 76 | 3 | | | 408 | **620** | P, PEDL |
| FLAT | `FIRM_WELL` | 30 | | 57 | 14 | 2 | 7 | | **110** | P, PEDL |
| FLAT | `LEGACY` | 630 | 22 | 1,032 | 36 | 4 | 3 | 55 | **1,782** | all seven |
| FLAT | `OTHER` | 63 | 4 | 248 | 5 | 1 | 4 | | **325** | P (+ EXL, PEDL `NOT_STARTED`) |
| FLAT | `SEISMIC` | 102 | 10 | 506 | 39 | 4 | 2 | | **663** | P, PEDL |
| PHASE | `CONTINGENT_WELL` | 1 | | 1 | | | | | **2** | P |
| PHASE | `DRILL_OR_DROP_WELL` | 258 | | 9 | | | | 3 | **270** | P |
| PHASE | `FIRM_WELL` | 4 | | 9 | | | | | **13** | P |
| PHASE | `OBTAIN_2D_SEISMIC` | 2 | | 25 | 2 | | | | **29** | P |
| PHASE | `OBTAIN_3D_SEISMIC` | 31 | 1 | 92 | 2 | | | | **126** | P |
| PHASE | `OTHER` | 193 | 2 | 415 | 9 | 1 | | | **620** | P |
| PHASE | `REPROCESS_2D_SEISMIC` | 2 | | 12 | | | | | **14** | P |
| PHASE | `REPROCESS_3D_SEISMIC` | 21 | | 66 | 5 | | | | **92** | P |
| PHASE | `SHOOT_2D_SEISMIC_CONTINGENT` | 1 | | | | | | | **1** | P |
| PHASE | `SHOOT_2D_SEISMIC_FIRM` | 1 | | 1 | | | | | **2** | P, (no type) |
| PHASE | `SHOOT_3D_SEISMIC_CONTINGENT` | 19 | | 6 | 9 | | | | **34** | P |
| PHASE | `SHOOT_3D_SEISMIC_FIRM` | 7 | 1 | 11 | 1 | | | | **20** | P |
| | **Total** | **1,498** | **44** | **2,577** | **170** | **16** | **16** | **466** | **4,787** | |

Flat lists hold 3,564 activities and phase lists 1,223.

**Reading:**

- **Every type and status is a known value.** There are no empty types or statuses, and nothing
  outside the `PED009X` map-sets. The plain `SHOOT_2D_SEISMIC` and `SHOOT_3D_SEISMIC` (no
  `_FIRM`/`_CONTINGENT`) never occur, and neither does `LEGACY` in a phase list.
- **`LEGACY` is the biggest type: 1,782 activities (37%), on every licence type.** `AL`, `DL`, `ML`
  and `PL` have only `LEGACY` activities, and `EXL` has only `LEGACY` and `OTHER`. That confirms
  WP-S2: the 12/07/2011 job gave every licence of the time one `LEGACY` summary. M1 maps it to
  `OTHER_ACTIVITY` "Legacy work programme".
- **Category mapping (plan M1), by volume:**

  | LMS category | Activities | From |
  |---|---|---|
  | `OTHER_ACTIVITY` | 2,727 (57%) | `LEGACY` 1,782, `OTHER` 945 |
  | `DRILL_OR_DROP_WELL` | 890 | `DRILL_OR_DROP_WELL` |
  | (flat `SEISMIC`, classified from title) | 663 | see WP-S4 |
  | `DRILL_WELL` | 189 | `FIRM_WELL` 123, `CONTINGENT_WELL` 66 |
  | `OBTAIN_EXISTING_2_D` / `_3_D_SEISMIC_DATA` | 29 / 126 | phase list |
  | `REPROCESS_SEISMIC_DATA` | 106 | `REPROCESS_2D` 14, `REPROCESS_3D` 92 |
  | `NEW_SHOOT_2_D` / `_3_D_SEISMIC_DATA` | 3 / 54 | phase list, suffixed |

  More than half of the migrated activities would land in `OTHER_ACTIVITY`.
- **Commitment (plan M1, Q3):** only 246 activities (5%) state one: firm or contingent wells (189)
  and suffixed shoot seismic (57). The 890 D&D wells need the D&D default (`CONDITIONAL`?). **The
  other 3,651 (76%) hold no commitment at all** (`LEGACY`, `OTHER`, `SEISMIC`, obtain,
  reprocess), so whatever default is chosen applies to most of the migration.
- **Statuses (plan M5):**

  | PEARS | Activities | LMS |
  |---|---|---|
  | `COMPLETE` | 2,577 (54%) | `COMPLETE` |
  | `NOT_STARTED` | 1,498 (31%) | `OPEN` |
  | `DROPPED` | **466 (10%)** | **no value**: 411 D&D wells, 55 `LEGACY` |
  | `FULL_WAIVER` | 170 | `FULL_WAIVER` |
  | `IN_PROGRESS` | 44 | `IN_PROGRESS` |
  | `PART_WAIVER` | 16 | **no value** |
  | `WAIVER_DECLINED` | 16 | **no value** |

  `DROPPED` is too common to fold into a comment: it is the normal outcome of a drill-or-drop
  well (411 of 890). `PART_WAIVER` and `WAIVER_DECLINED` are rare (32 together).
- **Anomaly:** 3 flat D&D wells are `FULL_WAIVER`, which isn't in the D&D map-set
  (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETE`, `DROPPED`). The status was probably set before the
  type was changed, or before the map-sets were split. Valid in LMS (`FULL_WAIVER`), so no action.
- **Typical outcomes:** 45 of 66 contingent wells are `FULL_WAIVER` (never triggered), and 411 of
  890 D&D wells are `DROPPED`. 258 of the 270 phase-list D&D wells are still `NOT_STARTED`; these
  are the recent rounds.

### WP-S3b. Activities with a waiver status LMS doesn't have

The 32 `PART_WAIVER` and `WAIVER_DECLINED` activities, with their licence status and the latest
status note, for the licensing team (WP-L4). Uses the same licence status lookup as WP-S10.

```sql
WITH latest_dp AS (
  SELECT licence_type, licence_no, ped_dp_id
  FROM (
    SELECT pdp.licence_type, pdp.licence_no, pdp.id ped_dp_id
         , ROW_NUMBER() OVER (PARTITION BY pdp.licence_type, pdp.licence_no
                              ORDER BY pdp.position_datetime DESC, pdp.position_sequence DESC) rn
    FROM pedmgr.ped_data_points pdp
    WHERE pdp.ped_sim_id = 0
  )
  WHERE rn = 1
)
, current_status AS (
  SELECT plm.id ped_lm_id, xplc.licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
)
SELECT plm.licence_type || plm.licence_no                 licence_reference
     , NVL(cs.licence_status, '(no status)')               licence_status
     , a.activity_status
     , a.activity_type
     , a.title
     , DBMS_LOB.SUBSTR(a.description, 300, 1)              description_start
     , a.last_note_datetime
     , a.last_note
FROM pedmgr.ped_work_programme wp
JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
JOIN pedmgr.ped_work_programme_details wpd
  ON wpd.ped_wp_id = wp.id AND wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
   , XMLTABLE('for $a in /WORK_PROGRAMME//ACTIVITY[ACTIVITY_STATUS = ("PART_WAIVER", "WAIVER_DECLINED")]
               let $n := (/WORK_PROGRAMME/NOTES_LIST/NOTE[ACTIVITY_ID = $a/ACTIVITY_ID])[last()]
               return <A>{$a/ACTIVITY_TYPE, $a/ACTIVITY_STATUS, $a/TITLE, $a/DESCRIPTION}
                         <NOTE_DT>{string($n/DATETIME)}</NOTE_DT>
                         <NOTE>{substring(string($n/DESCRIPTION), 1, 300)}</NOTE></A>'
       PASSING wpd.xml_data
       COLUMNS activity_type      VARCHAR2(40)  PATH 'ACTIVITY_TYPE'
             , activity_status    VARCHAR2(40)  PATH 'ACTIVITY_STATUS'
             , title              VARCHAR2(200) PATH 'TITLE'
             , description        CLOB          PATH 'DESCRIPTION'
             , last_note_datetime VARCHAR2(40)  PATH 'NOTE_DT'
             , last_note          VARCHAR2(400) PATH 'NOTE') a
ORDER BY a.activity_status, 2, 1;
```

"Latest note" is the last note in document order for that activity. Notes are appended, so that is
normally the most recent, and it usually records the waiver decision.

**Result (live, 29 October 2026):** 32 rows. Descriptions and notes are shortened here; the query
returns the first 300 characters.

**`PART_WAIVER` (16, all surrendered):**

| Licence | Type | Activity (short) | Latest note (short) | What it actually records |
|---|---|---|---|---|
| P1580 | LEGACY | D/D to 3505m, Ettrick or Buzzard sst | No well drilled on the Block, but part of the Blackbird Field… | Obligation met another way (field development) |
| P1817 | SEISMIC | Reprocess 230 sq km 3D to PreSDM | "Apologies - pressed wrong button. This part of WP complete." | **Error**: should be `COMPLETE` |
| P1835 | CONTINGENT_WELL | One contingent well to 1800m | See General Note below. | Detail is in a general note |
| P2015 | CONTINGENT_WELL | One contingent well, Bourbon Prospect | Contingent well waived and allowed to become a Drill or Drop | Converted to D&D |
| P2050 | CONTINGENT_WELL | One contingent well, Komodo | Komodo drilled as Firm; Contingent allowed to move elsewhere | Moved elsewhere |
| P2067 | FIRM_WELL | Three firm wells (Kookaburra, Wall, Lorikeet) | Kookaburra drilled. | One of three done |
| P2130 | CONTINGENT_WELL | One contingent well to 3500m | Contingent well moved to P1731… retained with a 2 year D/D | **Transferred** to P1731 |
| P2171 | SEISMIC | Obtain 80 km 2D | In progress. | Probably `IN_PROGRESS` |
| P2188 | SEISMIC | Obtain 200 sq km 3D | Transferred. | **Transferred** |
| P2312 | OTHER | Phase A studies + CSEM feasibility | CSEM contingent commitment waived, 24th April 2018 | Genuine part waiver |
| P2330 | SEISMIC | Shoot 600 sq km 3D (contingent, Phase B) | See note below. Decision made 10/04/22. | Detail is in another note |
| P2416 | OTHER | AVO and inversion products | …fulfilled by the alternative work programme… | Alternative work programme agreed |
| P912 | FIRM_WELL | Two firm wells | Contingent well waived. | Note doesn't match the activity (a firm well) |
| PEDL176 | LEGACY | 3F 800m O 20km 2D R 10km 2D | 2 firm wells outstanding. L46/01-3 drilled. | One of three done |
| PEDL179 | LEGACY | 2F 700m O 20km 2D R 10km 2D | "hfhyrht" | Junk note |
| PEDL211 | LEGACY | 2F 1000m | One well drilled… other waived as CBM deemed uneconomic | Genuine part waiver |

**`WAIVER_DECLINED` (16: 1 extant, 1 expired, 14 surrendered):**

| Licence | Status | Type | Activity (short) | Latest note (short) | What it actually records |
|---|---|---|---|---|---|
| **P1483** | **E** | FIRM_WELL | One firm well to 1800m, Carboniferous | Second firm well reallocated to another prospect… within extended initial term of this licence | **Still live**: moved within the same licence; obligation stands |
| P1286 | S | LEGACY | D/D to 1500m; acquire 250 km 2D | Caithness didn't drill but now IGas so can't chase | Unmet obligation on a surrendered licence |
| P1611 | S | LEGACY | 1F to 2060m, 14/26b-5 | FIRM WELL NOT WAIVED; potential Black Mark as Licence DROPPED | Unmet obligation |
| P1635 | S | LEGACY | 1F to 3650m, Katherine; reprocess 200 sq km 3D | Appropriate alternative still under discussion… | Unresolved alternative |
| P1822 | S | FIRM_WELL | One firm well to 5600m, Horne | Well NOT waived; Obligation transferred to West Franklin | **Transferred** |
| P1973 | S | FIRM_WELL | One firm well to 2000m, N17 | Waiver declined; well drilled on Sween instead (P2216) | **Transferred** to P2216 |
| P1995 | S | FIRM_WELL | One firm well to 2600m, Tulla | Will be drilled elsewhere; TAQA Sturgeon in P1774 | **Transferred** to P1774 |
| P2077 | S | FIRM_WELL | One firm well to 1250m, Sunbeam | Well transferred elsewhere… | **Transferred** |
| P2134 | S | FIRM_WELL | One firm well to 3903m | Transfer of Obligation agreed between Ineos and Bayerngas… | **Transferred** |
| P2138 | S | OTHER ×2 | Airborne gravity/magnetic; multibeam bathymetry | Relinquished with outstanding WP; Siccar Point accepted outstanding obligations | Obligations carried after relinquishment |
| P2139 | S | OTHER ×2 | Airborne gravity/magnetic; multibeam bathymetry | As P2138 | Obligations carried after relinquishment |
| P2284 | S | SEISMIC | Obtain 230 sq km 3D | Work Commitment not met. Alternative to be agreed with HALO | Unmet obligation |
| P2290 | S | FIRM_WELL | One firm well to 3250m, Newton | Firm well transferred - see below | **Transferred** |
| P2108 | X | SEISMIC | Shoot 350 km 2D | See below. | Detail is in another note |

**Reading:**

- **Only one is on a live licence: P1483** (extant, `WAIVER_DECLINED`, the second firm well still
  to be drilled within the extended initial term). The other 31 are on surrendered (30) or expired
  (1) licences, so for them the mapping only affects history.
- **The PEARS status is not the real outcome; the notes are.** The 32 break down as:
  - **obligation transferred to another licence: 9.** That's 6 `WAIVER_DECLINED` (P1822, P1973,
    P1995, P2077, P2134, P2290) and 3 `PART_WAIVER` (P2130, P2188, P2050). **LMS already has a
    `TRANSFERRED` status** with `licenceTransferredTo`. The target licence is sometimes named in the
    note (P1731, P2216, P1774), but only as free text.
  - **unmet obligation left on a surrendered licence: 8** (P1286, P1611, P1635, P2284, P2138 ×2,
    P2139 ×2). Some are carried by the licensee after relinquishment ("Siccar Point accepted
    outstanding obligations").
  - **genuine part waiver, or part done: 7** (P1580, P2015, P2067, P2312, P2416, PEDL176, PEDL211).
  - **status set in error or unclear: 7.** P1817 ("pressed wrong button… complete"), P2171 ("In
    progress"), P912 (note about a different well), PEDL179 (junk note), and P2108, P1835 and P2330,
    which point at another note.
  - **still live: 1.** P1483 (extant): the well was moved to another prospect on the same licence,
    to be drilled within the extended initial term. The obligation stands.
- **"See General Note below" / "see note below":** the explanation is often in a `GENERAL` note or
  a later note, not the latest activity note. General notes carry real decision history. They
  migrate to the planned LMS schedule comments area (plan M6), so the explanation stays visible
  next to the activity's own comments.
- **`LEGACY` activities on `P` licences do have titles**, e.g. "Legacy Obligations: Original
  blocks 20/3f. Competed 1". Their descriptions start with a licence-kind code, "(T)" or "(P)",
  probably Traditional / Promote. Only the onshore `LEGACY` activities in WP-S1d were untitled.
  WP-S4 will show the full split.
- **Activity descriptions can be long** (the contingent well clause runs past 300 characters), and
  P2067's "Firm Well 1" describes three wells in one activity.

## WP-S4. Titles of `SEISMIC`, `LEGACY` and `OTHER` activities

M1 classifies flat-list `SEISMIC` from its title, and uses `TITLE` as the `OTHER_ACTIVITY` name.
This shows the titles in use, and how many are auto-generated ("Firm Well 1").

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, acts AS (
  SELECT a.activity_type, a.title
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , title         VARCHAR2(4000) PATH 'TITLE') a
  UNION ALL
  SELECT a.activity_type, a.title
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , title         VARCHAR2(4000) PATH 'TITLE') a
)
SELECT activity_type
     -- strip a trailing number so "Firm Well 1" and "Firm Well 2" group together
     , REGEXP_REPLACE(TRIM(title), '\s*\d+$', '') title_stem
     , COUNT(*) activities
FROM acts
WHERE activity_type IN ('SEISMIC', 'LEGACY', 'OTHER')
GROUP BY activity_type, REGEXP_REPLACE(TRIM(title), '\s*\d+$', '')
ORDER BY 1, 3 DESC, 2;
```

That query returned 878 rows (`LEGACY` titles are nearly all unique), so it was re-run in
summarised form, bucketing each title by pattern:

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, acts AS (
  SELECT a.activity_type, TRIM(a.title) title
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME//ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , title         VARCHAR2(4000) PATH 'TITLE') a
  WHERE a.activity_type IN ('SEISMIC', 'LEGACY', 'OTHER')
)
, bucketed AS (
  SELECT activity_type, title
       , CASE
           WHEN title IS NULL                                                   THEN '(empty)'
           WHEN REGEXP_LIKE(title, '^Legacy Obligations', 'i')                  THEN 'Legacy Obligations: ...'
           WHEN REGEXP_LIKE(title, '^Other Activity\s*\d*$', 'i')               THEN 'Other Activity n (default)'
           WHEN REGEXP_LIKE(title, '^Seismic Data\s*\d*$', 'i')                 THEN 'Seismic Data n (default)'
           WHEN REGEXP_LIKE(title, '^Shoot 2D Seismic Data\s*\d*$', 'i')        THEN 'Shoot 2D Seismic Data n'
           WHEN REGEXP_LIKE(title, '^Shoot 3D Seismic Data\s*\d*$', 'i')        THEN 'Shoot 3D Seismic Data n'
           WHEN REGEXP_LIKE(title, '^Obtain 2D Seismic Data\s*\d*$', 'i')       THEN 'Obtain 2D Seismic Data n'
           WHEN REGEXP_LIKE(title, '^Obtain 3D Seismic Data\s*\d*$', 'i')       THEN 'Obtain 3D Seismic Data n'
           WHEN REGEXP_LIKE(title, '^Reprocess 2D Seismic Data\s*\d*$', 'i')    THEN 'Reprocess 2D Seismic Data n'
           WHEN REGEXP_LIKE(title, '^Reprocess 3D Seismic Data\s*\d*$', 'i')    THEN 'Reprocess 3D Seismic Data n'
           WHEN REGEXP_LIKE(title, '(shoot|acquir|new).*2\s*d|2\s*d.*(shoot|acquir|new)', 'i') THEN 'Hand-typed: shoot/acquire 2D'
           WHEN REGEXP_LIKE(title, '(shoot|acquir|new).*3\s*d|3\s*d.*(shoot|acquir|new)', 'i') THEN 'Hand-typed: shoot/acquire 3D'
           WHEN REGEXP_LIKE(title, '(obtain|purchas|licen[cs]e).*[23]\s*d', 'i')               THEN 'Hand-typed: obtain 2D/3D'
           WHEN REGEXP_LIKE(title, 'reprocess', 'i')                            THEN 'Hand-typed: reprocess'
           WHEN REGEXP_LIKE(title, 'seismic', 'i')                              THEN 'Hand-typed: other seismic'
           WHEN REGEXP_LIKE(title, 'stud|evaluat|review|model|interpret|report|assess', 'i') THEN 'Hand-typed: studies'
           WHEN REGEXP_LIKE(title, 'well|drill', 'i')                           THEN 'Hand-typed: well/drilling'
           WHEN REGEXP_LIKE(title, 'csem|em survey|gravity|magnetic|bathymetr|core|sample', 'i') THEN 'Hand-typed: other surveys/data'
           ELSE 'Hand-typed: other'
         END bucket
  FROM acts
)
SELECT activity_type
     , bucket
     , COUNT(*)                           activities
     , COUNT(DISTINCT title)              distinct_titles
     , MIN(SUBSTR(title, 1, 80))          example_1
     , MAX(SUBSTR(title, 1, 80))          example_2
FROM bucketed
GROUP BY activity_type, bucket
ORDER BY 1, 3 DESC;
```

**Result (live, 29 October 2026):** `DRAFT` excluded.

| Type | Bucket | Activities | Distinct titles | Examples |
|---|---|---|---|---|
| LEGACY | Legacy Obligations: … | **882** | 868 | "Legacy Obligations: Original blocks (13/25), 14/21, 22, (23). Competed 1"; "… Original blocks Shell. Competed 1.0" |
| LEGACY | Other title (3 distinct) | **567** | 3 | "Initial Work Programme"; "Legacy - Not Fallow 2010" |
| LEGACY | (empty) | **333** | 0 | |
| OTHER | Other Activity n (default) | **939** | 8 | "Other Activity 1" … "Other Activity 8" |
| OTHER | Hand-typed: other | 3 | 3 | "11th Rnd obligations fulfilled 3F"; "Other Activity 3 - CONTINGENT COMMITMENT" |
| OTHER | Hand-typed: studies | 2 | 2 | "Geological Studies"; "Rock Physics Study" |
| OTHER | Hand-typed: other seismic | 1 | 1 | "Seismic Inversion" |
| SEISMIC | Seismic Data n (default) | **145** | 3 | "Seismic Data 1" … "Seismic Data 3" |
| SEISMIC | Reprocess 3D Seismic Data n | 143 | 4 | |
| SEISMIC | Obtain 3D Seismic Data n | 126 | 4 | |
| SEISMIC | Shoot 3D Seismic Data n | 88 | 3 | |
| SEISMIC | Obtain 2D Seismic Data n | 66 | 4 | |
| SEISMIC | Shoot 2D Seismic Data n | 61 | 3 | |
| SEISMIC | Reprocess 2D Seismic Data n | 33 | 4 | |
| SEISMIC | (misbucketed) | 1 | 1 | "Obtain 2D Seismic Data 1javascript:ss('action-add-new-3d-seismic',…" |

Totals match WP-S3: `LEGACY` 1,782, `OTHER` 945, `SEISMIC` 663.

**Reading:**

- **`SEISMIC`:**
  - **518 of 663 (78%) have a standard `PED009X` title** ("Shoot 3D Seismic Data 1" etc.) and
    classify cleanly by title. That includes the one with a `javascript:ss(...)` fragment pasted
    onto "Obtain 2D Seismic Data 1": a UI glitch. The bucketing put it under "shoot" because the
    fragment contains "new"; the title prefix makes it "obtain 2D". Strip it in the migration and
    report it.
  - **145 (22%) have the generic "Seismic Data n" title**, which says nothing about the kind of
    seismic (e.g. P1817's "Seismic Data 1" is "Reprocess 230 sq kms 3D seismic data…", WP-S3b).
    They need classifying **from the description**. WP-S4b does this.
- **`OTHER`: the title is useless as a category name.** 939 of 945 (99%) keep the default "Other
  Activity n". Plan M1's "`otherCategoryName` = `TITLE`" would give LMS hundreds of activities
  named "Other Activity 1". It needs a fixed name, with the detail carried by the description. Only
  6 are hand-typed:
  - "Geological Studies", "Rock Physics Study" and "Seismic Inversion" are usable names;
  - "11th Rnd obligations fulfilled 3F" is a status note typed into the title;
  - "Other Activity 3 - CONTINGENT COMMITMENT" puts a **commitment** in the title. That's the only
    commitment held anywhere on an `OTHER`.
- **`LEGACY`:**
  - **882 "Legacy Obligations: Original blocks … Competed n"** (offshore). The title holds the
    original block list and a "Competed" figure (probably the number of competing applications),
    and is almost unique per activity. It's worth keeping as a prefix to the description, not as a
    category name.
  - **567 share 3 programme-level titles**, including "Initial Work Programme" and "Legacy - Not
    Fallow 2010" (the 2010 fallow initiative). These label the programme, not the activity, so they
    don't add to the description. The third title isn't shown (it sorts between the two examples);
    WP-S4c lists all three.
  - **333 untitled** (the onshore ones, WP-S1d).

### WP-S4b. Proposed seismic classification

Classifies flat `SEISMIC` activities for plan M1:
- **by title**, where it is a standard `PED009X` title ("Shoot 3D…", "Obtain 2D…", "Reprocess…");
- otherwise **by description keywords**, for the 145 generic "Seismic Data n".

`multi_verb` counts descriptions that mention more than one of shoot/obtain/reprocess. Those take
the first matching category, and would be reported. Anything `UNCLASSIFIED` goes to
`OTHER_ACTIVITY` "Seismic" and is reported.

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, acts AS (
  SELECT TRIM(a.title) title, a.description
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY[ACTIVITY_TYPE = "SEISMIC"]' PASSING c.xml_data
         COLUMNS title       VARCHAR2(4000) PATH 'TITLE'
               , description VARCHAR2(3000) PATH 'substring(DESCRIPTION, 1, 3000)') a
)
, classified AS (
  SELECT title, description
       , CASE WHEN REGEXP_LIKE(title, '^(Shoot|Obtain|Reprocess) [23]D', 'i') THEN 'TITLE' ELSE 'DESCRIPTION' END basis
       , CASE
           WHEN REGEXP_LIKE(title, '^Shoot 2D', 'i')     THEN 'NEW_SHOOT_2_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(title, '^Shoot 3D', 'i')     THEN 'NEW_SHOOT_3_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(title, '^Obtain 2D', 'i')    THEN 'OBTAIN_EXISTING_2_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(title, '^Obtain 3D', 'i')    THEN 'OBTAIN_EXISTING_3_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(title, '^Reprocess', 'i')    THEN 'REPROCESS_SEISMIC_DATA'
           -- generic "Seismic Data n": classify from the description
           WHEN REGEXP_LIKE(description, 'reprocess', 'i') THEN 'REPROCESS_SEISMIC_DATA'
           WHEN REGEXP_LIKE(description, 'shoot|acquir', 'i') AND REGEXP_LIKE(description, '2\s*d', 'i') THEN 'NEW_SHOOT_2_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(description, 'shoot|acquir', 'i') AND REGEXP_LIKE(description, '3\s*d', 'i') THEN 'NEW_SHOOT_3_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(description, 'obtain|purchas|licen[cs]e', 'i') AND REGEXP_LIKE(description, '2\s*d', 'i') THEN 'OBTAIN_EXISTING_2_D_SEISMIC_DATA'
           WHEN REGEXP_LIKE(description, 'obtain|purchas|licen[cs]e', 'i') AND REGEXP_LIKE(description, '3\s*d', 'i') THEN 'OBTAIN_EXISTING_3_D_SEISMIC_DATA'
           ELSE 'UNCLASSIFIED'
         END category
       , CASE WHEN REGEXP_LIKE(description, 'shoot|acquir', 'i') THEN 1 ELSE 0 END
       + CASE WHEN REGEXP_LIKE(description, 'obtain|purchas', 'i') THEN 1 ELSE 0 END
       + CASE WHEN REGEXP_LIKE(description, 'reprocess', 'i') THEN 1 ELSE 0 END verbs
       , CASE WHEN REGEXP_LIKE(title, 'javascript:', 'i') THEN 1 ELSE 0 END junk_title
  FROM acts
)
SELECT basis
     , category
     , COUNT(*)                                    activities
     , SUM(CASE WHEN verbs > 1 THEN 1 ELSE 0 END)  multi_verb
     , SUM(junk_title)                             junk_titles
     , MIN(SUBSTR(description, 1, 80))             example_1
     , MAX(SUBSTR(description, 1, 80))             example_2
FROM classified
GROUP BY basis, category
ORDER BY 1 DESC, 2;
```

Expected: `TITLE` rows totalling 518, and `DESCRIPTION` rows totalling 145. The interesting
numbers are the `UNCLASSIFIED` count and `multi_verb`.

**Result (live, 29 October 2026):**

| Basis | Category | Activities | Multi-verb | Junk titles | Examples |
|---|---|---|---|---|---|
| TITLE | `NEW_SHOOT_2_D_SEISMIC_DATA` | 61 | 0 | 0 | "None \*\* see other work"; "Shoot 850 kms 2D seismic data." |
| TITLE | `NEW_SHOOT_3_D_SEISMIC_DATA` | 88 | 0 | 0 | "None. We believe we already have all relevant data"; "Shoot and Process to PreSDM 494 sq kms 3D seismic data." |
| TITLE | `OBTAIN_EXISTING_2_D_SEISMIC_DATA` | 67 | 13 | 1 | "None"; "Phase A: 3 years (TOTAL PHASE A: 3 YEARS) ⏎ Obtain a minimum of 2000 kms 2D seismi…" |
| TITLE | `OBTAIN_EXISTING_3_D_SEISMIC_DATA` | 126 | **44** | 0 | "None. We believe we already have all relevant data."; "Phase A: 4 years ⏎ Obtain 25 sq kms 3D Broadband seismic data." |
| TITLE | `REPROCESS_SEISMIC_DATA` | 176 | 1 | 0 | "Carry out Post Stack High Frequency Reprocessing on 150 sq kms 3D…"; "Reprocess 93 sq kms 3D seismic data." |
| DESCRIPTION | `NEW_SHOOT_2_D_SEISMIC_DATA` | 7 | 0 | 0 | "Shoot 140 kms 2D Geostreamer seismic data;" |
| DESCRIPTION | `NEW_SHOOT_3_D_SEISMIC_DATA` | 13 | 0 | 0 | "Shoot 1036 sq kms Long Offset 3D seismic data;" |
| DESCRIPTION | `OBTAIN_EXISTING_2_D_SEISMIC_DATA` | 19 | 0 | 0 | "Obtain 100 kms 2D Long Offset seismic data;" |
| DESCRIPTION | `OBTAIN_EXISTING_3_D_SEISMIC_DATA` | 28 | 0 | 0 | "Obtain 90 sq kms 3D seismic data;" |
| DESCRIPTION | `REPROCESS_SEISMIC_DATA` | 77 | **24** | 0 | "Obtain 130 sq kms of Long Offset 3D seismic data; Reprocess 850 sq kms of 3D sei…"; "Reprocess 90 sq kms 3D seismic data to PSTM and PreSDM;" |
| DESCRIPTION | `UNCLASSIFIED` | 1 | 0 | 0 | "Carry out a 3D seismic inversion of 50 sq kms 3d seismic data;" |

Totals: `TITLE` 518 (61 + 88 + 67 + 126 + 176; reprocess = WP-S4's 143 3D + 33 2D), `DESCRIPTION`
145. Both match WP-S4.

**Reading:**

- **The classification works.**
  - The 518 titled activities classify by title.
  - **144 of the 145 generic "Seismic Data n" classify from the description.** Each generic
    description is a single clear sentence ("Shoot 70 sq kms 3D seismic data;").
  - The one `UNCLASSIFIED` is a 3D seismic **inversion**. That matches the hand-typed `OTHER` title
    "Seismic Inversion" (WP-S4), so `OTHER_ACTIVITY` "Seismic inversion" fits.
  - The junk `javascript:` title is correctly under obtain 2D.
- **"None" activities (new finding):** descriptions such as "None", "None \*\* see other work" and
  "None. We believe we already have all relevant data". The application import turns every
  application field into an activity (plan, Lifecycle), so a field where the applicant committed to
  nothing still becomes an activity. **These aren't obligations.** Migrating them would put
  "Shoot 3D seismic: None" on the LMS timeline. Proposed: don't migrate them, and report them.
  WP-S5 counts them across every type.
- **Multi-verb descriptions: 82 (12% of `SEISMIC`).**
  - Mostly obtain-and-reprocess: 44 under the obtain 3D title, 13 under obtain 2D, and 24 generic
    descriptions classified as reprocess, e.g. "Obtain 130 sq kms of Long Offset 3D seismic data;
    Reprocess 850 sq kms of 3D…".
  - In LMS each is still one activity, with one category, so the second operation is only in the
    description.
  - For titled ones the title decides, which matches what the licensee was originally asked for.
    For description-classified ones, "reprocess" wins because it is checked first.
  - Report them all; splitting one activity into two isn't safe by rule.
- **Phase timing written into flat-list descriptions (new finding):** e.g. "Phase A: 3 years
  (TOTAL PHASE A: 3 YEARS) ⏎ Obtain…". These are flat-list activities (`SEISMIC` only exists in
  flat lists), but the text says which phase they belong to. If this is common, the phase can be
  read from the text and the activity placed `WITHIN_A_PHASE` instead of the Initial term (plan
  M3, Q11). WP-S5 counts the "Phase X:" prefixes.

### WP-S4c. The programme-level `LEGACY` titles

The three titles shared by 567 `LEGACY` activities (WP-S4), by licence type.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT TRIM(a.title) title, c.licence_type, COUNT(*) activities
FROM cur_wp c
   , XMLTABLE('/WORK_PROGRAMME//ACTIVITY[ACTIVITY_TYPE = "LEGACY"]' PASSING c.xml_data
       COLUMNS title VARCHAR2(4000) PATH 'TITLE') a
WHERE TRIM(a.title) IS NOT NULL
AND NOT REGEXP_LIKE(a.title, '^\s*Legacy Obligations', 'i')
GROUP BY TRIM(a.title), c.licence_type
ORDER BY 1, 2;
```

**Result:** not run.

## WP-S5. Descriptions

LMS requires a description (plan M2). This counts, per list and type:
- empty descriptions;
- descriptions that still hold the `PED009X` template placeholders (`***`);
- activities with competed blocks (plan D7, flat list only);
- **"None" activities**, which aren't obligations and aren't migrated (plan M2, from WP-S4b);
- **flat-list descriptions that start with a phase**, e.g. "Phase A: 3 years…" (plan M3 rule 3,
  from WP-S4b), with which phase.

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, acts AS (
  SELECT 'FLAT' list_kind, a.activity_type, a.title, a.description, a.competed_blocks
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type   VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , title           VARCHAR2(4000) PATH 'TITLE'
               , description     CLOB           PATH 'DESCRIPTION'
               , competed_blocks VARCHAR2(4000) PATH 'COMPETED_BLOCKS') a
  UNION ALL
  SELECT 'PHASE', a.activity_type, a.title, a.description, NULL
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/PHASE_LIST/PHASE/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS activity_type   VARCHAR2(40)   PATH 'ACTIVITY_TYPE'
               , title           VARCHAR2(4000) PATH 'TITLE'
               , description     CLOB           PATH 'DESCRIPTION') a
)
SELECT list_kind
     , activity_type
     , COUNT(*)                                                                           activities
     , SUM(CASE WHEN description IS NULL OR TRIM(DBMS_LOB.SUBSTR(description, 4000)) IS NULL
                THEN 1 ELSE 0 END)                                                        empty_description
     , SUM(CASE WHEN DBMS_LOB.INSTR(description, '***') > 0 THEN 1 ELSE 0 END)           template_placeholder
     , SUM(CASE WHEN TRIM(title) IS NULL THEN 1 ELSE 0 END)                               empty_title
     , SUM(CASE WHEN TRIM(competed_blocks) IS NOT NULL THEN 1 ELSE 0 END)                 with_competed_blocks
     , SUM(CASE WHEN REGEXP_LIKE(TRIM(DBMS_LOB.SUBSTR(description, 200)), '^None(\W|$)', 'i')
                THEN 1 ELSE 0 END)                                                        none_activity
     , SUM(CASE WHEN REGEXP_LIKE(TRIM(DBMS_LOB.SUBSTR(description, 200)), '^Phase A\s*:', 'i') THEN 1 ELSE 0 END) starts_phase_a
     , SUM(CASE WHEN REGEXP_LIKE(TRIM(DBMS_LOB.SUBSTR(description, 200)), '^Phase B\s*:', 'i') THEN 1 ELSE 0 END) starts_phase_b
     , SUM(CASE WHEN REGEXP_LIKE(TRIM(DBMS_LOB.SUBSTR(description, 200)), '^Phase C\s*:', 'i') THEN 1 ELSE 0 END) starts_phase_c
     , SUM(CASE WHEN NOT REGEXP_LIKE(TRIM(DBMS_LOB.SUBSTR(description, 200)), '^Phase [ABC]\s*:', 'i')
                 AND REGEXP_LIKE(DBMS_LOB.SUBSTR(description, 4000), 'Phase [ABC]', 'i')
                THEN 1 ELSE 0 END)                                                        phase_mentioned_later
     , MAX(DBMS_LOB.GETLENGTH(description))                                               max_description_length
FROM acts
GROUP BY ROLLUP(list_kind, activity_type)
ORDER BY 1 NULLS LAST, 2 NULLS LAST;
```

The `ROLLUP` adds a total per list and a grand total. `phase_mentioned_later` counts descriptions
that mention a phase somewhere other than at the start, where the phase can't safely be read by
rule.

Check `max_description_length` against the LMS `work_programme_activities.description` column
length.

**Result (live, 29 October 2026):** `DRAFT` excluded. Columns that are 0 everywhere are left out.
"Phase A/B/C" are the descriptions that start with that phase; "Later" means a phase mentioned
elsewhere in the description.

| List | Type | Activities | Empty desc | `***` | Empty title | Competed blocks | None | Phase A | Phase B | Phase C | Later | Max length |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| FLAT | `CONTINGENT_WELL` | 64 | | | | | 1 | | | | | 952 |
| FLAT | `DRILL_OR_DROP_WELL` | 620 | | 1 | | | 1 | | | **25** | | 164 |
| FLAT | `FIRM_WELL` | 110 | | | | | 1 | | | **4** | | 325 |
| FLAT | `LEGACY` | 1,782 | **13** | | **333** | **9** | 1 | | | | | 454 |
| FLAT | `OTHER` | 325 | | | | | | **27** | **14** | | | 519 |
| FLAT | `SEISMIC` | 663 | | | | | 4 | **27** | **11** | | | 171 |
| FLAT | *subtotal* | **3,564** | 13 | 1 | 333 | 9 | **8** | **54** | **25** | **29** | 0 | 952 |
| PHASE | `CONTINGENT_WELL` | 2 | | | | | | | | | 1 | 906 |
| PHASE | `OTHER` | 620 | | | | | | | | | 5 | 497 |
| PHASE | `SHOOT_3D_SEISMIC_CONTINGENT` | 34 | | | | | | | | | 24 | 732 |
| PHASE | other phase types (10) | 567 | | | | | | | | | | ≤ 595 |
| PHASE | *subtotal* | **1,223** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 30 | 906 |
| **ALL** | | **4,787** | **13** | **1** | **333** | **9** | **8** | **54** | **25** | **29** | **30** | **952** |

**Reading:**

- **Descriptions are in good shape.**
  - **13 empty**, all `LEGACY`. M2 defaults to the title, or to the category name if the title is
    empty too.
  - **1 leftover `***` template placeholder**, on a D&D well. Migrated as is and reported.
  - **9 competed blocks**, all on `LEGACY`. Append them to the description (M2).
  - **The longest is 952 characters** (a contingent-well clause). The LMS column is `TEXT`
    (`V27__work_programme_activity_data_model.sql`) and the form validator sets no maximum, so
    everything fits.
- **"None" activities: only 8** (4 seismic, 1 each contingent, D&D, firm, `LEGACY`). The WP-S4b
  examples made it look bigger than it is. Skip and report them (plan M2), so 4,779 activities
  remain.
- **Flat descriptions that start with a phase: 108** (54 Phase A, 25 Phase B, 29 Phase C):
  - `OTHER` and `SEISMIC` start with Phase A or B, which are the studies and seismic phases.
  - D&D and firm wells start with Phase C, the drilling phase.

  That's consistent with how phases are used, so plan M3 rule 3 can place these 108 in their
  phase. **That's only 3% of the 3,564 flat activities**, so the Initial-term default (rule 4)
  still covers nearly all of them.
- **No flat description mentions a phase anywhere other than at the start** (`Later` = 0), so the
  prefix rule misses nothing.
- **Possible refinement:** within one programme, perhaps only the first activity of each phase
  carries the "Phase X: n years" header, and the ones after it don't. If so, the activities
  following a prefixed one may belong to the same phase. **WP-S5b** checks this.
- **Phase lists: 30 mention a phase later in the text**, e.g. "contingent on the results of Phase
  A". Those activities are already anchored to their phase list's phase, so nothing changes.

### WP-S5b. Activities that follow a phase-prefixed one in the same programme

For flat programmes with at least one "Phase X:" description, this lists each activity in document
order with its own prefix (if any) and the most recent prefix before it. If unprefixed activities
regularly sit between prefixes, the header probably covers the activities below it.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type || plm.licence_no licence_reference, wpd.id wpd_id, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
  AND XMLEXISTS('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY[matches(normalize-space(DESCRIPTION), "^Phase [ABC]\s*:", "i")]'
                PASSING wpd.xml_data)
)
, acts AS (
  SELECT c.licence_reference, c.wpd_id, a.seq, a.activity_type
       , UPPER(REGEXP_SUBSTR(TRIM(a.desc_start), '^Phase ([ABC])\s*:', 1, 1, 'i', 1)) own_phase
       , SUBSTR(a.desc_start, 1, 60) desc_start
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/ACTIVITY_LIST/ACTIVITY' PASSING c.xml_data
         COLUMNS seq           FOR ORDINALITY
               , activity_type VARCHAR2(40)  PATH 'ACTIVITY_TYPE'
               , desc_start    VARCHAR2(200) PATH 'substring(normalize-space(DESCRIPTION), 1, 200)') a
)
, with_prev AS (
  SELECT a.*
       , LAST_VALUE(own_phase IGNORE NULLS) OVER (PARTITION BY wpd_id ORDER BY seq
                                                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) inherited_phase
  FROM acts a
)
SELECT CASE WHEN own_phase IS NOT NULL        THEN 'OWN PREFIX'
            WHEN inherited_phase IS NOT NULL  THEN 'AFTER A PREFIX'
            ELSE                                   'BEFORE ANY PREFIX' END position
     , activity_type
     , COUNT(*)                     activities
     , COUNT(DISTINCT wpd_id)       programmes
     , MIN(licence_reference)       example_licence
FROM with_prev
GROUP BY CASE WHEN own_phase IS NOT NULL        THEN 'OWN PREFIX'
              WHEN inherited_phase IS NOT NULL  THEN 'AFTER A PREFIX'
              ELSE                                   'BEFORE ANY PREFIX' END
       , activity_type
ORDER BY 1, 2;
```

`AFTER A PREFIX` is the number of extra activities that could inherit a phase. If it's large, a
sample of those programmes should be checked by eye before adopting the inherit rule.

**Result (live, 29 October 2026):**

| Position | Type | Activities | Programmes | Example |
|---|---|---|---|---|
| OWN PREFIX | `DRILL_OR_DROP_WELL` | 25 | 25 | P2310 |
| OWN PREFIX | `FIRM_WELL` | 4 | 4 | P2309 |
| OWN PREFIX | `OTHER` | 41 | 25 | P2310 |
| OWN PREFIX | `SEISMIC` | 38 | 20 | P2310 |
| AFTER A PREFIX | `OTHER` | 6 | 6 | P2311 |
| BEFORE ANY PREFIX | `OTHER` | 1 | 1 | P2312 |

**Reading:**

- **`OWN PREFIX` = 108**, matching WP-S5.
- **Nearly every activity in these programmes carries its own phase prefix.** Only 7 don't: 6 after
  a prefixed activity and 1 before any, all `OTHER`. So "Phase X: n years" is written on each
  activity, not as a heading over the ones below it. **No inherit rule:** those 7 fall back to
  the Initial term (plan M3 rule 4) and are reported.
- **It is one group of programmes:** about 25 `P` licences around P2309–P2312. Each has a D&D
  well in Phase C and studies and seismic in Phases A and B. That looks like a single licensing round that
  wrote phase-style work programmes but was configured with the flat list
  (`USE_GENERAL_ORGANISATION`). For these, rule 3 gives the same result as a phase-organised
  programme.

## WP-S6. Breakpoints and drill-or-drop wells

M3 proposes using the breakpoint as the due date of drill-or-drop wells. This shows how often the
two appear together.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, plm.licence_type || plm.licence_no licence_reference, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, per_wp AS (
  SELECT c.licence_type, c.licence_reference, x.wp_type
       , x.bp_years, x.bp_months, x.bp_days
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "DRILL_OR_DROP_WELL"])'
                          PASSING c.xml_data RETURNING CONTENT) AS NUMBER) dd_activities
       , TO_NUMBER(x.dd_obligated DEFAULT NULL ON CONVERSION ERROR)        dd_obligated_count
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME' PASSING c.xml_data
         COLUMNS wp_type      VARCHAR2(20) PATH 'WORK_PROGRAMME_TYPE'
               , bp_years     VARCHAR2(10) PATH 'BREAKPOINT/YEARS'
               , bp_months    VARCHAR2(10) PATH 'BREAKPOINT/MONTHS'
               , bp_days      VARCHAR2(10) PATH 'BREAKPOINT/DAYS'
               , dd_obligated VARCHAR2(10) PATH 'WELL_COUNT_LIST/WELL_COUNT[WELL_STATUS = "OBLIGATED"]/DRILL_OR_DROP_COUNT') x
)
SELECT licence_type
     , wp_type
     , CASE WHEN COALESCE(bp_years, bp_months, bp_days) IS NOT NULL THEN 'Y' ELSE 'N' END has_breakpoint
     , CASE WHEN dd_activities > 0 THEN 'Y' ELSE 'N' END                                   has_dd_activity
     , CASE WHEN NVL(dd_obligated_count, 0) > 0 THEN 'Y' ELSE 'N' END                      has_dd_well_count
     , COUNT(*) programmes
     , SUM(CASE WHEN NVL(bp_months, '0') != '0' OR NVL(bp_days, '0') != '0' THEN 1 ELSE 0 END) breakpoint_not_whole_years
FROM per_wp
GROUP BY licence_type, wp_type
       , CASE WHEN COALESCE(bp_years, bp_months, bp_days) IS NOT NULL THEN 'Y' ELSE 'N' END
       , CASE WHEN dd_activities > 0 THEN 'Y' ELSE 'N' END
       , CASE WHEN NVL(dd_obligated_count, 0) > 0 THEN 'Y' ELSE 'N' END
ORDER BY 1, 2, 3, 4, 5;
```

What to look at:
- `has_breakpoint = Y, has_dd_activity = N`: a breakpoint with no D&D well to give a date to. It
  still feeds the licence documents.
- `has_breakpoint = N, has_dd_activity = Y`: D&D wells that fall back to the flat-list or phase rule
  in M3.
- `breakpoint_not_whole_years`: these need the PEARS-vs-LMS date arithmetic check (plan M3, step 2).

**Result (live, 29 October 2026):** `DRAFT` excluded. BP = has a breakpoint, D&D = has a D&D
activity, Count = obligated D&D count in the well-count table above 0.

| Type | WP type | BP | D&D | Count | Programmes | BP not whole years |
|---|---|---|---|---|---|---|
| AL / DL / EXL / ML / PL | INITIAL | N | N | N | 4 / 5 / 38 / 7 / 28 | |
| P | EXTENSION | N | N | N | 2 | |
| P | FALLOW | N | N | N | 16 | |
| P | FALLOW | N | N | **Y** | 6 | |
| P | FALLOW | N | Y | Y | 5 | |
| P | INITIAL | N | N | N | 1,042 | |
| P | INITIAL | N | N | **Y** | **546** | |
| P | INITIAL | N | Y | N | 7 | |
| P | INITIAL | N | Y | Y | **598** | |
| P | INITIAL | Y | N | N | 3 | |
| P | INITIAL | Y | N | **Y** | 2 | 2 |
| P | INITIAL | Y | Y | N | 3 | |
| P | INITIAL | **Y** | **Y** | Y | **213** | 2 |
| PEDL | EXTENSION | N | Y | Y | 1 | |
| PEDL | INITIAL | N | N | N | 271 | |
| PEDL | INITIAL | N | N | **Y** | 16 | |
| PEDL | INITIAL | N | Y | Y | 46 | |
| PEDL | INITIAL | **Y** | **Y** | Y | **12** | |
| (no type) | INITIAL | N | Y | Y | 5 | |

**Reading (numbered licences only):**

- **Breakpoints are rare: 233 programmes**, all `P` (221) or `PEDL` (12) and all INITIAL. None
  of the onshore types other than `PEDL` has one.
- **Of the 885 programmes with a D&D activity, only 228 (26%) have a breakpoint** (216 `P`,
  12 `PEDL`). So plan M3 rule 2 gives D&D wells a real date on about a quarter of programmes. The
  other **657** (605 `P` INITIAL, 46 `PEDL`, 5 `P` FALLOW, 1 `PEDL` EXTENSION) stay on the phase
  or term default.
- **Breakpoints are almost all whole years:** only 4 of 233 have months or days. The PEARS-vs-LMS
  date arithmetic difference (days first vs months first) can only affect those 4. Check them
  in the comparison report.
- **5 programmes have a breakpoint but no D&D activity** (all `P`). There is nothing to give the
  date to. Since there's no write-back and the licence documents are already issued, the breakpoint
  is only history for these. Proposed: record it as a schedule comment ("Drill-or-drop breakpoint:
  n years from licence start") and report it.
- **570 programmes count an obligated D&D well in the well-count table but have no D&D activity**
  (546 `P` INITIAL, 6 `P` FALLOW, 2 `P` with a breakpoint, 16 `PEDL`). These are almost certainly
  the `LEGACY` programmes. The D&D well is inside the `LEGACY` summary ("DorD 3 yrs…", WP-S1d), and
  the well count was filled in separately. So the well-count table holds structured D&D information
  that the activities don't, for about 570 programmes. That matters for plan D12 and WP-S8.
- **10 programmes have a D&D activity but no obligated D&D count** (all `P` INITIAL): the well
  count wasn't kept up to date.
- The 5 unnumbered awards have D&D wells but no breakpoint; they're out of scope.

### WP-S6b. Breakpoint vs the schedule's `DD` event

The template schedule adds a placeholder `DD` event, but the breakpoint isn't used for its date.
This compares the two where both exist.

```sql
WITH cur_wp AS (
  SELECT wp.ped_lm_id, plm.licence_type || plm.licence_no licence_reference, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, bp AS (
  SELECT c.ped_lm_id, c.licence_reference
       , TO_NUMBER(x.y DEFAULT NULL ON CONVERSION ERROR) bp_y
       , TO_NUMBER(x.m DEFAULT NULL ON CONVERSION ERROR) bp_m
       , TO_NUMBER(x.d DEFAULT NULL ON CONVERSION ERROR) bp_d
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/BREAKPOINT' PASSING c.xml_data
         COLUMNS y VARCHAR2(10) PATH 'YEARS', m VARCHAR2(10) PATH 'MONTHS', d VARCHAR2(10) PATH 'DAYS') x
)
, dd AS (
  SELECT ped_lm_id, years, months, days, event_date
  FROM pedmgr.xview_ped_lic_schedule_events
  WHERE end_datetime IS NULL
  AND event_mnem = 'DD'
  AND NVL(dummy_event_flag, 'false') != 'true'
)
SELECT bp.licence_reference
     , bp.bp_y || 'y ' || bp.bp_m || 'm ' || bp.bp_d || 'd' breakpoint
     , dd.years || 'y ' || dd.months || 'm ' || dd.days || 'd' schedule_dd_offset
     , dd.event_date                                          schedule_dd_date
     , CASE WHEN dd.ped_lm_id IS NULL THEN 'NO DD EVENT'
            WHEN NVL(bp.bp_y, 0) = NVL(TO_NUMBER(dd.years DEFAULT NULL ON CONVERSION ERROR), 0)
             AND NVL(bp.bp_m, 0) = NVL(TO_NUMBER(dd.months DEFAULT NULL ON CONVERSION ERROR), 0)
             AND NVL(bp.bp_d, 0) = NVL(TO_NUMBER(dd.days DEFAULT NULL ON CONVERSION ERROR), 0)
            THEN 'SAME' ELSE 'DIFFERENT' END comparison
FROM bp
LEFT JOIN dd ON dd.ped_lm_id = bp.ped_lm_id
WHERE COALESCE(bp.bp_y, bp.bp_m, bp.bp_d) IS NOT NULL
ORDER BY 5, 1;
```

If they mostly agree, the migrated `DD` schedule event (schedule plan: `OTHER_ACTIVITY`
"Drill-or-drop") and a D&D well anchored on the breakpoint would duplicate each other on the LMS
timeline. That needs a decision.

**Result (live, 29 October 2026):** 233 rows, one per breakpoint (matching WP-S6). Summarised:

| Comparison | Programmes | Licence types | Detail |
|---|---|---|---|
| **SAME** | **119** (51%) | 118 `P`, 1 `PEDL` (PEDL297) | Nearly all 2 years. They cluster by round on shared `DD` dates: 10/01/2013 (P1856–P1883), 01/01/2015 (P1940–P2090), 20/12/2015 (P2002–P2106) and 01/12/2016 (P2160–P2217). |
| **NO DD EVENT** | **97** (42%) | all `P` (P1407–P2496) | Mostly 2 years, some 1, 3 or 4. 4 have months: P1655 and P1662 (2y 4m), P1727 (2y 3m), P1729 (2y 6m). **P1647 is `0y 0m 0d`**, an empty breakpoint. |
| **DIFFERENT** | **17** | 6 `P`, 11 `PEDL` | In every case the schedule's `DD` is **later** than the breakpoint. |

The 17 `DIFFERENT`:

| Licence | Breakpoint | Schedule `DD` | Schedule `DD` date |
|---|---|---|---|
| P1811, P1814 | 2y | 2y 9m | 10/10/2013 |
| P1879 | 2y | 2y 6m | 10/07/2013 |
| P2007 | 2y | 2y 3m | 01/04/2015 |
| P2071 | 4y | 4y 11m | 26/08/2018 |
| P2101 | 2y | 4y | 20/12/2017 |
| PEDL257, 260, 262, 263, 268, 298, 302, 339 | 2y | 5y | 21/07/2021 |
| PEDL293, PEDL295 | 2y | 8y | 21/07/2024 |
| PEDL267 | 2y | 14y | 21/07/2030 |

**Reading:**

- **Where both exist, they usually agree (119 of 136).** Where they differ, the schedule's `DD` is
  always later. The `P` ones are 3–11 months later (one by 2 years), which looks like a
  drill-or-drop extension recorded on the schedule but not on the work programme. The 11 `PEDL`
  are all from one round (licence start 21/07/2016) with a 2-year breakpoint against a 5-, 8- or
  14-year `DD`, so the schedule there reflects later decisions. **The schedule is maintained and
  the work programme breakpoint isn't, so the schedule's date should win.**
- **97 `P` licences have a breakpoint but no `DD` event on the schedule.** For these, the
  breakpoint is the only record of the drill-or-drop decision date, and it's worth migrating. P1647's
  zero breakpoint should be ignored.
- **Duplication on the LMS timeline:** the schedule migration already brings every `DD` event in
  as an `OTHER_ACTIVITY` "Drill-or-drop" schedule event (schedule plan, Mapping). If the D&D well
  activity were also dated from the breakpoint, the 119 `SAME` licences would show the
  drill-or-drop decision twice on the same date. The 17 `DIFFERENT` would show two different dates,
  one of them out of date. See plan M3 rule 2 and Open question 13.
- **Months in breakpoints:** the 4 from WP-S6 are all `NO DD EVENT` rows (P1655, P1662, P1727,
  P1729). They are the only ones where PEARS-vs-LMS date arithmetic could differ. With no days, the
  results only differ if the licence starts on the last day of a short month (`ADD_MONTHS` snaps to
  the month end), so the comparison report checks those 4.

## WP-S7. Phase timings vs the schedule

The work programme holds phase lengths (whole years) that are also on the schedule (plan D15). M3
anchors phase-organised activities on the migrated phases, so the two must agree. This compares the
cumulative work programme phase end (sum of `TIMINGS` up to each phase) with the current schedule's
`P` event offset for the same phase. Only INITIAL programmes.

```sql
WITH cur_wp AS (
  SELECT wp.ped_lm_id, plm.licence_type, plm.licence_type || plm.licence_no licence_reference, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
  AND XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_TYPE/text()' PASSING wpd.xml_data RETURNING CONTENT)
              AS VARCHAR2(20)) = 'INITIAL'
)
, wp_phases AS (
  SELECT c.ped_lm_id, c.licence_type, c.licence_reference
       , REPLACE(p.phase_type, 'Phase ') phase
       , TO_NUMBER(p.timings DEFAULT NULL ON CONVERSION ERROR) timings
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/PHASE_LIST/PHASE' PASSING c.xml_data
         COLUMNS phase_type VARCHAR2(20) PATH 'TYPE'
               , timings    VARCHAR2(10) PATH 'TIMINGS') p
)
, wp_cum AS (
  SELECT w.*
       , SUM(timings) OVER (PARTITION BY ped_lm_id ORDER BY phase) wp_phase_end_years
  FROM wp_phases w
)
, sched_p AS (
  SELECT ped_lm_id, phase
       , TO_NUMBER(years  DEFAULT NULL ON CONVERSION ERROR) y
       , TO_NUMBER(months DEFAULT NULL ON CONVERSION ERROR) m
       , TO_NUMBER(days   DEFAULT NULL ON CONVERSION ERROR) d
  FROM pedmgr.xview_ped_lic_schedule_events
  WHERE end_datetime IS NULL
  AND event_mnem = 'P'
  AND NVL(dummy_event_flag, 'false') != 'true'
)
SELECT w.licence_type
     , CASE WHEN s.ped_lm_id IS NULL AND NOT EXISTS (SELECT 1 FROM sched_p s2 WHERE s2.ped_lm_id = w.ped_lm_id)
                 THEN 'SCHEDULE HAS NO PHASES'
            WHEN s.ped_lm_id IS NULL THEN 'PHASE MISSING FROM SCHEDULE'
            WHEN NVL(s.m, 0) != 0 OR NVL(s.d, 0) != 0 THEN 'SCHEDULE NOT WHOLE YEARS'
            WHEN s.y = w.wp_phase_end_years THEN 'SAME'
            ELSE 'DIFFERENT' END comparison
     , COUNT(*)                     phases
     , COUNT(DISTINCT w.ped_lm_id)  licences
     , MIN(w.licence_reference)     example
FROM wp_cum w
LEFT JOIN sched_p s ON s.ped_lm_id = w.ped_lm_id AND s.phase = w.phase
GROUP BY w.licence_type
       , CASE WHEN s.ped_lm_id IS NULL AND NOT EXISTS (SELECT 1 FROM sched_p s2 WHERE s2.ped_lm_id = w.ped_lm_id)
                   THEN 'SCHEDULE HAS NO PHASES'
              WHEN s.ped_lm_id IS NULL THEN 'PHASE MISSING FROM SCHEDULE'
              WHEN NVL(s.m, 0) != 0 OR NVL(s.d, 0) != 0 THEN 'SCHEDULE NOT WHOLE YEARS'
              WHEN s.y = w.wp_phase_end_years THEN 'SAME'
              ELSE 'DIFFERENT' END
ORDER BY 1, 2;
```

`DIFFERENT` is expected where a phase was extended on the schedule (e.g. P2522's Phase A, 2y + a
12-month extension) without the work programme being updated. Those still migrate: M3 anchors on
the schedule's phase, so the activity follows the extension. The case to look at is
`PHASE MISSING FROM SCHEDULE`, where activities sit in a phase the schedule doesn't have. That is
M3's fall-back to the Initial term.

**Result (live, 29 October 2026):** `DRAFT` excluded.

| Type | Comparison | Phases | Licences | Example |
|---|---|---|---|---|
| P | **SAME** | **450** | 220 | P2345 |
| P | DIFFERENT | 90 | 50 | P2348 |
| P | SCHEDULE NOT WHOLE YEARS | 50 | 29 | P2356 |
| P | SCHEDULE HAS NO PHASES | 1 | 1 | P2643 |
| (no type) | SAME | 2 | 1 | |
| (no type) | SCHEDULE HAS NO PHASES | 9 | 4 | |

Licences can appear in more than one row (one phase the same, another extended).

**Reading:**

- **No `PHASE MISSING FROM SCHEDULE`.** Wherever the current schedule has phases, every work
  programme phase exists on it. So plan M3 rule 1 (`WITHIN_A_PHASE`) works for every
  phase-organised activity, and none falls back to the Initial term.
- **591 work programme phases on numbered licences:** 450 (76%) end exactly where the schedule's
  phase ends, and 140 differ:
  - **90 by whole years**;
  - **50 by months or days**, where the schedule phase isn't whole years, e.g. a 6- or 12-month
    extension.

  These are phase extensions recorded on the schedule only. They don't matter for the migration:
  activities anchor to the schedule's phase and follow it. They do confirm the work programme's
  own `TIMINGS` are award-time values that weren't kept up to date, so they shouldn't be migrated
  as data (plan D15).
- **P2643: work programme phases, but no phases on the schedule.** P2643 is one of the schedule
  survey's "no terms" licences (pattern E, L9), which the schedule migration doesn't migrate. So
  its work programme has no LMS schedule to go on. It is reported with the other work-programme-
  but-no-schedule licences (plan Q8, WP-S10).
- The unnumbered awards are out of scope.

## WP-S8. Well-count matrix vs activities

The well-count matrix is maintained by hand, separately from the activities (plan D12). If it
always agrees with the activities, LMS can derive it and nothing is lost. This compares the
`OBLIGATED` and `COMPLETED` counts with the number of well activities of each kind.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, plm.licence_type || plm.licence_no licence_reference, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, per_wp AS (
  SELECT c.licence_type, c.licence_reference
       , TO_NUMBER(x.ob_firm DEFAULT NULL ON CONVERSION ERROR) ob_firm
       , TO_NUMBER(x.ob_cont DEFAULT NULL ON CONVERSION ERROR) ob_cont
       , TO_NUMBER(x.ob_dd   DEFAULT NULL ON CONVERSION ERROR) ob_dd
       , TO_NUMBER(x.co_firm DEFAULT NULL ON CONVERSION ERROR) co_firm
       , TO_NUMBER(x.co_cont DEFAULT NULL ON CONVERSION ERROR) co_cont
       , TO_NUMBER(x.co_dd   DEFAULT NULL ON CONVERSION ERROR) co_dd
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "FIRM_WELL"])'                                 PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_firm
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "CONTINGENT_WELL"])'                           PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_cont
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "DRILL_OR_DROP_WELL"])'                        PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_dd
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "FIRM_WELL"          and ACTIVITY_STATUS = "COMPLETE"])' PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_firm_done
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "CONTINGENT_WELL"    and ACTIVITY_STATUS = "COMPLETE"])' PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_cont_done
       , XMLCAST(XMLQUERY('count(//ACTIVITY[ACTIVITY_TYPE = "DRILL_OR_DROP_WELL" and ACTIVITY_STATUS = "COMPLETE"])' PASSING c.xml_data RETURNING CONTENT) AS NUMBER) act_dd_done
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/WELL_COUNT_LIST' PASSING c.xml_data
         COLUMNS ob_firm VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "OBLIGATED"]/FIRM_COUNT'
               , ob_cont VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "OBLIGATED"]/CONTINGENT_COUNT'
               , ob_dd   VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "OBLIGATED"]/DRILL_OR_DROP_COUNT'
               , co_firm VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "COMPLETED"]/FIRM_COUNT'
               , co_cont VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "COMPLETED"]/CONTINGENT_COUNT'
               , co_dd   VARCHAR2(10) PATH 'WELL_COUNT[WELL_STATUS = "COMPLETED"]/DRILL_OR_DROP_COUNT') x
)
SELECT licence_type
     , COUNT(*) programmes_with_matrix
     , SUM(CASE WHEN NVL(ob_firm, 0) = act_firm      AND NVL(ob_cont, 0) = act_cont      AND NVL(ob_dd, 0) = act_dd
                THEN 1 ELSE 0 END) obligated_matches_activities
     , SUM(CASE WHEN NVL(co_firm, 0) = act_firm_done AND NVL(co_cont, 0) = act_cont_done AND NVL(co_dd, 0) = act_dd_done
                THEN 1 ELSE 0 END) completed_matches_activities
     , SUM(CASE WHEN NVL(ob_firm, 0) + NVL(ob_cont, 0) + NVL(ob_dd, 0) > 0 AND act_firm + act_cont + act_dd = 0
                THEN 1 ELSE 0 END) counts_but_no_well_activities
     , SUM(CASE WHEN NVL(ob_firm, 0) + NVL(ob_cont, 0) + NVL(ob_dd, 0) = 0 AND act_firm + act_cont + act_dd > 0
                THEN 1 ELSE 0 END) well_activities_but_no_counts
FROM per_wp
GROUP BY ROLLUP(licence_type)
ORDER BY GROUPING(licence_type), 1;
```

Well activities can be described as more than one well ("Two firm wells…"), so a mismatch doesn't
prove an error. Pull a sample of mismatches before concluding anything. `WAIVED`, `DROPPED` and
`WAIVER_DECLINED` counts are left out here; add them if the obligated and completed rows mostly
agree.

**Result (live, 29 October 2026):** `DRAFT` excluded.

| Type | Programmes | Obligated matches | Completed matches | Counts, no well activities | Well activities, no counts |
|---|---|---|---|---|---|
| AL / DL / EXL / ML / PL | 4 / 5 / 38 / 7 / 28 | all | all | 0 | 0 |
| **P** | 2,443 | 1,577 | 1,709 | **844** | 4 |
| **PEDL** | 346 | 306 | 329 | **28** | 0 |
| (no type) | 5 | 5 | 5 | 0 | 0 |
| **ALL** | **2,876** | **1,970** | **2,125** | **872** | **4** |

**Reading:**

- **Every programme has a well-count table.** On the onshore types other than `PEDL` it is all
  zeros and there are no well activities, so they "match" trivially.
- **872 programmes (30%) count wells but have no well activities:** 844 `P` and 28 `PEDL`.
  - That's more than WP-S6's 570 because it covers firm and contingent wells as well as D&D.
  - These are almost certainly the `LEGACY` programmes. The wells are inside the `LEGACY` summary
    text ("2F 1000m", "(T) 1F to 2060m…", WP-S1d, WP-S3b), and the counts were kept separately.
  - **For these licences, the table is the only structured record of how many wells were obligated
    and what happened to them** (completed, dropped, waived). Plan D12.
- **Where both exist, they nearly always agree.** Excluding the 872 and the 4 below, the obligated
  counts disagree on only about 30 programmes (`P` ≈ 18, `PEDL` 12). That's easily explained by
  activities describing more than one well ("Three Firm Wells…", WP-S3b). So where well activities
  exist, the table adds nothing the activities don't.
- **4 `P` programmes have well activities but an all-zero table:** the table was never filled in.
- **Next step if the table is to be migrated:** WP-S8b gives the full status breakdown (obligated,
  completed, dropped, waived, waiver declined × firm/contingent/D&D) for the 872, to see what
  migrating it would carry.

### WP-S8b. Well counts on programmes with no well activities

The full well-count table for the 872 programmes from WP-S8, summed by status and well type.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
  AND NOT XMLEXISTS('//ACTIVITY[ACTIVITY_TYPE = ("FIRM_WELL", "CONTINGENT_WELL", "DRILL_OR_DROP_WELL")]' PASSING wpd.xml_data)
)
SELECT c.licence_type
     , w.well_status
     , COUNT(*)                                                      programmes
     , SUM(TO_NUMBER(w.firm DEFAULT 0 ON CONVERSION ERROR))          firm
     , SUM(TO_NUMBER(w.cont DEFAULT 0 ON CONVERSION ERROR))          contingent
     , SUM(TO_NUMBER(w.dd   DEFAULT 0 ON CONVERSION ERROR))          drill_or_drop
FROM cur_wp c
   , XMLTABLE('/WORK_PROGRAMME/WELL_COUNT_LIST/WELL_COUNT' PASSING c.xml_data
       COLUMNS well_status VARCHAR2(20) PATH 'WELL_STATUS'
             , firm        VARCHAR2(10) PATH 'FIRM_COUNT'
             , cont        VARCHAR2(10) PATH 'CONTINGENT_COUNT'
             , dd          VARCHAR2(10) PATH 'DRILL_OR_DROP_COUNT') w
WHERE c.licence_type IN ('P', 'PEDL')
GROUP BY c.licence_type, w.well_status
ORDER BY 1, DECODE(w.well_status, 'OBLIGATED', 1, 'COMPLETED', 2, 'DROPPED', 3, 'WAIVED', 4, 'WAIVER_DECLINED', 5, 9);
```

This includes the programmes whose table is all zeros, so `programmes` counts every no-well-activity
programme, not only the 872. The sums are what matter.

**Result:** not run.

## WP-S9. Notes

M5 rebuilds the status history from the status notes. M6 sends notes to two places: activity notes
become comments on the LMS activity, and everything else (`GENERAL` notes, notes whose activity
isn't migrated or no longer exists) goes to the planned LMS **schedule comments** area above the
schedule (plan, Target). This counts each kind, and checks that the notes are complete enough to
rely on. WP-S9c counts the notes whose activity has gone.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.id wpd_id, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, notes AS (
  SELECT c.licence_type, c.wpd_id, n.*
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/NOTES_LIST/NOTE' PASSING c.xml_data
         COLUMNS note_type     VARCHAR2(40)  PATH 'TYPE'
               , activity_id   VARCHAR2(20)  PATH 'ACTIVITY_ID'
               , new_status    VARCHAR2(40)  PATH 'NEW_ACTIVITY_STATUS'
               , note_datetime VARCHAR2(40)  PATH 'DATETIME'
               , created_by    VARCHAR2(200) PATH 'CREATED_BY'
               , text_start    VARCHAR2(20)  PATH 'substring(normalize-space(DESCRIPTION), 1, 20)') n
)
SELECT licence_type
     , CASE WHEN note_type = 'GENERAL' THEN 'GENERAL'
            WHEN new_status IS NOT NULL THEN 'STATUS CHANGE'
            ELSE 'OTHER: ' || NVL(note_type, '(empty)') END note_kind
     , COUNT(*)                                                  notes
     , COUNT(DISTINCT wpd_id)                                    programmes
     , SUM(CASE WHEN activity_id IS NULL THEN 1 ELSE 0 END)      no_activity_id
     , SUM(CASE WHEN note_datetime IS NULL THEN 1 ELSE 0 END)    no_datetime
     , SUM(CASE WHEN created_by IS NULL THEN 1 ELSE 0 END)       no_author
     , SUM(CASE WHEN text_start IS NULL THEN 1 ELSE 0 END)       no_text
     , MIN(note_datetime)                                        earliest
FROM notes
GROUP BY licence_type
       , CASE WHEN note_type = 'GENERAL' THEN 'GENERAL'
              WHEN new_status IS NOT NULL THEN 'STATUS CHANGE'
              ELSE 'OTHER: ' || NVL(note_type, '(empty)') END
ORDER BY 1, 2;
```

**Result (live, 29 October 2026):** `DRAFT` excluded. Every `no_datetime`, `no_author` and
`no_text` count is 0, so those columns are left out.

| Type | General notes | on programmes | Status-change notes | on programmes | Earliest general | Earliest status change |
|---|---|---|---|---|---|---|
| AL | 4 | 4 | 2 | 2 | **2011-07-12 17:47:26** | 2014-03-24 |
| DL | 7 | 5 | 2 | 2 | **2011-07-12 17:47:26** | 2017-06-28 |
| EXL | 41 | 37 | 13 | 12 | **2011-07-12 17:47:26** | 2012-06-12 |
| ML | 7 | 7 | 2 | 2 | **2011-07-12 17:47:26** | 2011-10-04 |
| **P** | **1,569** | 932 | **2,649** | 1,098 | 2010-10-20 | 2010-10-18 |
| **PEDL** | **424** | 314 | **197** | 147 | 2011-06-18 | 2010-12-14 |
| PL | 38 | 28 | 18 | 18 | **2011-07-12 17:47:26** | 2012-10-24 |
| **Total** | **2,090** | | **2,883** | | | |

**Reading:**

- **4,973 notes, and the data is complete.** Every note has a date, an author and some text.
  There is no third kind: every note is either `GENERAL` or a status change.
- **General notes never have an activity ID, and status-change notes always do.** So plan M6 splits
  cleanly:
  - **2,090 general notes → schedule comments.**
  - **2,883 status-change notes → event comments and status history** (WP-S9c checks how many of
    their activities still exist).
- **The `LEGACY` bulk job wrote a general note too.** On `AL`, `DL`, `EXL`, `ML` and `PL`, the
  earliest general note is 12/07/2011 17:47:26, the second the bulk job ran (WP-S1c, WP-S2).
  - Nearly every programme on those types has one (37 of 38 `EXL`, all 28 `PL`, all 7 `ML`).
  - If it is boilerplate (e.g. "Legacy work programme imported"), migrating it would put hundreds of
    identical comments on LMS schedules. **WP-S9d** shows the most common general note texts.
- **General notes are common on `PEDL`:** 314 of 346 programmes (91%), against 932 of 2,443 on `P`
  (38%). Some of that will be the bulk-job note.
- **Status-change notes are fewer than status changes.** There are 2,883 notes, but 3,289 activities
  are not `NOT_STARTED` (WP-S3), and an activity can change status more than once. So many
  status changes have no note. WP-S9b counts the activities whose current status has none.
- **Unnumbered awards have no notes.**

### WP-S9b. Activities whose current status has no status note

M5 falls back to one status row dated at the version's `start_datetime` for these. A large number
means the status history mostly can't be rebuilt.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT c.licence_type
     , a.activity_status
     , COUNT(*) activities
     , SUM(CASE WHEN a.status_notes = 0 THEN 1 ELSE 0 END) without_status_note
FROM cur_wp c
   , XMLTABLE('for $a in /WORK_PROGRAMME//ACTIVITY
               return <A>{$a/ACTIVITY_STATUS}
                         <N>{count(/WORK_PROGRAMME/NOTES_LIST/NOTE[ACTIVITY_ID = $a/ACTIVITY_ID
                                                                   and normalize-space(NEW_ACTIVITY_STATUS) != ""])}</N></A>'
       PASSING c.xml_data
       COLUMNS activity_status VARCHAR2(40) PATH 'ACTIVITY_STATUS'
             , status_notes    NUMBER       PATH 'N') a
GROUP BY c.licence_type, a.activity_status
ORDER BY 1, 2;
```

`NOT_STARTED` activities without a note are normal: it is the starting status.

**Result (live, 29 October 2026):** `DRAFT` excluded.

| Type | Status | Activities | Without a status note |
|---|---|---|---|
| AL / DL / EXL / ML / PL | COMPLETE | 2 / 2 / 12 / 2 / 18 | 0 |
| AL / DL / EXL / ML / PL | NOT_STARTED | 2 / 3 / 26 / 5 / 10 | all |
| **P** | **COMPLETE** | **2,474** | **741** (30%) |
| P | DROPPED | 426 | 0 |
| P | FULL_WAIVER | 115 | 0 |
| P | IN_PROGRESS | 41 | **17** |
| P | NOT_STARTED | 1,170 | 1,157 |
| P | PART_WAIVER | 13 | 0 |
| P | WAIVER_DECLINED | 16 | 0 |
| PEDL | COMPLETE | 67 | 0 |
| PEDL | DROPPED | 40 | 0 |
| PEDL | FULL_WAIVER | 55 | 0 |
| PEDL | IN_PROGRESS | 3 | 0 |
| PEDL | NOT_STARTED | 260 | 259 |
| PEDL | PART_WAIVER | 3 | 0 |
| (no type) | NOT_STARTED | 22 | 22 |

Totals reconcile with WP-S3: 4,787 activities, 2,577 `COMPLETE`.

**Reading:**

- **The status history is nearly complete.** Every `DROPPED`, `FULL_WAIVER`, `PART_WAIVER`
  and `WAIVER_DECLINED` activity has a status note, as does every onshore and `PEDL` `COMPLETE`.
  Plan M5 can rebuild their history from the notes.
- **The gaps are all on `P`: 741 `COMPLETE` (30% of `P` `COMPLETE`) and 17 `IN_PROGRESS` have no
  status note.**
  - Both lists share one activity definition in `PED009X` (`type-activity-list`), whose status
    selector always writes a note (`action-actvity-status-changed`). So it isn't a phase-list
    gap.
  - More likely, these activities were **already `COMPLETE` when the programme was first saved**,
    e.g. a programme created after the work had been done, or by an import. Or a note was removed
    without reverting the status.
  - **WP-S9e** checks this against the version history. It also gives a better date for these
    activities' single status row: the first version in which the activity had that status,
    rather than the current version's date.
- **14 `NOT_STARTED` activities have a status note** (13 `P`, 1 `PEDL`). They were changed and
  then changed back, so their history has more than one entry.
- **PEARS defect found while checking:** `action-remove-note` reverts the activity's status only
  for flat-list activities (it updates `ACTIVITY_LIST/ACTIVITY[…]`, not the phase lists). Removing
  a status note on a phase-list activity leaves the new status in place with no note.

### WP-S9e. When did the note-less `P` statuses first appear?

For each `P` activity whose current status is `COMPLETE` or `IN_PROGRESS` with no status note, this
finds two dates in the version history:
- the first version the activity appears in;
- the first version in which it has its current status.

If they're the same version, the status was set when the activity was created. Otherwise it was
changed later without a note.

```sql
WITH cur_wp AS (
  SELECT wp.id wp_id, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND plm.licence_type = 'P'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, gaps AS (
  SELECT c.wp_id, a.activity_id, a.activity_type, a.activity_status, a.list_kind
  FROM cur_wp c
     , XMLTABLE('for $a in /WORK_PROGRAMME//ACTIVITY[ACTIVITY_STATUS = ("COMPLETE", "IN_PROGRESS")]
                 where not(/WORK_PROGRAMME/NOTES_LIST/NOTE[ACTIVITY_ID = $a/ACTIVITY_ID
                                                          and normalize-space(NEW_ACTIVITY_STATUS) != ""])
                 return <A>{$a/ACTIVITY_ID, $a/ACTIVITY_TYPE, $a/ACTIVITY_STATUS}
                           <K>{if ($a/parent::ACTIVITY_LIST/parent::PHASE) then "PHASE" else "FLAT"}</K></A>'
         PASSING c.xml_data
         COLUMNS activity_id     VARCHAR2(20) PATH 'ACTIVITY_ID'
               , activity_type   VARCHAR2(40) PATH 'ACTIVITY_TYPE'
               , activity_status VARCHAR2(40) PATH 'ACTIVITY_STATUS'
               , list_kind       VARCHAR2(5)  PATH 'K') a
)
, hist AS (
  SELECT wpd.ped_wp_id wp_id, wpd.start_datetime, h.activity_id, h.activity_status
  FROM pedmgr.ped_work_programme_details wpd
     , XMLTABLE('/WORK_PROGRAMME//ACTIVITY' PASSING wpd.xml_data
         COLUMNS activity_id     VARCHAR2(20) PATH 'ACTIVITY_ID'
               , activity_status VARCHAR2(40) PATH 'ACTIVITY_STATUS') h
  WHERE wpd.record_status = 'ACTIVE'
  AND wpd.ped_wp_id IN (SELECT wp_id FROM gaps)
)
, per_act AS (
  SELECT g.list_kind, g.activity_type, g.activity_status
       , MIN(h.start_datetime)                                                         first_seen
       , MIN(CASE WHEN h.activity_status = g.activity_status THEN h.start_datetime END) first_with_status
  FROM gaps g
  JOIN hist h ON h.wp_id = g.wp_id AND h.activity_id = g.activity_id
  GROUP BY g.list_kind, g.activity_type, g.activity_status, g.wp_id, g.activity_id
)
SELECT list_kind
     , activity_type
     , activity_status
     , CASE WHEN first_with_status = first_seen THEN 'STATUS FROM CREATION'
            ELSE 'CHANGED LATER, NO NOTE' END how
     , COUNT(*)                                 activities
     , TO_CHAR(MIN(first_with_status), 'YYYY')  earliest_year
     , TO_CHAR(MAX(first_with_status), 'YYYY')  latest_year
FROM per_act
GROUP BY list_kind, activity_type, activity_status
       , CASE WHEN first_with_status = first_seen THEN 'STATUS FROM CREATION'
              ELSE 'CHANGED LATER, NO NOTE' END
ORDER BY 1, 2, 3, 4;
```

The `activities` column should total 758 (741 + 17). It reads every version of the affected
programmes, so it may take a minute.

**Result (live, 29 October 2026):**

| List | Type | Status | How | Activities | Year |
|---|---|---|---|---|---|
| FLAT | `LEGACY` | COMPLETE | STATUS FROM CREATION | 741 | 2011 |
| FLAT | `LEGACY` | IN_PROGRESS | STATUS FROM CREATION | 17 | 2011 |

**Reading:**

- **All 758 are `LEGACY` activities that the 12/07/2011 bulk job created with their status already
  set.** None was changed later without a note, so the PEARS status history has no hidden gaps:
  every status change made in `PED009X` since 2010 wrote a note.
- **Their only date is the import date (12/07/2011), not when the work was done.** The bulk job
  carried the status over from the pre-2010 records, and the real completion date isn't in PEARS.
  Plan M5 dates their single status row from the first version, which is 12/07/2011. It also adds
  a comment so it isn't read as the completion date, e.g. "Status carried over from legacy records
  on 12/07/2011".
- Onshore `LEGACY` `COMPLETE` activities do have notes (WP-S9b), and their earliest status-change
  notes are from 2011–2017 (WP-S9). So the bulk job seems to have carried statuses over only on
  `P`. Onshore, it created everything `NOT_STARTED`, and the statuses were updated by hand later.

### WP-S9c. Notes by destination in LMS

Splits the notes in current versions by where M6 sends them:
- `EVENT COMMENT`: the activity exists;
- `SCHEDULE COMMENT (general)`: a `GENERAL` note or no activity ID;
- `SCHEDULE COMMENT (activity gone)`: the `ACTIVITY_ID` matches no activity in the version, e.g.
  the activity was deleted after the note was written.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT c.licence_type
     , n.destination
     , COUNT(*) notes
FROM cur_wp c
   , XMLTABLE('for $n in /WORK_PROGRAMME/NOTES_LIST/NOTE
               return <N>{
                 if ($n/TYPE = "GENERAL" or normalize-space($n/ACTIVITY_ID) = "")
                   then "SCHEDULE COMMENT (general)"
                 else if (/WORK_PROGRAMME//ACTIVITY[ACTIVITY_ID = $n/ACTIVITY_ID])
                   then "EVENT COMMENT"
                 else "SCHEDULE COMMENT (activity gone)"
               }</N>'
       PASSING c.xml_data
       COLUMNS destination VARCHAR2(40) PATH '.') n
GROUP BY ROLLUP(c.licence_type, n.destination)
ORDER BY 1 NULLS LAST, 2 NULLS LAST;
```

Notes on the 6 duplicate-programme licences (WP-L2) also go to schedule comments while those are
unresolved, but they are counted as `EVENT COMMENT` here.

**Result (live, 29 October 2026):** run with an extra first test, `CREATED_BY = "Migration
Process"` → `EXCLUDED (system note)`, after WP-S9f.

| Type | Event comment | Schedule comment (general) | Excluded (system) | Activity gone | Total |
|---|---|---|---|---|---|
| AL | 2 | 0 | 4 | 0 | 6 |
| DL | 2 | 2 | 5 | 0 | 9 |
| EXL | 13 | 4 | 37 | 0 | 54 |
| ML | 2 | 0 | 7 | 0 | 9 |
| **P** | **2,649** | **609** | 960 | 0 | 4,218 |
| **PEDL** | 197 | 172 | 252 | 0 | 621 |
| PL | 18 | 10 | 28 | 0 | 56 |
| **Total** | **2,883** | **797** | **1,293** | **0** | **4,973** |

**Reading:**

- **Every total reconciles:** 1,293 excluded and 797 general (WP-S9f), and 2,883 event comments
  (WP-S9).
- **No note's activity has gone.** Every status note points to an activity that still exists in
  the current version, so every one becomes a comment on its LMS activity. M6's "activity gone"
  route is kept as a safeguard but never used on live.
- **Onshore types other than `PEDL` have hardly any human general notes:** 16 in all, and `AL` and
  `ML` none. Their general notes were nearly all the bulk job's.

### WP-S9d. The most common general note texts

Checks whether general notes include boilerplate, e.g. from the 12/07/2011 bulk job (WP-S9), that
shouldn't be migrated as hundreds of identical schedule comments. Groups by the first 100
characters of the note, and shows the 30 most common.

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, notes AS (
  SELECT n.text_start, n.note_datetime, n.created_by
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/NOTES_LIST/NOTE[TYPE = "GENERAL"]' PASSING c.xml_data
         COLUMNS text_start    VARCHAR2(400) PATH 'substring(normalize-space(DESCRIPTION), 1, 100)'
               , note_datetime VARCHAR2(40)  PATH 'DATETIME'
               , created_by    VARCHAR2(200) PATH 'CREATED_BY') n
)
SELECT *
FROM (
  SELECT text_start
       , COUNT(*)                     notes
       , MIN(SUBSTR(note_datetime, 1, 10)) first_date
       , MAX(SUBSTR(note_datetime, 1, 10)) last_date
       , COUNT(DISTINCT created_by)   authors
       , MIN(created_by)              example_author
  FROM notes
  GROUP BY text_start
  ORDER BY COUNT(*) DESC
)
WHERE ROWNUM <= 30;
```

A text with hundreds of notes, one author and a single date is boilerplate. Hand-written notes will
show as count 1 or 2.

**Decided (29 October 2026):** system-generated notes, such as the bulk job's, are **not migrated**
(plan M6). This query identifies them, so the migration can exclude them by text and timestamp.

**Result (live, 29 October 2026):** top 30 general note texts (first 100 characters).

| Text (start) | Notes | Dates | Author |
|---|---|---|---|
| Start Date: 01-Jul-2008 | 96 | 2011-07-12 | **Migration Process** |
| [[MIGRATED DATA]] - Required to drill 1 drill or drop well(s).1 drill or drop well completed. | 75 | 2011-07-12 | Migration Process |
| DROPPED | 62 | 2011-07-12 | Migration Process |
| Legacy. | 43 | 2011-08-24 – 2018-04-04 | Jen Brzozowska (2 authors) |
| Start Date: 08-Sep-2000 | 37 | 2011-07-12 | Migration Process |
| REL 2009 | 33 | 2011-07-12 | Migration Process |
| Start Date: 18-Mar-1998 | 28 | 2011-07-12 | Migration Process |
| Remove 'long offset' | 25 | 2015-11-07 – 08 | Ms Toni Harvey |
| Start Date: 01-Feb-2002 / 01-Oct-2004 | 22 / 22 | 2011-07-12 | Migration Process |
| 1st May 2010 | 17 | 2011-07-12 | Migration Process |
| Licence is determining with outstanding work commitments. Decision made by Director on 15 July 2024. | 17 (+11 without the full stop) | 2024-07-18 | Mr Alan Poole |
| Start Date: 04-Apr-1996 | 16 | 2011-07-12 | Migration Process |
| [[MIGRATED DATA]] - Required to drill 1 drill or drop well(s). | 16 | 2011-07-12 | Migration Process |
| Rep. Recd. | 12 | 2011-07-12 | Migration Process |
| [[MIGRATED DATA]] - Required to drill 1 firm well(s). 1 firm well completed. | 10 | 2011-07-12 | Migration Process |
| Legacy | 8 | 2012–2013 | Mrs Jen Brzozowska |
| Start Date: 01-Jul-2003 | 8 | 2011-07-12 | Migration Process |
| Rel? 2009 | 8 | 2011-07-12 | Migration Process |
| Legacy. No WP as such. | 7 | 2012–2017 | Jen Brzozowska (2 authors) |
| Start Date: 01-Oct-1991 PEDL Conversion: 09-Jan-1997 | 7 | 2011-07-12 | Migration Process |
| Licence with 2 yr extension to initial term is GdF deal where a well on this licence will count for … | 7 | 2013-11-14 | Ms Toni Harvey |
| [[MIGRATED DATA]] - Cont. / Waived | 6 | 2011-07-12 | Migration Process |
| [[MIGRATED DATA]] - Required to drill 1 firm well(s). | 6 | 2011-07-12 | Migration Process |
| Assignments: Area 1, Area 2 Start Date: 08-Sep-2000 | 5 | 2011-07-12 | Migration Process |
| change to drill a well into the abandoned colliery workings. | 5 | 2016-03-28 | Ms Toni Harvey |
| [[MIGRATED DATA]] - Required to drill 1 contingent well(s). 1 contingent well waived. | 5 | 2011-07-12 | Migration Process |
| Promote Licence not continuing. | 5 | 2011-01-24 | Jen Brzozowska |
| Alternative WP 28/03/24-Bathymetric survey in the Liverpool Bay area of the East Irish Sea, publish … | 4 | 2024-11-27 | Ms Sonya Croke |

**Reading:**

- **The bulk-job notes are authored "Migration Process" and dated 2011-07-12.** That's the
  system user of the 12/07/2011 legacy migration, so they're easy to identify exactly, by author.
- **But they aren't empty boilerplate. They carry pre-2010 data, in three kinds:**
  1. **"Start Date: dd-Mon-yyyy"** (with variants such as "PEDL Conversion: …" and
     "Assignments: Area 1, Area 2"). The licence start date, which the migrated schedule already
     holds. The variants add a little (the PEDL conversion date, area assignments).
  2. **"[[MIGRATED DATA]] - Required to drill n … well(s). n … completed / waived"** and
     "[[MIGRATED DATA]] - Cont. / Waived". A text version of the well obligations and their
     outcomes: **the same information as the well-count table** (WP-S8, WP-L6).
  3. **Old-system remarks:** "DROPPED", "REL 2009", "Rel? 2009", "Rep. Recd.", "1st May 2010".
     Short comments typed into the pre-2010 system and carried over (relinquished 2009, report
     received, and so on). They're terse, but they are the only record of those remarks.
- **Repeated notes written by people are real and must stay**, for example:
  - "Licence is determining with outstanding work commitments. Decision made by Director on 15 July
    2024" (Alan Poole, 28 licences, 2024);
  - "Remove 'long offset'" (Toni Harvey, 25 licences, 2015);
  - "Legacy." / "Legacy. No WP as such." (Jen Brzozowska);
  - "Promote Licence not continuing."

  A rule based on "the same text on many programmes" would wrongly drop these, so the rule must
  be **by author ("Migration Process")**, not by repetition.
- **Decided (29 October 2026): all "Migration Process" notes, and any similar system-authored
  notes, are left out of LMS**, whatever their content (plan M6). The rule is **by author**, so the
  repeated notes written by people above still migrate. WP-S9f lists every note author, to find any
  other system authors.

### WP-S9f. Note authors

Every `CREATED_BY` on notes in current versions, with counts and date range. It finds system
authors other than "Migration Process" for the exclusion rule. The table lists the top 40 authors,
which should include every system author: they write many notes, often on one day.

```sql
WITH cur_wp AS (
  SELECT wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
, notes AS (
  SELECT n.created_by, n.note_type, SUBSTR(n.note_datetime, 1, 10) note_date
  FROM cur_wp c
     , XMLTABLE('/WORK_PROGRAMME/NOTES_LIST/NOTE' PASSING c.xml_data
         COLUMNS created_by    VARCHAR2(200) PATH 'CREATED_BY'
               , note_type     VARCHAR2(40)  PATH 'TYPE'
               , note_datetime VARCHAR2(40)  PATH 'DATETIME') n
)
SELECT *
FROM (
  SELECT created_by
       , COUNT(*)                                                    notes
       , SUM(CASE WHEN note_type = 'GENERAL' THEN 1 ELSE 0 END)      general_notes
       , COUNT(*) - SUM(CASE WHEN note_type = 'GENERAL' THEN 1 ELSE 0 END) status_notes
       , COUNT(DISTINCT note_date)                                   distinct_days
       , MIN(note_date)                                              first_date
       , MAX(note_date)                                              last_date
  FROM notes
  GROUP BY created_by
  ORDER BY COUNT(*) DESC
)
WHERE ROWNUM <= 40;
```

A system author shows as a name that isn't a person (e.g. "Migration Process"), usually with many
notes on very few distinct days.

**Result (live, 29 October 2026):** all 20 authors (fewer than the 40 limit).

| Author | Notes | General | Status | Days | First | Last |
|---|---|---|---|---|---|---|
| Mrs Jen Brzozowska | 1,576 | 189 | 1,387 | 356 | 2010-10-18 | 2019-08-08 |
| **Migration Process** | **1,293** | **1,293** | 0 | **1** | 2011-07-12 | 2011-07-12 |
| Jen Brzozowska | 433 | 155 | 278 | 137 | 2010-10-20 | 2018-05-08 |
| Ms Toni Harvey | 350 | 123 | 227 | 140 | 2010-10-28 | 2023-07-05 |
| Mr Duncan Bruce | 254 | 86 | 168 | 134 | 2019-05-24 | 2026-09-01 |
| Mr John Gordon | 186 | 59 | 127 | 89 | 2019-05-28 | 2023-09-21 |
| Dr Jon Seedhouse | 162 | 7 | 155 | 39 | 2022-09-08 | 2026-09-24 |
| Dr Matt Farris | 152 | 4 | 148 | 57 | 2019-02-27 | 2024-11-28 |
| Mr Andrew Upton | 117 | 60 | 57 | 39 | 2016-03-23 | 2019-02-12 |
| Mr Mark Quint | 111 | 2 | 109 | 27 | 2016-05-03 | 2026-02-10 |
| Mr Alan Poole | 71 | 61 | 10 | 25 | 2023-11-01 | 2026-06-30 |
| Miss Georgina Malas | 68 | 11 | 57 | 39 | 2020-04-17 | 2025-06-12 |
| Miss Alana Finlayson | 60 | 3 | 57 | 21 | 2019-03-22 | 2021-04-28 |
| Miss Rhiannon Jones | 39 | 4 | 35 | 16 | 2022-01-20 | 2023-06-14 |
| Mrs Rhiannon Jones | 38 | 1 | 37 | 13 | 2023-08-17 | 2026-01-05 |
| Ms Kirsty Simpson | 24 | 3 | 21 | 10 | 2023-10-16 | 2026-10-07 |
| Ms Sonya Croke | 22 | 22 | 0 | 4 | 2020-05-26 | 2024-11-27 |
| Mr Daniel Ralph | 11 | 3 | 8 | 8 | 2024-09-11 | 2025-04-25 |
| Mike Hawkins | 4 | 2 | 2 | 3 | 2014-03-03 | 2015-01-20 |
| Miss Laura Bardsley | 2 | 2 | 0 | 1 | 2023-06-26 | 2023-06-26 |
| **Total** | **4,973** | **2,090** | **2,883** | | | |

Totals reconcile with WP-S9.

**Reading:**

- **"Migration Process" is the only system author:** 1,293 general notes, no status notes, all
  on 12/07/2011. The other 19 are people. So the exclusion rule (plan M6) is exactly
  `CREATED_BY = 'Migration Process'`.
- **What migrates:**
  - **797 general notes → schedule comments** (2,090 − 1,293);
  - **2,883 status notes → event comments** and status history;
  - **3,680 notes** in all, out of 4,973.

  62% of general notes were system notes, so the schedule comments area gets far less than WP-S9
  suggested.
- **The same person appears under two names:** "Mrs Jen Brzozowska" / "Jen Brzozowska" and
  "Miss Rhiannon Jones" / "Mrs Rhiannon Jones". PEARS stores the user's full name as it was when the
  note was written (`:{user}/FULL_NAME`), not a user id. LMS keeps the name as written, in the
  comment text (plan M6), so the variants stay. They aren't mapped to LMS users.
- **One person wrote 41% of all notes:** Jen Brzozowska, 2,009 under both names, 2010–2019.
  Recent maintenance (2019–2026) is spread across about ten people.

## WP-S10. Work programme vs schedule coverage

Which licences have a work programme but no PEARS schedule, and the reverse, by licence status.
A work programme can only be migrated onto a migrated schedule, so a licence with a work programme
and no schedule (or one the schedule migration skips, e.g. pattern E) has nowhere to put it.

```sql
WITH latest_dp AS (
  SELECT licence_type, licence_no, ped_dp_id
  FROM (
    SELECT pdp.licence_type, pdp.licence_no, pdp.id ped_dp_id
         , ROW_NUMBER() OVER (PARTITION BY pdp.licence_type, pdp.licence_no
                              ORDER BY pdp.position_datetime DESC, pdp.position_sequence DESC) rn
    FROM pedmgr.ped_data_points pdp
    WHERE pdp.ped_sim_id = 0
  )
  WHERE rn = 1
)
, current_status AS (
  SELECT plm.id ped_lm_id, xplc.licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
)
SELECT plm.licence_type
     , NVL(cs.licence_status, '(no status)') licence_status
     , CASE WHEN EXISTS (SELECT 1
                         FROM pedmgr.ped_work_programme wp
                         JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
                         WHERE wp.ped_lm_id = plm.id
                         AND wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE' AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT')
            THEN 'Y' ELSE 'N' END has_work_programme
     , CASE WHEN EXISTS (SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = plm.id)
            THEN 'Y' ELSE 'N' END has_schedule
     , COUNT(*) licences
FROM pedmgr.ped_licence_master plm
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
WHERE plm.licence_type IS NOT NULL
GROUP BY plm.licence_type, NVL(cs.licence_status, '(no status)')
       , CASE WHEN EXISTS (SELECT 1
                           FROM pedmgr.ped_work_programme wp
                           JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
                           WHERE wp.ped_lm_id = plm.id
                           AND wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE' AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT')
              THEN 'Y' ELSE 'N' END
       , CASE WHEN EXISTS (SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = plm.id)
              THEN 'Y' ELSE 'N' END
ORDER BY 1, 2, 3, 4;
```

Then cross-check the `has_work_programme = Y` licences against the schedule migration's
"No terms in PEARS" list (schedule survey L9) and any other licences it skips.

**Result (live, 29 October 2026):**

| Type | Status | WP | Schedule | Licences |
|---|---|---|---|---:|
| `A` | (no status) | N | N | 103 |
| `AL` | (no status) | N | N | 6 |
| `AL` | (no status) | Y | N | 1 |
| `AL` | E | Y | Y | 1 |
| `AL` | S | Y | Y | 1 |
| `AL` | X | Y | Y | 1 |
| `B` | (no status) | N | N | 13 |
| `CE` | E | N | Y | 1 |
| `DL` | E | Y | Y | 4 |
| `DL` | X | Y | Y | 1 |
| `EXL` | (no status) | N | N | 258 |
| `EXL` | (no status) | Y | N | 23 |
| `EXL` | E | Y | Y | 5 |
| `EXL` | R | Y | Y | 1 |
| `EXL` | S | Y | N | 2 |
| `EXL` | S | Y | Y | 6 |
| `ML` | (no status) | N | N | 25 |
| `ML` | E | Y | Y | 7 |
| `NA` | (no status) | N | N | 1 |
| `P` | (no status) | N | Y | 2 |
| `P` | (no status) | Y | N | 10 |
| `P` | (no status) | Y | Y | 18 |
| `P` | E | N | Y | 33 |
| `P` | E | Y | Y | 312 |
| `P` | R | N | N | 3 |
| `P` | R | Y | N | 2 |
| `P` | R | Y | Y | 3 |
| `P` | S | N | N | 148 |
| `P` | S | N | Y | 15 |
| `P` | S | Y | N | 951 |
| `P` | S | Y | Y | 931 |
| `P` | X | N | Y | 1 |
| `P` | X | Y | Y | 210 |
| `PEDL` | (no status) | N | N | 4 |
| `PEDL` | (no status) | Y | N | 50 |
| `PEDL` | (no status) | Y | Y | 5 |
| `PEDL` | E | Y | Y | 36 |
| `PEDL` | R | Y | Y | 13 |
| `PEDL` | S | Y | N | 70 |
| `PEDL` | S | Y | Y | 124 |
| `PEDL` | X | Y | Y | 42 |
| `PL` | (no status) | N | N | 238 |
| `PL` | E | Y | Y | 21 |
| `PL` | S | Y | N | 2 |
| `PL` | S | Y | Y | 2 |
| `PL` | X | Y | Y | 3 |
| `XL` | (no status) | N | N | 211 |

The totals agree with WP-S1 (`P` 2,437 with a work programme) and schedule survey S2 (`P` 1,525
with a schedule).

**Reading:**

- **No extant licence has a work programme without a schedule.** The 1,111 work-programme-only
  licences are 1,025 surrendered, 2 revoked and 84 with no current status:

  | Type | S | R | (no status) | Total |
  |---|---:|---:|---:|---:|
  | `P` | 951 | 2 | 10 | 963 |
  | `PEDL` | 70 | | 50 | 120 |
  | `EXL` | 2 | | 23 | 25 |
  | `PL` | 2 | | | 2 |
  | `AL` | | | 1 | 1 |
  | **Total** | **1,025** | **2** | **84** | **1,111** |

  They have no schedule to sit on, so they are left out and reported (plan Q8). Nearly all are
  surrendered `P` licences, which matches the schedule survey's finding that the licences without a
  schedule are historic.
- **1,747 licences have both** and are the work programme migration's population: `P` 1,474,
  `PEDL` 220, `PL` 26, `EXL` 12, `ML` 7, `DL` 5, `AL` 3. Of these, 386 are extant.
- **Every extant licence has a schedule.** There is no `E` in any `has_schedule = N` row.
- **52 licences have a schedule but no work programme.** 34 are extant: 33 `P` and 1 `CE`. They
  get a schedule with no activities, which LMS allows. This is not a migration issue, but the
  licensing team may want to know that 33 extant production licences have no work programme
  recorded in PEARS.
- **107 licences with a work programme have no current status** (84 without a schedule, 23 with
  one). No current data point usually means the licence was never raised or its records end
  before PEARS. WP-S10b breaks them down.
- **`A`, `B`, `NA` and `XL` (328 licences) are master rows only.** A follow-up check (live, 29
  October 2026) found no data points for any of them: `A` 103 (nos 1–536), `B` 13 (25–262), `NA` 1
  (1), `XL` 211 (1–217). `XL` is "Exploration - Landward" in the `DEVUK_LICTYP` map-set, the same
  label as `EXL`, so it looks like an older onshore exploration code. `A`, `B` and `NA` aren't
  defined anywhere in PEARS. None has a work programme or a schedule, and none exists in LMS, so
  they are out of scope.

## WP-S10b. Licences with a work programme but no current status

Splits the 107 work programme licences with no current status (WP-S10) by whether they have any
data points at all, whether they have a schedule, and the programme status, to tell unraised
awards (`DRAFT_FINAL`, no data points) apart from licences whose records just end.

```sql
WITH latest_dp AS (
  SELECT licence_type, licence_no, ped_dp_id
  FROM (
    SELECT pdp.licence_type, pdp.licence_no, pdp.id ped_dp_id
         , ROW_NUMBER() OVER (PARTITION BY pdp.licence_type, pdp.licence_no
                              ORDER BY pdp.position_datetime DESC, pdp.position_sequence DESC) rn
    FROM pedmgr.ped_data_points pdp
    WHERE pdp.ped_sim_id = 0
  )
  WHERE rn = 1
)
, current_status AS (
  SELECT plm.id ped_lm_id, xplc.licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
)
, no_status_wp AS (
  SELECT plm.id ped_lm_id, plm.licence_type, plm.licence_no
       , XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)) wp_status
  FROM pedmgr.ped_licence_master plm
  JOIN pedmgr.ped_work_programme wp ON wp.ped_lm_id = plm.id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
  AND plm.licence_type IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM current_status cs WHERE cs.ped_lm_id = plm.id)
)
, flagged AS (
  SELECT nsw.ped_lm_id, nsw.licence_type, nsw.licence_no
       , CASE WHEN EXISTS (SELECT 1 FROM pedmgr.ped_data_points pdp
                           WHERE pdp.licence_type = nsw.licence_type AND pdp.licence_no = nsw.licence_no
                           AND pdp.ped_sim_id = 0)
              THEN 'Y' ELSE 'N' END has_data_points
       , CASE WHEN EXISTS (SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = nsw.ped_lm_id)
              THEN 'Y' ELSE 'N' END has_schedule
       , NVL(nsw.wp_status, '(none)') wp_status
  FROM no_status_wp nsw
)
SELECT f.licence_type, f.has_data_points, f.has_schedule, f.wp_status
     , COUNT(DISTINCT f.ped_lm_id) licences
     , MIN(f.licence_type || f.licence_no) example
FROM flagged f
GROUP BY f.licence_type, f.has_data_points, f.has_schedule, f.wp_status
ORDER BY 1, 2, 3, 4;
```

**Result (live, 29 October 2026):**

| Type | Data points | Schedule | Programme status | Licences | Example |
|---|---|---|---|---:|---|
| `AL` | N | N | `NOT_STARTED` | 1 | AL5 |
| `EXL` | N | N | `NOT_STARTED` | 23 | EXL142 |
| `P` | N | N | `COMPLETE` | 1 | P1382 |
| `P` | N | N | `NOT_STARTED` | 9 | P1261 |
| `P` | N | Y | `DRAFT_FINAL` | 18 | P2488 |
| `PEDL` | N | N | `NOT_STARTED` | 50 | PEDL10 |
| `PEDL` | N | Y | `DRAFT_FINAL` | 5 | PEDL286 |

**Reading:**

- **None of the 107 has any data points**, so none has licence data in PEARS. The total is 107, so
  no licence has more than one current programme here.
- **The 23 with a schedule are all `DRAFT_FINAL`** (18 `P`, 5 `PEDL`): awards issued for raise
  whose licences haven't been raised yet. Issuing the award creates the template schedule and the
  `DRAFT_FINAL` programme; the licence data only appears at raise. They are part of the WP-L1
  count (76 `P`, 15 `PEDL`), which therefore splits into **23 not yet raised** and **68 raised**.
  Whether the 23 migrate depends on whether the schedule migration creates an LMS licence for a
  licence with no data points. If it doesn't, their programmes have nowhere to go and should be
  created in LMS when the licence is raised (plan Q7, Q9).
- **The 84 without a schedule are out of scope.** They're mostly old onshore licences (`PEDL`,
  `EXL`, `AL`) and older `P` numbers whose programme was saved (`NOT_STARTED`, one `COMPLETE`) but
  which have no licence data or schedule in PEARS. They're reported with the other
  work-programme-only licences (WP-S10).

## WP-S11. Soft-deleted and multiple-current rows

`delete_work_programme` leaves `status_control = 'C'` on deleted rows, and some PEARS readers
(`TEMPLATE_SCHEDULE`, `LICENCE_CREATE`, the raise reports) don't filter on `record_status`. This
counts deleted rows still marked current, and programmes with no current row or more than one.

```sql
SELECT plm.licence_type
     , SUM(CASE WHEN x.c_active  = 1 THEN 1 ELSE 0 END) one_current
     , SUM(CASE WHEN x.c_active  = 0 AND x.c_draft > 0 THEN 1 ELSE 0 END) draft_only
     , SUM(CASE WHEN x.c_active  = 0 AND x.c_draft = 0 AND x.c_deleted > 0 THEN 1 ELSE 0 END) deleted_only
     , SUM(CASE WHEN x.c_active  = 0 AND x.c_draft = 0 AND x.c_deleted = 0 THEN 1 ELSE 0 END) no_current_row
     , SUM(CASE WHEN x.c_active  > 1 THEN 1 ELSE 0 END) multiple_current
     , SUM(CASE WHEN x.c_active >= 1 AND x.c_deleted > 0 THEN 1 ELSE 0 END) current_and_deleted
FROM (
  SELECT d.ped_wp_id id, d.ped_lm_id
       , SUM(CASE WHEN d.status_control = 'C' AND d.record_status = 'ACTIVE'  AND d.wp_status != 'DRAFT' THEN 1 ELSE 0 END) c_active
       , SUM(CASE WHEN d.status_control = 'C' AND d.record_status = 'ACTIVE'  AND d.wp_status  = 'DRAFT' THEN 1 ELSE 0 END) c_draft
       , SUM(CASE WHEN d.status_control = 'C' AND d.record_status = 'DELETED' THEN 1 ELSE 0 END) c_deleted
  FROM (
    SELECT wp.id ped_wp_id, wp.ped_lm_id, wpd.status_control, wpd.record_status
         , NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') wp_status
    FROM pedmgr.ped_work_programme wp
    LEFT JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  ) d
  GROUP BY d.ped_wp_id, d.ped_lm_id
) x
JOIN pedmgr.ped_licence_master plm ON plm.id = x.ped_lm_id
GROUP BY ROLLUP(plm.licence_type)
ORDER BY GROUPING(plm.licence_type), 1;
```

`deleted_only` programmes are excluded from the migration. `multiple_current` would be a data
error (skip and report), as in the schedule survey's S2.

**Result (live, 29 October 2026):**

| Type | One current | Draft only | Deleted only | No current row | Multiple current | Current and deleted |
|---|---:|---:|---:|---:|---:|---:|
| `AL` | 4 | 0 | 0 | 0 | 0 | 0 |
| `DL` | 5 | 0 | 0 | 0 | 0 | 0 |
| `EXL` | 38 | 0 | 0 | 0 | 0 | 0 |
| `ML` | 7 | 0 | 0 | 0 | 0 | 0 |
| `P` | 2,443 | 0 | 0 | 0 | 0 | 0 |
| `PEDL` | 346 | 0 | 0 | 0 | 0 | 0 |
| `PL` | 28 | 0 | 0 | 0 | 0 | 0 |
| (no type) | 5 | 8 | 0 | 0 | 0 | 0 |
| **Total** | **2,876** | **8** | **0** | **0** | **0** | **0** |

**Reading:**

- **The soft-delete risk (D19) doesn't occur in live data.** No programme has a deleted row still
  marked current, and none is deleted-only. The `record_status` filter stays in the migration as a
  safeguard, but nothing is excluded by it today.
- **Every programme has exactly one current row.** No multiple-current errors and no programmes
  with no current row.
- **The 8 `DRAFT`-only programmes are all on unnumbered awards** (the "no type" row, with the 5
  `DRAFT_FINAL` ones from WP-S1). They are excluded.
- The programme counts are a little above the licence counts in WP-S1 (`P` 2,443 programmes vs
  2,437 licences, `PEDL` 346 vs 340, `EXL` 38 vs 37). These are the licences with more than one
  programme (an INITIAL plus a FALLOW or EXTENSION, or the duplicate INITIALs in WP-L2), which
  WP-S1c covers.

## WP-S12. `ACTIVITY_ID` uniqueness and stability

The plan (D17) relies on `ACTIVITY_ID` to match an activity across versions (`originalEventId`). This
checks that it is always set and unique within a version, and that an ID never changes type
between versions.

```sql
WITH all_acts AS (
  SELECT wpd.ped_wp_id, wpd.id wpd_id, wpd.start_datetime, a.activity_id, a.activity_type
  FROM pedmgr.ped_work_programme_details wpd
     , XMLTABLE('/WORK_PROGRAMME//ACTIVITY' PASSING wpd.xml_data
         COLUMNS activity_id   VARCHAR2(20) PATH 'ACTIVITY_ID'
               , activity_type VARCHAR2(40) PATH 'ACTIVITY_TYPE') a
  WHERE wpd.record_status = 'ACTIVE'
)
SELECT 'Activities, all versions'                                     check_name, COUNT(*) cnt FROM all_acts
UNION ALL
SELECT 'No ACTIVITY_ID', COUNT(*) FROM all_acts WHERE TRIM(activity_id) IS NULL
UNION ALL
SELECT 'Duplicate ACTIVITY_ID within a version', COUNT(*)
FROM (SELECT wpd_id, activity_id FROM all_acts WHERE activity_id IS NOT NULL
      GROUP BY wpd_id, activity_id HAVING COUNT(*) > 1)
UNION ALL
SELECT 'ACTIVITY_ID on more than one programme', COUNT(*)
FROM (SELECT activity_id FROM all_acts WHERE activity_id IS NOT NULL
      GROUP BY activity_id HAVING COUNT(DISTINCT ped_wp_id) > 1)
UNION ALL
SELECT 'ACTIVITY_ID whose type changes between versions', COUNT(*)
FROM (SELECT ped_wp_id, activity_id FROM all_acts WHERE activity_id IS NOT NULL
      GROUP BY ped_wp_id, activity_id HAVING COUNT(DISTINCT activity_type) > 1);
```

The flat list and the phase lists share one ID sequence, so `//ACTIVITY` is right here. An ID on
more than one programme would point to copied XML, e.g. a FALLOW programme created from an INITIAL
one.

**Result (live, 29 October 2026):**

| Check | Count |
|---|---:|
| Activities, all versions | 43,388 |
| No `ACTIVITY_ID` | 0 |
| Duplicate `ACTIVITY_ID` within a version | 0 |
| `ACTIVITY_ID` on more than one programme | 0 |
| `ACTIVITY_ID` whose type changes between versions | 0 |

**Reading:** `ACTIVITY_ID` is a reliable key. Across 43,388 activity rows in every active version,
each has an ID, IDs are unique within a version and never shared between programmes, and an ID
always keeps its type. Matching activities across versions on `ACTIVITY_ID` (plan D17) is safe,
and so is matching notes to activities on it (WP-S9c). Programmes weren't created by copying
another programme's XML.

## WP-S13. Legal text and review audit

How many programmes hold legal text (which feeds licence documents, plan D14) and review
acknowledgements (plan D16). Both inform Open question 5.

```sql
WITH cur_wp AS (
  SELECT plm.licence_type, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE wpd.status_control = 'C' AND wpd.record_status = 'ACTIVE'
  AND NVL(XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)), '-') != 'DRAFT'
)
SELECT licence_type
     , COUNT(*)                                                     programmes
     , SUM(CASE WHEN legal_text_nodes > 0 THEN 1 ELSE 0 END)        with_legal_text
     , SUM(CASE WHEN review_audits > 0 THEN 1 ELSE 0 END)           with_review_audit
     , SUM(review_audits)                                           review_audit_entries
     , MAX(latest_ack)                                              latest_acknowledgement
FROM (
  SELECT c.licence_type
       , XMLCAST(XMLQUERY('count(/WORK_PROGRAMME/LEGAL_TEXT/node())'              PASSING c.xml_data RETURNING CONTENT) AS NUMBER) legal_text_nodes
       , XMLCAST(XMLQUERY('count(/WORK_PROGRAMME/REVIEW_AUDIT_LIST/REVIEW_AUDIT)' PASSING c.xml_data RETURNING CONTENT) AS NUMBER) review_audits
       , XMLCAST(XMLQUERY('max(for $d in /WORK_PROGRAMME/REVIEW_AUDIT_LIST/REVIEW_AUDIT/ACKNOWLEDGED_DATETIME
                               return substring($d, 1, 10))'
                          PASSING c.xml_data RETURNING CONTENT) AS VARCHAR2(20))                                           latest_ack
  FROM cur_wp c
)
GROUP BY ROLLUP(licence_type)
ORDER BY GROUPING(licence_type), 1;
```

`max()` over strings needs an XQuery processor that compares strings. If Oracle rejects it, drop
the `latest_ack` column.

**Result (live, 29 October 2026):**

| Type | Programmes | With legal text | With review audit | Review audit entries | Latest acknowledgement |
|---|---:|---:|---:|---:|---|
| `AL` | 4 | 0 | 3 | 7 | 2025-02-05 |
| `DL` | 5 | 0 | 3 | 9 | 2024-07-03 |
| `EXL` | 38 | 0 | 13 | 43 | 2025-03-28 |
| `ML` | 7 | 0 | 6 | 12 | 2026-10-16 |
| `P` | 2,443 | 838 | 1,334 | 4,635 | 2026-10-29 |
| `PEDL` | 346 | 88 | 209 | 596 | 2026-08-11 |
| `PL` | 28 | 0 | 22 | 81 | 2026-10-19 |
| (no type) | 5 | 1 | 0 | 0 | |
| **Total** | **2,876** | **927** | **1,590** | **5,383** | **2026-10-29** |

**Reading:**

- **Legal text is common on offshore and `PEDL` programmes:** 838 `P` (34%) and 88 `PEDL` (25%),
  and none on the other types. It feeds the licence documents (D14), so it can't simply be
  dropped. It is one block per programme and won't be split per activity: it either goes into a
  programme-level area or is copied to every activity (plan Q5, WP-L10).
- **Review acknowledgements are in active use:** 1,590 programmes (55%) hold 5,383 entries, and
  the latest was made today. **Decided: the review audit is not migrated** (D16).
- The one unnumbered award with legal text is out of scope (no licence type).

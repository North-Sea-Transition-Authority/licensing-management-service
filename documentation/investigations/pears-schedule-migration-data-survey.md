# PEARS Schedule Migration: Data Survey

> Companion to [pears-schedule-migration-plan.md](pears-schedule-migration-plan.md). This document
> holds the read-only survey queries run against PEARS and their results. The plan holds the
> decisions that come from them, and links here for the evidence.

## Conventions

- **Run against live**, read-only, unless a result says otherwise. Record the date each was run.
- **Scope (decided after S1–S18 were run):** every PEARS licence schedule on a PEARS-managed licence
  type (`LicenceType.managedByLms = false` in LMS), regardless of licence status. That is every
  licence type with a PEARS schedule: `P`, `PEDL`, `PL`, `EXL`, `ML`, `DL`, `AL` and `CE`. Only
  schedules with no terms (left to the licensing team) are
  not migrated. See the plan, "Scope: licence types and
  statuses".
- **Coverage:** every section covers every licence type. S2, S3, S6 and S17 had no type filter.
  **S7–S16 and S18 were first run on `P` and `PEDL` only** (`licence_type IN ('P', 'PEDL')`), then
  re-run with `NOT IN ('P', 'PEDL')` for the other 54 typed licences (595 versions), and the
  results are combined in each section. The follow-up queries S8c, S8d, S11b and S13b concern `P` and `PEDL` findings only.
- **Placeholder grid rows** (`DUMMY_EVENT = 'true'`), which the Fox grid builds, are always excluded.
- "Current" means the schedule version with `end_datetime IS NULL`. "All versions" includes history.
- **Licence status** is taken from the latest live data point (`ped_data_points`, `ped_sim_id = 0`)
  through `xview_ped_ld_current.licence_status`. The codes, as `pedmgr.licensing_web_services`
  maps them: `E` Extant, `S` Surrendered, `C` Conditional, `R` Revoked, `P` Provisional, `X` Expired.

## Status

| # | Survey item | Status | Section |
|---|---|---|---|
| S1 | Event type catalogue | ✅ Live, 27 Oct 2026 | [S1](#s1-event-type-catalogue) |
| S2 | Licences and schedule versions by licence type | ✅ Live, 27 Oct 2026 (with S2b) | [S2](#s2-licences-and-schedule-versions) |
| S3 | Events by type | ✅ Live, 27 Oct 2026 | [S3](#s3-events-by-type) |
| S4 | Retention/development area rate | ✅ Live, 27 Oct 2026: never used | [S4](#s4-retentiondevelopment-area-rate) |
| S5 | `RECUR_END_DATE` | ✅ Live, 27 Oct 2026: never used | [S5](#s5-recur_end_date) |
| S6 | Events with no event type | ✅ Live, 27 Oct 2026 (current and history) | [S6](#s6-events-with-no-event-type) |
| S7 | Expiry events per current schedule | ✅ Live, 27 Oct 2026, all types | [S7](#s7-expiry-events-per-current-schedule) |
| S8 | Term structure (current versions) | ✅ Live, 27 Oct 2026 (summary and S8b all types; S8c, S8d `P`/`PEDL` only) | [S8](#s8-term-structure-current-versions) |
| S9 | Phase structure (current versions) | ✅ Live, 27 Oct 2026: all pass; other types have no phases | [S9](#s9-phase-structure-current-versions) |
| S10 | `ORIGIN_DATE` missing or different from the licence start date (all versions) | ✅ Live, 27 Oct 2026, all types (with S10b) | [S10](#s10-origin_date-all-versions) |
| S11 | `RATE` values (whole, 1–2 dp, > 2 dp, does not fit) | ✅ Live, 27 Oct 2026, all types (with S11b, S11c) | [S11](#s11-rate-values) |
| S12 | Rate recurrence and gaps (current versions) | ✅ Live, 27 Oct 2026, all types (with S12b) | [S12](#s12-rate-recurrence-and-gaps-current-versions) |
| S13 | Mandatory relinquishment percentages (current versions) | ✅ Live, 27 Oct 2026, all types (with S13b) | [S13](#s13-mandatory-relinquishment-percentages-current-versions) |
| S14 | LMS vs PEARS date arithmetic differences | ✅ Live, 27 Oct 2026, all types | [S14](#s14-lms-vs-pears-date-arithmetic) |
| S15 | `RD`/`PA` events (current versions) | ✅ Live, 27 Oct 2026, all types | [S15](#s15-rdpa-events-current-versions) |
| S16 | `EVENT_ID` and ambiguous cross-version matches | ✅ Live, 27 Oct 2026, all types | [S16](#s16-event_id-and-ambiguous-cross-version-matches) |
| — | Event types beyond the known catalogue | ✅ None on live (S1) | [S1](#s1-event-type-catalogue) |
| S17 | Licences with no PEARS schedule | ✅ Live, 27 Oct 2026, all types (with S17b): none extant | [S17](#s17-licences-with-no-pears-schedule) |
| S18 | Licences with a schedule, by status | ✅ Live, 27 Oct 2026, all types: 420 extant | [S18](#s18-licences-with-a-schedule-by-status) |

## Summary (live, 27 October 2026)

The survey is complete (S1–S18, all licence types). Overall the data is consistent enough to migrate by rule. The
exceptions are mostly systematic, by licensing round, rather than one-off errors.

| Area | Finding | Section |
|---|---|---|
| Size | 1,799 licences with a licence type and a schedule, 10,834 versions, ~215.8k events, ~450k LMS rows, all in scope. `P` and `PEDL` are 1,745 licences and 10,239 versions. The other 54 are `PL`, `EXL`, `ML`, `DL`, `AL`, `CE`. Only 420 typed licences (23%) are extant (381 `P`/`PEDL`, 39 others); 1,354 are surrendered, expired or revoked, and 25 have no status. | S2, S3, S18 |
| Event types | The 11 known types only. `2N` is never used. | S1, S3 |
| Start dates | Every current version has one. 1,125 historical versions have none and use a borrowed date (decided). 7 current versions (5 `P`, 1 `PL`, 1 `ML`) differ from the licence start date, which is legitimate: the schedule start date is migrated (L3). | S10 |
| Terms | 1,540 of 1,798 current schedules follow the plan's rule exactly, and 166 more have terms but no expiry. 172 don't fit the original rule (134 `P`/`PEDL`, 38 other types). They fall into 7 patterns (A–G), and every pattern now has a decided rule (L5–L10). 40 have no terms and are left to the licensing team (L9). | S7, S8 |
| Phases | All 309 schedules with phases follow the rule (last phase ends at `T(1)`). Skipped phases are normal and allowed by business rule (in order). | S9 |
| Rates | All populated `P`, `PEDL`, `EXL`, `DL` and `AL` values are whole numbers; 2 rate-only schedules have an empty rate. `ML` has pence, and `PL`/`ML` have decimals with no leading zero (e.g. `.5`), which parse fine (S11c). On migrated schedules every rate fits. Every schedule ends in one indefinite yearly rate. About 310 deliberate gaps between rates on migrated schedules (298 `P`, 12 `PL`), mostly pre-1999. Fixed-count yearly rates on 9 `PL` and 3 `AL`. | S11, S12 |
| Date arithmetic | PEARS stored dates are reliable (1 exception). Copying offsets would move 96 events (94 `P`, 2 `EXL`) by up to 3 days, which deriving offsets from dates avoids. | S14 |
| Untyped events | 201 on current versions: ~180 empty leftovers, ~20 holding data on 16 licences. Never shown on the schedule screen, so ignored by the migration (L1). | S6 |
| Relinquishment % | Decided: its own LMS field, migrated and written back. 585 feed licence documents (incl. 8 `PL`). 713 hold the figure only in comments. 303 hold `x` (not migrated). | S13 |
| `RD`/`PA` | 463 current events (403 `P`/`PEDL`, 60 other types). A fourth subtype, `END`. 51 older `PA` events have no subtype, area block or `EVENT_ID`. | S15 |
| History matching | `EVENT_ID` only on `RD`/`PA`. Type + offset is unique within a version; comments often repeat. | S16 |
| Never used | Retention/development area rate, `RECUR_END_DATE`, `PRODUCING` area type. (`RECURRANCES` counts are unused on `P`/`PEDL` but used on 9 `PL` and 3 `AL` schedules, S12.) | S4, S5, S12, S15 |

## For the licensing team

Items that need the licensing team, as either a **fix in PEARS** before the production run or a
**confirmation or decision** on a migration rule. Each links to its evidence.

### Fix in PEARS before the production run

| # | Item | Licences | Why it matters | Section |
|---|---|---|---|---|
| L2 | **Indefinite yearly rate followed by another rate** | P2644 (7500 yearly from 2038, then a one-off 150 in 2039) | The payments report pays both. Probably a data error. | S12b |

### Confirm or decide a migration rule

| # | Question | Licences | Proposed rule | Section |
|---|---|---|---|---|
| L12 | **Rate gaps: errors or deliberate no-payment years?** | 298 `P` gaps, mostly pre-1999 (e.g. initial rental, then the next rate 6 years later), plus 12 on `PL` (e.g. `40` then `90` 4 years later) | Confirm from the licence terms (one licence per pattern). If they are **errors**, the previous rate runs on in LMS and the write-back fills the gap. If they are **legitimate**, LMS uses a **£0 rate** for those years, migrated in and written back. | S12b |
| L14 | **Expiry date convention** | 1,612 current schedules with an expiry (all types) | Decide Open question 8: LMS stores the day before the PEARS expiry, or LMS's check changes. | plan |

### Decided

Business rule: **not every term or phase needs to exist on a licence schedule, but those present
must be in order** (e.g. only Term 2 and Term 3, or only Phase A and Phase C). The first term
present, and the first phase present, start on the licence start date.

| # | Item | Licences | Rule | Section |
|---|---|---|---|---|
| L1 | **Events saved without an event type** (current and history) | 201 on current versions: ~180 empty rows, ~20 holding data on 16 licences (P1159, P1272, P2036, P2311, P2314, P2318, P2319, P2356, P2397, P2438, P2476, P2582, P2660, P328, P335, PL179) | **Ignored by the migration** and only counted in the report. The schedule screen never shows them, so the licensing team never sees them and can't fix them there. LMS matches what PEARS shows today: P1272 and P328 have no term for their untyped rows, and the untyped rates (e.g. P2582's 7500 from 2032) aren't migrated, just as the payments report doesn't pay them. | S6 |
| L3 | **Schedule start date differs from the licence start date** | P1064, P1930, P2138, P2139, P2349, PL220, ML6 | **Decided (28 October 2026): legitimate.** The LMS schedule start date is the PEARS schedule start date (`ORIGIN_DATE`), not the licence start date, and the difference is not reported. (P2349 and ML6 have no terms, pattern E, so they aren't migrated.) | S10b |
| L5 | **No `T(1)`, only `T(2)` and `T(3)` = expiry (pattern A)** | 49 `P` (2017–2024 rounds) and PEDL141 (pattern G) | Business rule. No Initial term. Second = start → `T(2)`, Third = `T(2)` → `T(3)`/expiry. None has phases, so LMS's phase checks (which need an Initial term) don't apply. | S8b, S8c |
| L6 | **Last `T` earlier than the expiry (pattern B)** | ~30, mostly 1980s (P328, P351, P473, P483–P614, P556, P726, P774, P799, P1055, P1192, P1262, P1577, P1722, P1885, P2316, PEDL116, PEDL155, PEDL255), plus PEDL141 and P1159; and 15 others (PL55, PL77, PL79, PL80, PL81, PL90, PL116, PL161, PL162, PL211, PL233, PL234, PL259, EXL250, EXL273) | Migrated as they are: no extra term, the final term ends on its `T`, and the expiry keeps the `E` date. Each one is reported. LMS's Review and Apply check will flag the difference the next time the schedule is edited. | S8b |
| L7 | **Four terms (pattern C)** | 13: P1162, P1165, P1368, P1373, P1374, P1484, P1485, P1487, P1488, P1636, P1638, P1640, P1641 | LMS gets a Fourth term (`T(4)` ends it). It can only be maintained on licences whose schedule already has one, and can't be added to any other schedule. See the plan's "Decision: four-term licences". | S8b |
| L8 | **Unnumbered `T` on the start date (pattern D)** | P2313, P2322, P2326, P2327, P2331, P2332, P1159; PL205, PL211, PL213, PL215, PL220, PL233, PL234, PL259 | A `T` event with no number is ignored and not migrated, and only counted in the report (as untyped events, L1). The numbered terms then fit the rules. | S8b |
| L9 | **Schedules with no terms (pattern E)** | 40. 22 `P`: 14 extant (P249, P251, P256, P257, P291, P292, P293, P295, P296, P324, P416, P2333, P2495, P2643), 7 surrendered (P252, P258, P288, P404, P474, P2334, P2349), 1 expired (P294). 18 others: ML3, ML4, ML5, ML6, ML7, ML18, ML21, DL1–DL5, PL89, PL199, PL240, PL241, PL249, CE1 (17 extant, all but DL5) | **Not migrated.** Reported with their licence status, and the licensing team creates the LMS schedule manually. | S8, S8b, S8d |
| L10 | **`T(1)` and `T(3)` = expiry, no `T(2)` (pattern F)** | P1800, PEDL143, PEDL189, PEDL235, PL235 | Business rule. No Second term. Initial = start → `T(1)`, Third = `T(1)` → `T(3)`. | S8b |
| L11 | **Phases `C` only or `BC`** | 16 (`C`), 15 (`BC`) | Business rule. The first phase present starts on the licence start date. With `C` only, Phase C covers the whole initial term. | S9 |
| L13 | **Mandatory relinquishment percentage** | 585 events feed licence documents (incl. 8 `PL`). 713 hold the figure only in comments. 303 hold `x`. | **Decided (28 October 2026):** the percentage becomes its own data point on mandatory relinquishment events in LMS (0–100, up to 1 dp). It is migrated from PEARS (`x` and comment-only figures migrate with an empty percentage) and written back. | S13 |
| L15 | **Other licence types** | 54: PL 26, EXL 12, ML 7, DL 5, AL 3, CE 1 | **Migrated.** Every PEARS-managed licence type (`managedByLms = false`) is in scope, whatever its status. All six are `LANDWARD_PRODUCTION` in LMS (`AL`, `CE` and `DL` since 28 October 2026, plan Open question 14). Every `ML`, `DL` and `CE` schedule, and 5 `PL`, have no terms, so they are not migrated: they are reported and left to the licensing team (L9). | S2, S8b |
| L17 | **Licence with no current schedule version** | P1161 (surrendered 30/11/2012; both versions end-dated 14/10/2011, while still extant) | **Decided (28 October 2026): migrated** with both versions. The later one becomes the active LMS version, and the licence is reported as "No current version in PEARS". | S2b |

### For information only

- **Untyped events in history only:** P1328, P1665, P2329. Ignored like all untyped events (L1).
- **186 current schedules have no expiry event** (185 `P` and CE1). The 166 that have terms migrate
  without an expiry record. The other 20 have no terms either (pattern E), so they aren't migrated.
- **`x` in the relinquishment percentage** (303 events) is not migrated (decided). The event is.
- **One `P` rate** has a stored date that differs from PEARS's own formula (S14). It will show in the
  comparison report.
- **15 schedules with "term dates out of order"** (S8: 7 `P`, 8 `PL`). The check compared each `T`
  with the one before it by term number, with an unnumbered `T` sorted last. So an unnumbered `T`
  on the start date always counts as out of order.
  - For the 8 `PL`, S8b confirms they are exactly the pattern D schedules, in order once the
    unnumbered `T` is ignored.
  - The 7 `P` match pattern D's count, but the `P` run of S8b only listed problems, so it doesn't
    show the pairing directly. Confirm when the survey is re-run before the production run.
  - Any that remain are skipped and reported (plan, "Term boundaries" rule 5).

## S1. Event type catalogue

```sql
SELECT mnem, short_title, display_seq, active_offshore, active_onshore, recurrable, acknowledgeable
FROM pedmgr.ped_template_event_types
ORDER BY display_seq;
```

**Result (live, 27 October 2026; identical to UAT):**

| MNEM | SHORT_TITLE | DISPLAY_SEQ | OFFSHORE | ONSHORE | RECURRABLE | ACKNOWLEDGEABLE |
|---|---|---|---|---|---|---|
| P | Phase | -4 | Y | Y | N | Y |
| T | Term | -3 | Y | Y | N | Y |
| R | Rate | -2 | Y | Y | Y | N |
| MR | Mandatory Relinquishment | -1 | Y | Y | N | Y |
| E | Licence Expiry Date | 1 | Y | Y | | Y |
| PA | Prospective Area | 5 | Y | N | | Y |
| BM | Bespoke Milestone | 6 | Y | Y | | Y |
| DD | Drill-or-drop | 7 | Y | Y | | Y |
| 2M | Second Term with autosurrender | 8 | Y | Y | | Y |
| 2N | 2nd term with all acreage | 9 | Y | Y | | Y |
| RD | Retention/Development Area | 10 | N | Y | N | Y |

**Reading:**

- 11 types, the same 11 the plan maps. No other user-created types exist on live.
- Every type is **acknowledgeable except `R`**. Rate dates never create PEARS acknowledgements, so
  the write-back's acknowledgement matching (event type + date + `EVENT_ID`) matters for every
  other type, including `RD`/`PA` and the user-defined ones.
- `RECURRABLE` is only `Y` for `R`. It is empty, not `N`, for `E`, `PA`, `BM`, `DD`, `2M`, `2N`; the
  Fox screen tests for `= 'Y'`, so those are not recurrable.

## S2. Licences and schedule versions

```sql
WITH per_licence AS (
  SELECT plm.id ped_lm_id
       , plm.licence_type
       , COUNT(*)                                                   versions
       , SUM(CASE WHEN plsd.end_datetime IS NULL THEN 1 ELSE 0 END) open_versions
  FROM pedmgr.ped_licence_master plm
  JOIN pedmgr.ped_licence_schedule_details plsd ON plsd.ped_lm_id = plm.id
  WHERE plm.licence_type IS NOT NULL
  GROUP BY plm.id, plm.licence_type
)
SELECT CASE WHEN GROUPING(pl.licence_type) = 1 THEN 'ALL' ELSE pl.licence_type END AS lic_type
     , COUNT(*)                                                   AS licences_with_schedule
     , SUM(pl.versions)                                           AS schedule_versions
     , SUM(pl.open_versions)                                      AS current_versions
     , ROUND(AVG(pl.versions), 1)                                 AS avg_versions_per_licence
     , MAX(pl.versions)                                           AS max_versions_per_licence
     , SUM(CASE WHEN pl.open_versions = 0 THEN 1 ELSE 0 END)      AS licences_no_open_version
     , SUM(CASE WHEN pl.open_versions > 1 THEN 1 ELSE 0 END)      AS licences_multiple_open
     , GROUPING(pl.licence_type)                                  AS is_total
FROM per_licence pl
GROUP BY ROLLUP(pl.licence_type)
ORDER BY 9, 1;
```

**Result (live, 27 October 2026):**

| LIC_TYPE | LICENCES | VERSIONS | CURRENT | AVG | MAX | NO OPEN | MULTIPLE OPEN |
|---|---|---|---|---|---|---|---|
| AL | 3 | 30 | 3 | 10 | 14 | 0 | 0 |
| CE | 1 | 5 | 1 | 5 | 5 | 0 | 0 |
| DL | 5 | 53 | 5 | 10.6 | 15 | 0 | 0 |
| EXL | 12 | 91 | 12 | 7.6 | 13 | 0 | 0 |
| ML | 7 | 80 | 7 | 11.4 | 23 | 0 | 0 |
| **P** | **1,525** | **8,312** | **1,524** | 5.5 | 66 | **1** | 0 |
| **PEDL** | **220** | **1,927** | **220** | 8.8 | 28 | 0 | 0 |
| PL | 26 | 336 | 26 | 12.9 | 24 | 0 | 0 |
| **ALL** | **1,799** | **10,834** | **1,798** | 6 | 66 | 1 | 0 |

**Reading:**

- **In scope** (scope decision, see Conventions): the 1,799 licences with a licence type, 10,834
  versions, 1,798 current.
- **`P` and `PEDL`:** 1,745 licences, **10,239** schedule versions, 1,744 current. Most of the
  survey (S7–S16, S18) looked at these only.
- **Other types:** 54 licences, 595 versions: PL, EXL, ML, DL, AL and CE.
- One `P` licence has no current version (P1161, surrendered, S2b). It is migrated with its latest
  version as the active one, and reported. The largest history is now **66**
  versions.
- An earlier run (October 2026, environment not recorded) gave 10,547 versions in total (9,938 in
  scope, max 59). Licence counts are unchanged; only versions have grown. PEARS keeps adding a
  version on every save, so the final run's size will be higher again.

### S2b. The licence with no current schedule version

```sql
SELECT plm.licence_type || plm.licence_no licence_reference, plm.id ped_lm_id,
       COUNT(*) versions, MAX(plsd.end_datetime) last_ended
FROM pedmgr.ped_licence_master plm
JOIN pedmgr.ped_licence_schedule_details plsd ON plsd.ped_lm_id = plm.id
GROUP BY plm.licence_type, plm.licence_no, plm.id
HAVING SUM(CASE WHEN plsd.end_datetime IS NULL THEN 1 ELSE 0 END) = 0;
```

**Result (live, 27 October 2026):** one licence, **P1161** (`ped_lm_id` 3728). It has 2 schedule
versions, and the last was end-dated on 14/10/2011 at 14:13:29 with no new version saved.

**Licence status (live, 28 October 2026):** extant from 01/12/2004, **surrendered** at the data
point of 30/11/2012. So its schedule was ended about a year before the licence was surrendered.

| Data point | Status |
|---|---|
| 30/11/2012 | `S` Surrendered |
| 30/11/2011 | `E` Extant |
| 17/01/2011 | `E` Extant |
| 30/11/2006 | `E` Extant |
| 01/12/2004 | `E` Extant |

**Reading:** nothing records why its schedule was ended in 2011. **Decided (28 October 2026):**
P1161 is migrated with both versions. The later one becomes the `ACTIVE` LMS detail, and it is
reported as "No current version in PEARS" (plan, "Schedule history"). Nothing to fix in PEARS
(L17).

## S3. Events by type

```sql
SELECT CASE WHEN GROUPING(e.event_mnem) = 1 THEN 'ALL' ELSE e.event_mnem END AS mnem
     , COUNT(*)                                                   AS events_all_versions
     , SUM(CASE WHEN plsd.end_datetime IS NULL THEN 1 ELSE 0 END) AS events_current_versions
     , COUNT(DISTINCT plsd.ped_lm_id)                             AS licences
     , GROUPING(e.event_mnem)                                     AS is_total
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data
    COLUMNS event_mnem VARCHAR2(10) PATH 'EVENT_MNEM'
  ) e
WHERE plm.licence_type IS NOT NULL
GROUP BY ROLLUP(e.event_mnem)
ORDER BY 5, 2 DESC;
```

**Result (live, 27 October 2026; all 1,799 licences with a licence type):**

| MNEM | ALL VERSIONS | CURRENT | LICENCES |
|---|---|---|---|
| R | 157,401 | 25,685 | 1,798 |
| T | 27,434 | 4,817 | 1,765 |
| MR | 10,799 | 2,026 | 1,419 |
| E | 9,316 | 1,612 | 1,791 |
| P | 4,384 | 658 | 310 |
| RD | 2,091 | 264 | 60 |
| *(no type)* | 1,498 | 201 | 209 |
| PA | 1,449 | 151 | 27 |
| DD | 1,004 | 240 | 249 |
| 2M | 251 | 36 | 38 |
| BM | 185 | 45 | 26 |
| **ALL** | **215,812** | **35,735** | **1,798** |

**Reading:**

- `2N` has never been used.
- Rates are about three-quarters of all events, roughly one per anniversary per version.
- Estimated LMS rows for the full history: about **450k**:

  | LMS table | Rows |
  |---|---|
  | Schedule details + start dates | ~10.8k each |
  | Rates | ~157k (more under yearly-rate option B) |
  | Terms | ~27.4k |
  | Other schedule events (`MR`, `DD`, `2M`, `BM`) | ~12.2k |
  | Expiries | ~9.3k |
  | Phases | ~4.4k |
  | Area events (`RD`, `PA`) | ~3.5k |
  | Migration links (all typed events) | ~214k |
  | Event comments | up to the commented T/P/R events, fewer after removing repeats |

  These are upper bounds: the 40 schedules with no terms (S8) are not migrated. P1161 (S2b) is
  migrated, but its events aren't in the "current" counts, as neither of its versions is current.
- 186 current schedules have no `E` (1,798 − 1,612), which matches S7 (no schedule has more than
  one).
- These counts cover every licence with a schedule, which is the migration's scope.
- An earlier run (October 2026, environment not recorded) gave 210,497 events (35,923 current).
  Current-version counts barely moved; the growth is in history, from new versions.

## S4. Retention/development area rate

```sql
-- 1. Any schedule version, current or historical, with a value set
SELECT plm.licence_type || plm.licence_no licence_reference, plsd.id plsd_id,
       plsd.start_datetime, plsd.end_datetime, x.rate
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE' PASSING plsd.xml_data
    COLUMNS rate VARCHAR2(4000) PATH 'RETENTION_AREA_RATE/text()') x
WHERE TRIM(x.rate) IS NOT NULL;
```

```sql
-- 2. Current schedules: element missing, present but empty, or set
SELECT plm.licence_type,
       CASE
         WHEN XMLEXISTS('/SCHEDULE/RETENTION_AREA_RATE/text()' PASSING plsd.xml_data) THEN 'value set'
         WHEN XMLEXISTS('/SCHEDULE/RETENTION_AREA_RATE' PASSING plsd.xml_data) THEN 'element empty'
         ELSE 'element missing'
       END rate_state,
       COUNT(*) schedules
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
WHERE plsd.end_datetime IS NULL
AND plm.licence_type IS NOT NULL
GROUP BY plm.licence_type,
       CASE
         WHEN XMLEXISTS('/SCHEDULE/RETENTION_AREA_RATE/text()' PASSING plsd.xml_data) THEN 'value set'
         WHEN XMLEXISTS('/SCHEDULE/RETENTION_AREA_RATE' PASSING plsd.xml_data) THEN 'element empty'
         ELSE 'element missing'
       END
ORDER BY 1, 2;
```

```sql
-- 3. Cross-check through the data warehouse view
SELECT COUNT(*) FROM pedmgr.xview_ped_lic_schedule_events WHERE retention_area_rate IS NOT NULL;
```

**Result (live, 27 October 2026):**

- **Query 1:** no rows.
- **Query 2 (current schedules):**

  | LICENCE_TYPE | RATE_STATE | SCHEDULES |
  |---|---|---|
  | AL | element empty | 3 |
  | CE | element missing | 1 |
  | DL | element empty | 5 |
  | EXL | element empty / missing | 8 / 4 |
  | ML | element empty / missing | 3 / 4 |
  | **P** | element missing | **1,524** |
  | **PEDL** | element empty / missing | **114 / 106** |
  | PL | element empty / missing | 25 / 1 |

  No schedule has a value set. Offshore (`P`) schedules never have the element. Onshore schedules
  have an empty element when they have been opened for editing on the onshore schedule screen,
  which adds it, and no element otherwise.
- **Query 3:** 0.

**Never used.** See the plan, "Retention/development area rate: unused". For the write-back,
writing an empty element for onshore licences (as the plan says) or omitting it are both
consistent with today's data.

## S5. `RECUR_END_DATE`

```sql
-- 1. Summary, current and historical
SELECT
  CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END AS version_state
, e.event_mnem
, e.recurring
, CASE WHEN e.recurrances IS NOT NULL THEN 'Y' ELSE 'N' END AS has_recurrances
, COUNT(*)                         AS events
, COUNT(DISTINCT plsd.ped_lm_id)   AS licences
FROM pedmgr.ped_licence_schedule_details plsd
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(RECUR_END_DATE) != "" and not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data
    COLUMNS
      event_mnem     VARCHAR2(10)   PATH 'EVENT_MNEM'
    , recurring      VARCHAR2(20)   PATH 'RECURRING'
    , recurrances    VARCHAR2(10)   PATH 'RECURRANCES'
    , recur_end_date VARCHAR2(10)   PATH 'RECUR_END_DATE'
  ) e
GROUP BY
  CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END
, e.event_mnem
, e.recurring
, CASE WHEN e.recurrances IS NOT NULL THEN 'Y' ELSE 'N' END
ORDER BY 1, 2;
```

```sql
-- 3. Cross-check through the data warehouse view
SELECT COUNT(*) events, COUNT(DISTINCT ped_lm_id) licences
FROM pedmgr.xview_ped_lic_schedule_events
WHERE recur_end_date IS NOT NULL
AND end_datetime IS NULL
AND NVL(dummy_event_flag, 'false') != 'true';
```

**Result (live, 27 October 2026):** query 1 returned no rows, and query 3 returned 0 events and
0 licences. **Never used.** See the plan, "`RECUR_END_DATE`: unused".

## S6. Events with no event type

```sql
-- 1. All untyped events on current versions (remove the end_datetime filter for history)
SELECT plm.licence_type || plm.licence_no licence_reference
     , plsd.id plsd_id
     , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
     , e.event_date, e.term_no, e.phase, e.rate, e.percentage, e.event_subtype, e.comments
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) = "" and not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data
    COLUMNS event_date VARCHAR2(10) PATH 'EVENT_DATE', term_no VARCHAR2(5) PATH 'TERM_NO'
          , phase VARCHAR2(5) PATH 'PHASE', rate VARCHAR2(50) PATH 'RATE'
          , percentage VARCHAR2(10) PATH 'PERCENTAGE', event_subtype VARCHAR2(30) PATH 'EVENT_SUBTYPE'
          , comments VARCHAR2(4000) PATH 'COMMENTS'
  ) e
WHERE plsd.end_datetime IS NULL
ORDER BY 1, e.event_date;
```

```sql
-- 2. Only the untyped events that hold data (for the licensing team)
SELECT plm.licence_type || plm.licence_no licence_reference
     , plsd.id plsd_id
     , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
     , e.event_date, e.term_no, e.phase, e.rate, e.percentage, e.comments
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) = "" and not(DUMMY_EVENT = "true")
            and (normalize-space(RATE) != "" or normalize-space(TERM_NO) != "" or normalize-space(PHASE) != ""
                 or normalize-space(PERCENTAGE) != "" or normalize-space(COMMENTS) != "")]'
    PASSING plsd.xml_data
    COLUMNS event_date VARCHAR2(10) PATH 'EVENT_DATE', term_no VARCHAR2(5) PATH 'TERM_NO'
          , phase VARCHAR2(5) PATH 'PHASE', rate VARCHAR2(50) PATH 'RATE'
          , percentage VARCHAR2(4000) PATH 'PERCENTAGE', comments VARCHAR2(4000) PATH 'COMMENTS'
  ) e
WHERE plsd.end_datetime IS NULL
ORDER BY 1, e.event_date;
```

**Result (live, 27 October 2026; query 1, current versions):** 201 untyped events, in two groups.
The same rows as the earlier October run. Some licences now show a newer `plsd_id` (e.g. AL10,
P335, P362, P57, PEDL165, PL81) because they have been saved since. Untyped rows carry over into
each new version on save.

1. **Empty rows, about 180:** no rate, term, phase, percentage or comments. Most are dated on the
   licence start date (e.g. AL, EXL, PL and PEDL licences on their start dates, P23xx on
   2017-05-15, often twice). Some have no date (P2128, P2158, P2594, P2607, P421 ×3).
2. **Rows that hold data, about 20:**

   | Looks like | Licences |
   |---|---|
   | Rate missing its `R` code | P1159 (6600, 2015-12-01), P2311, P2314, P2318, P2319 (15, 2017-05-15), P2356 (300, no date), P2397 (7500, no date), P2438 (7500, 2031-10-01), P2476 (300, 2023-07-15), P2582 (7500, 2032-12-01), P2660 (6900, 2035-06-01) |
   | Term missing its `T` code | P1272 (term 2, 2022-06-01), P328 (term 1, 1986-12-17) |
   | Text in the wrong field | P2036 (text in `PERCENTAGE`, 2013-01-01) |
   | Comment only | P2582 ("DOV 20/09/22: 2 month extension of Phase A, Phase C preserved. New event row added for final recurring rate."), P335 ("Mariner PPA's Longstop date", no date), PL179 (DoV note on term durations, no date) |

**Query 2, history version** (groups identical rows across versions):

```sql
SELECT plm.licence_type || plm.licence_no licence_reference
     , COUNT(DISTINCT plsd.id) versions_affected
     , MIN(plsd.start_datetime) first_seen
     , MAX(CASE WHEN plsd.end_datetime IS NULL THEN 'Y' ELSE 'N' END) still_current
     , e.event_date, e.term_no, e.phase, e.rate, e.percentage
     , SUBSTR(e.comments, 1, 100) comments
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) = "" and not(DUMMY_EVENT = "true")
            and (normalize-space(RATE) != "" or normalize-space(TERM_NO) != "" or normalize-space(PHASE) != ""
                 or normalize-space(PERCENTAGE) != "" or normalize-space(COMMENTS) != "")]'
    PASSING plsd.xml_data
    COLUMNS event_date VARCHAR2(10) PATH 'EVENT_DATE', term_no VARCHAR2(5) PATH 'TERM_NO'
          , phase VARCHAR2(5) PATH 'PHASE', rate VARCHAR2(50) PATH 'RATE'
          , percentage VARCHAR2(4000) PATH 'PERCENTAGE', comments VARCHAR2(4000) PATH 'COMMENTS'
  ) e
GROUP BY plm.licence_type, plm.licence_no, e.event_date, e.term_no, e.phase, e.rate, e.percentage
       , SUBSTR(e.comments, 1, 100)
ORDER BY 1, e.event_date;
```

**Result (live, 27 October 2026, all versions):**

| Licence | Versions | First seen | Current | Event date | Term | Rate | Percentage | Comments |
|---|---|---|---|---|---|---|---|---|
| P1159 | 1 → 11 | 06/02/2013 | Y | (none) → 2015-12-01 | | 6600 | | |
| P1272 | 1 → 5 | 31/05/2022 | Y | (none) → 2022-06-01 | 2 | | | |
| P1328 | 1 → 2 | 19/03/2013 | **N** | (none) → 2013-03-22 | 2 | | "You must enter this item" | |
| P1665 | 1 → 1 | 25/11/2021 | **N** | (none) → 2022-06-03 | 2 | | | |
| P2036 | 1 → 5 → 1 | 16/11/2012 | Y | (none) → 1900-01-01 → 2013-01-01 | | | "You must enter this item" | |
| P2311 | 1 → 13 | 24/04/2017 | Y | 1900-01-01 → 2017-05-15 | | 15 | | |
| P2314 | 1 → 3 | 24/04/2017 | Y | (none) → 2017-05-15 | | 15 | | |
| P2318 | 1 → 3 | 24/04/2017 | Y | (none) → 2017-05-15 | | 15 | | |
| P2319 | 1 → 4 | 24/04/2017 | Y | (none) → 2017-05-15 | | 15 | | |
| P2329 | 1 → 1 | 24/04/2017 | **N** | 1900-01-01 → 2017-05-15 | | 15 | | |
| P2356 | 1 | 17/08/2020 | Y | (none) | | 300 | | |
| P2397 | 10 | 19/08/2021 | Y | (none) | | 7500 | | |
| P2438 | 1 → 2 | 17/04/2023 | Y | (none) → 2031-10-01 | | 7500 | | |
| P2476 | 1 → 5 | 17/08/2020 | Y | (none) → 2023-07-15 | | 300 | | |
| P2582 | 2 → 3 | 22/09/2022 | Y | (none) → 2032-12-01 | | 7500 | | (plus a separate comment row "DOV 20/09/22: … New event row added for final recurring rate", 2 → 3 versions) |
| P2660 | 1 → 9 | 13/05/2024 | Y | (none) → 2035-06-01 | | 6900 | | |
| P328 | 1 → 7 | 27/01/2016 | Y | (none) → 1986-12-17 | 1 | | | |
| P335 | 7 | 14/11/2018 | Y | (none) | | | | "Mariner PPA's Longstop date" |
| PL179 | 6 | 04/01/2021 | Y | (none) | | | | "DoV 14/11/2020 (Term durations regularised …" |

("1 → 11" means the row appeared in 1 version without a date, then in 11 versions with one.)

**Reading:**

- **How they arise:** each row first appears **without a date**, and gets one on a later save, once
  the Fox screen calculates it from the offset. A `1900-01-01` date means it was calculated before
  the schedule had an `ORIGIN_DATE` (PEARS's date base). So these are events added with a value but
  without choosing an event type, and they carry forward on every save.
- **"You must enter this item"** in `PERCENTAGE` (P1328, P2036) is a Fox validation message saved
  as data, not a real percentage.
- **Long-standing:** P2311 has had an untyped rate of 15 since April 2017 (13 versions), P2397 a
  rate of 7500 since August 2021, and P2582 7500 since September 2022. None appear in the payments
  report.
- **History only, already gone from current:** P1328, P1665, P2329.
- **Still current:** the other 16 licences, the ones listed in query 1's data rows.
- **Decided (L1):** all untyped events, current and historical, are **ignored by the migration** and
  only counted. The schedule screen (`PED012X`) only shows events by type, so the licensing team
  never sees these rows and can't fix them there. LMS then matches what PEARS shows.

**Other types, checked separately (live, 27 October 2026):** query 1 filtered to
`NOT IN ('P', 'PEDL')` returned 25 rows, all already counted above. 24 are empty rows dated on the
licence start date (all 3 `AL`, 11 `EXL` and 10 `PL`), which are ignored. The other is PL179's
comment-only row, already counted above. Nothing new holds data.

The handling rule is in the plan, under "Events with no event type".

## S7. Expiry events per current schedule

```sql
SELECT plm.licence_type
     , x.e_count                  AS expiry_events
     , COUNT(*)                   AS current_schedules
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE'
    PASSING plsd.xml_data
    COLUMNS e_count NUMBER PATH 'count(EVENT_LIST/EVENT[EVENT_MNEM = "E" and not(DUMMY_EVENT = "true")])'
  ) x
WHERE plsd.end_datetime IS NULL
AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
GROUP BY plm.licence_type, x.e_count
ORDER BY plm.licence_type, x.e_count;
```

**Result (live, 27 October 2026; both runs, all types):**

| LICENCE_TYPE | EXPIRY_EVENTS | CURRENT_SCHEDULES |
|---|---|---|
| P | 0 | 185 |
| P | 1 | 1,339 |
| PEDL | 1 | 220 |
| PL | 1 | 26 |
| EXL | 1 | 12 |
| ML | 1 | 7 |
| DL | 1 | 5 |
| AL | 1 | 3 |
| CE | 0 | 1 |

**Reading:**

- **186 current schedules have no expiry event:** 185 `P` and CE1. The 166 with terms are migrated
  without an expiry record, and their final term ends at its last `T` (see the plan, "Term
  boundaries", rule 3). The other 20 (19 `P` and CE1) have no terms either, so they aren't migrated (pattern E).
- **No schedule has more than one** expiry event, so the "more than one `E`" skip rule never
  triggers on current versions.
- Every other schedule (`PEDL`, `PL`, `EXL`, `ML`, `DL`, `AL`) has exactly one.

## S8. Term structure (current versions)

Checks the term rules in the plan ("Term boundaries"): each `T(n)` marks the end of term n, the
numbers run 1..n without gaps or duplicates, the dates increase with the number, `T(1)` is after
`ORIGIN_DATE`, and the final `T` equals `E` when there is an `E`. Typed, non-dummy events only;
`P` and `PEDL` in the first run, the other types in the second.

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type, plsd.xml_data
       , TO_DATE(x.origin DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') origin_date
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE' PASSING plsd.xml_data
      COLUMNS origin VARCHAR2(20) PATH 'ORIGIN_DATE') x
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
, terms AS (
  SELECT s.plsd_id
       , TO_NUMBER(TRIM(t.term_no) DEFAULT NULL ON CONVERSION ERROR) term_no
       , TO_DATE(t.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') event_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data
      COLUMNS term_no VARCHAR2(10) PATH 'TERM_NO', event_date VARCHAR2(20) PATH 'EVENT_DATE') t
)
, terms_ordered AS (
  SELECT t.*
       , LAG(t.event_date) OVER (PARTITION BY t.plsd_id ORDER BY t.term_no) prev_date
  FROM terms t
)
, expiry AS (
  SELECT s.plsd_id, MAX(TO_DATE(e.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD')) e_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "E" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') e
  GROUP BY s.plsd_id
)
, per_schedule AS (
  SELECT s.plsd_id, s.licence_type, s.origin_date, ex.e_date
       , COUNT(t.plsd_id)                                                  t_events
       , COUNT(DISTINCT t.term_no)                                         distinct_terms
       , MIN(t.term_no)                                                    min_term
       , MAX(t.term_no)                                                    max_term
       , SUM(CASE WHEN t.plsd_id IS NOT NULL AND t.term_no IS NULL THEN 1 ELSE 0 END)     t_no_number
       , SUM(CASE WHEN t.plsd_id IS NOT NULL AND t.event_date IS NULL THEN 1 ELSE 0 END)  t_no_date
       , SUM(CASE WHEN t.event_date <= t.prev_date THEN 1 ELSE 0 END)      t_out_of_order
       , MAX(CASE WHEN t.term_no = 1 THEN t.event_date END)                t1_date
       , MAX(t.event_date) KEEP (DENSE_RANK LAST ORDER BY t.term_no NULLS FIRST) final_t_date
  FROM sched s
  LEFT JOIN terms_ordered t ON t.plsd_id = s.plsd_id
  LEFT JOIN expiry ex ON ex.plsd_id = s.plsd_id
  GROUP BY s.plsd_id, s.licence_type, s.origin_date, ex.e_date
)
SELECT licence_type
     , COUNT(*)                                                                         schedules
     , SUM(CASE WHEN t_events = 0 THEN 1 ELSE 0 END)                                    no_t_events
     , SUM(CASE WHEN t_events > 0 AND (min_term != 1 OR distinct_terms != max_term) THEN 1 ELSE 0 END) numbering_gap
     , SUM(CASE WHEN t_events > distinct_terms + t_no_number THEN 1 ELSE 0 END)         duplicate_term_no
     , SUM(CASE WHEN t_no_number > 0 THEN 1 ELSE 0 END)                                 t_without_number
     , SUM(CASE WHEN t_no_date > 0 THEN 1 ELSE 0 END)                                   t_without_date
     , SUM(CASE WHEN t_out_of_order > 0 THEN 1 ELSE 0 END)                              dates_out_of_order
     , SUM(CASE WHEN max_term > 3 THEN 1 ELSE 0 END)                                    more_than_3_terms
     , SUM(CASE WHEN origin_date IS NULL THEN 1 ELSE 0 END)                             no_origin_date
     , SUM(CASE WHEN t1_date <= origin_date THEN 1 ELSE 0 END)                          t1_on_or_before_origin
     , SUM(CASE WHEN e_date IS NOT NULL AND final_t_date = e_date THEN 1 ELSE 0 END)    final_t_equals_e
     , SUM(CASE WHEN e_date IS NOT NULL AND final_t_date != e_date THEN 1 ELSE 0 END)   final_t_differs_from_e
     , SUM(CASE WHEN e_date IS NULL AND t_events > 0 THEN 1 ELSE 0 END)                 no_e_has_terms
FROM per_schedule
GROUP BY ROLLUP(licence_type)
ORDER BY GROUPING(licence_type), licence_type;
```

**Result (live, 27 October 2026; current versions; both runs, all types):**

| Check | P | PEDL | PL | EXL | ML | DL | AL | CE | Total |
|---|---|---|---|---|---|---|---|---|---|
| Schedules | 1,524 | 220 | 26 | 12 | 7 | 5 | 3 | 1 | 1,798 |
| No `T` events | **22** | 0 | **5** | 0 | **7** | **5** | 0 | **1** | 40 |
| Term numbering gap (not 1..n) | **50** | **4** | **1** | 0 | 0 | 0 | 0 | 0 | 55 |
| Duplicate term number | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `T` without a term number | **7** | 0 | **8** | 0 | 0 | 0 | 0 | 0 | 15 |
| `T` without a date | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Term dates out of order | **7** | 0 | **8** | 0 | 0 | 0 | 0 | 0 | 15 |
| More than 3 terms | **13** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 13 |
| No `ORIGIN_DATE` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `T(1)` on or before `ORIGIN_DATE` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Final `T` = `E` | 1,303 | 216 | 8 | 10 | 0 | 0 | 3 | 0 | 1,540 |
| Final `T` ≠ `E` | **33** | **4** | **13** | **2** | 0 | 0 | 0 | 0 | 52 |
| No `E`, but has terms | 166 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 166 |

**Reading:**

- **Clean on the basics:** every current schedule has an `ORIGIN_DATE`, no `T(1)` is on or before
  it, no term number is duplicated, and every `T` has a date.
- **Follows the plan's rule:** 1,540 of 1,798 schedules have their final `T` equal to `E`. A further
  166 have terms but no `E`, and migrate without an expiry record.
- **Would be skipped, or need a decided rule** (the categories overlap, so the totals do not add
  up; the detail query below gives the distinct list):
  - **40 schedules with no `T` at all** (pattern E): 22 `P`, plus **all 7 `ML`, all 5 `DL`, CE1 and
    5 `PL`**. There is nothing to build terms from. **Decided:** not migrated; reported with their
    licence status, and the licensing team creates them in LMS manually.
    17 of the other 18 are live: all 7 `ML`, 4 `DL`, CE1 and all 5
    `PL` are extant (S18).
  - 55 with a numbering gap (e.g. no `T(1)`, or 1 and 3 without 2), and 15 with a `T` that has no
    number. **Update:** numbering gaps are allowed by business rule (terms may be skipped but must
    be in order), so these are no longer skipped for that reason (patterns A, F and G in S8b). A
    `T` with no number is ignored (pattern D).
  - 15 with term dates out of order. Probably the pattern D schedules (see "For information only"):
    on both `P` (7 and 7) and `PL` (8 and 8), the count matches the unnumbered-`T` count exactly.
  - 13 `P` schedules with more than 3 terms. **Update:** decided. LMS gets a Fourth term for
    these licences only (pattern C in S8b).
  - 52 whose final `T` differs from `E`, including 13 of the 26 `PL` and 2 `EXL`. **Decided:**
    migrated as they are and reported (pattern B).
- **`AL` is clean:** all 3 have terms ending on `E`. `EXL` is clean apart from 2 pattern B
  schedules.
- The detail query (S8b) lists them by pattern, and every pattern now has a decided rule.

### S8b. Term structure problems, one row per schedule

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type, plm.licence_type || plm.licence_no licence_reference
       , plsd.xml_data
       , TO_DATE(x.origin DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') origin_date
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE' PASSING plsd.xml_data
      COLUMNS origin VARCHAR2(20) PATH 'ORIGIN_DATE') x
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')
)
, terms AS (
  SELECT s.plsd_id
       , TRIM(t.term_no) term_no_text
       , TO_NUMBER(TRIM(t.term_no) DEFAULT NULL ON CONVERSION ERROR) term_no
       , TO_DATE(t.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') event_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data
      COLUMNS term_no VARCHAR2(10) PATH 'TERM_NO', event_date VARCHAR2(20) PATH 'EVENT_DATE') t
)
, terms_ordered AS (
  SELECT t.*, LAG(t.event_date) OVER (PARTITION BY t.plsd_id ORDER BY t.term_no) prev_date
  FROM terms t
)
, expiry AS (
  SELECT s.plsd_id, MAX(TO_DATE(e.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD')) e_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "E" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') e
  GROUP BY s.plsd_id
)
, per_schedule AS (
  SELECT s.plsd_id, s.licence_type, s.licence_reference, s.origin_date, ex.e_date
       , COUNT(t.plsd_id) t_events
       , COUNT(DISTINCT t.term_no) distinct_terms
       , MIN(t.term_no) min_term
       , MAX(t.term_no) max_term
       , SUM(CASE WHEN t.plsd_id IS NOT NULL AND t.term_no IS NULL THEN 1 ELSE 0 END) t_no_number
       , SUM(CASE WHEN t.event_date <= t.prev_date THEN 1 ELSE 0 END) t_out_of_order
       , MAX(t.event_date) KEEP (DENSE_RANK LAST ORDER BY t.term_no NULLS FIRST) final_t_date
       , LISTAGG(NVL(t.term_no_text, '?') || '@' || TO_CHAR(t.event_date, 'YYYY-MM-DD'), ', ')
           WITHIN GROUP (ORDER BY t.event_date, t.term_no) term_sequence
  FROM sched s
  LEFT JOIN terms_ordered t ON t.plsd_id = s.plsd_id
  LEFT JOIN expiry ex ON ex.plsd_id = s.plsd_id
  GROUP BY s.plsd_id, s.licence_type, s.licence_reference, s.origin_date, ex.e_date
)
SELECT licence_reference
     , plsd_id
     , TO_CHAR(origin_date, 'YYYY-MM-DD') origin_date
     , TO_CHAR(e_date, 'YYYY-MM-DD') expiry_date
     , term_sequence
     , RTRIM(
         CASE WHEN t_events = 0 THEN 'NO_TERMS, ' END ||
         CASE WHEN t_events > 0 AND (min_term != 1 OR distinct_terms != max_term) THEN 'NUMBERING_GAP, ' END ||
         CASE WHEN t_no_number > 0 THEN 'TERM_WITHOUT_NUMBER, ' END ||
         CASE WHEN t_out_of_order > 0 THEN 'DATES_OUT_OF_ORDER, ' END ||
         CASE WHEN max_term > 3 THEN 'MORE_THAN_3_TERMS, ' END ||
         CASE WHEN e_date IS NOT NULL AND final_t_date != e_date THEN 'FINAL_T_DIFFERS_FROM_E, ' END
       , ', ') problems
FROM per_schedule
WHERE t_events = 0
   OR (t_events > 0 AND (min_term != 1 OR distinct_terms != max_term))
   OR t_no_number > 0
   OR t_out_of_order > 0
   OR max_term > 3
   OR (e_date IS NOT NULL AND final_t_date != e_date)
ORDER BY licence_reference;
```

`term_sequence` shows each `T` as `term number@date` in date order (`?` = no number), e.g.
`1@2018-12-01, 2@2028-12-01, 3@2040-12-01`.

The second run (other types) used `NOT IN ('P', 'PEDL')` and dropped the final `WHERE`, so it
returned all 54 schedules, clean ones included.

**Result (live, 27 October 2026; both runs):** 172 current schedules with a problem: 134 `P` and
`PEDL` (127 `P`, 7 `PEDL`) and 38 of the 54 other-type schedules. They fall into clear patterns,
each now with a decided rule:

| # | Pattern | Licences | Example | Rule |
|---|---|---|---|---|
| A | **No `T(1)`, only `T(2)` and `T(3)` = `E`** (modern rounds, 2017–2024) | 49 `P` (see S8c) | P2505: `2@2027-12-01, 3@2042-12-01`, `E` 2042-12-01 | **Decided (business rule: terms may be skipped but must be in order).** No Initial term: **Second** = `ORIGIN_DATE`→`T(2)`, **Third** = `T(2)`→`T(3)`/`E`. No phases exist (S8c), so the phase checks' need for an Initial term doesn't arise. |
| B | **Last `T` earlier than `E`**: only `T(1)` (or `T(1)`, `T(2)`), then `E` years later (mostly 1980s licences, some later) | ~30 `P`/`PEDL`: P328, P351, P473, P483–P614, P556, P726, P774, P799, P1055, P1192, P1262, P1577, P1722, P1885, P2316, PEDL116, PEDL155, PEDL255. **Plus 15 others:** PL55, PL77, PL79, PL80, PL81, PL90, PL116, PL161, PL162 (only `T(1)`, then `E` decades later), PL211, PL233, PL234, PL259 (also pattern D), EXL250, EXL273 | P516: `1@1991-06-14`, `E` 2021-06-14; PL116: `1@1975-08-01`, `E` 2045-08-01 | **Decided: migrate as they are and report.** No extra term is added. The final term ends on its `T`, and the expiry keeps the `E` date. (The earlier candidate was to add the next term from the last `T` to `E`, as PL179's DoV note suggests.) |
| C | **4 terms** (2004–2009 rounds) | 13: P1162, P1165, P1368, P1373, P1374, P1484, P1485, P1487, P1488, P1636, P1638, P1640, P1641 | P1484: `1@2009-04-01, 2@2013-04-01, 3@2019-04-01, 4@2037-04-01` | **Decided.** Migrate as four terms: LMS gets a `FOURTH` term type, and `T(4)` ends the Fourth term. It can only be maintained on licences whose schedule already has one. No other schedule can have it added. |
| D | **Unnumbered `T` on `ORIGIN_DATE`**, plus 1, 2, 3 | 7 `P`: P2313, P2322, P2326, P2327, P2331, P2332, and P1159. **Plus 8 `PL`:** PL205, PL211, PL213, PL215, PL220, PL233, PL234, PL259 | P2313: `?@2017-05-15, 1@2026-05-15, 2@2032-05-15, 3@2050-05-15`; PL213: `?@1982-08-09, 1@1986-08-09, 2@2006-08-09, 3@2029-08-09` | **Decided: ignore any `T` event with no number** (not migrated; only counted in the report, as untyped events are). The rest then fit the rules. P1159 also has a `T(3)` earlier than `E`. It is pattern B, so it is migrated as it is and reported (see L6). |
| E | **No `T` events at all** | 22 `P`: old licences 1976–1982 (P249–P474) and some recent ones (P2333, P2334, P2349, P2495, P2643). **Plus 18 others:** ML3, ML4, ML5, ML6, ML7, ML18, ML21, DL1–DL5, PL89, PL199, PL240, PL241, PL249, CE1 | P249: one `R`, no terms, no `E`; ML3: no terms, `E` 2034-01-01 | **Decided: not migrated.** Reported with the licence status; the licensing team creates the LMS schedule manually. `P`: rate-only schedules (S8d), 14 extant, 7 surrendered, 1 expired. Others: 17 of 18 extant (all but DL5); all but CE1 have an `E`. |
| F | **`T(1)` and `T(3)` = `E`, no `T(2)`** | 4: P1800, PEDL143, PEDL189, PEDL235. **Plus PL235** (`1@1987-10-28, 3@2035-10-28`) | PEDL189: `1@2025-07-01, 3@2039-07-01` | **Decided (business rule).** A skipped second term: **Initial** = `ORIGIN_DATE`→`T(1)`, **Third** = `T(1)`→`T(3)`. No Second term. |
| G | **`T(2)` only, `E` later** | 1: PEDL141 | `2@2013-10-01`, `E` 2031-10-01 | **Decided (business rule), then pattern B.** **Second** = `ORIGIN_DATE` (2004-10-01)→`T(2)` (2013). No Third term is added. The expiry keeps `E` (2031-10-01), and the licence is reported. |

**Other types, clean (16 of 54):**
- All 3 `AL` (`1`, `2`, `3` = `E`).
- 10 of the 12 `EXL` (`1`, `2`, `3` = `E`).
- PL178, PL179, PL182 (`1`, `2` = `E`).

PL205, PL213, PL215 and PL220 are also clean once their unnumbered `T` is ignored (pattern D). The
other types' terms run on the classic onshore shape: a 4–6 year initial term, then a second term,
then often a long third.

**"Dates out of order" is confirmed to come from the unnumbered `T`:** on the other types, every
schedule flagged `DATES_OUT_OF_ORDER` is one of the 8 with `TERM_WITHOUT_NUMBER`. The `?` sits on
`ORIGIN_DATE`, and the numbered terms are in order.

**All patterns are now decided.** The business rule (confirmed after the survey) is that a licence
schedule doesn't need every term (e.g. only Term 2 and Term 3), but the terms present must be in
order. So a **numbering gap is not a data error**, and patterns A, F and G need no special reading:
the first term present starts on `ORIGIN_DATE` and each `T(n)` ends term n, as everywhere else. The
others were decided separately:
- B is migrated as it is, with the expiry and final term end different, and reported.
- C gets a Fourth term in LMS, for these licences only.
- D ignores `T` events with no number.
- E is not migrated; it is reported, and the licensing team creates those schedules manually.

The plan's "Term boundaries" rules 3 and 5–7 and "Decision: four-term licences" hold them.

### S8c. Phases on schedules with no `T(1)` (pattern A check)

Shows whether the schedules with terms but no `T(1)` have phase events whose last boundary could
stand in for the end of the initial term.

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type || plm.licence_no licence_reference, plsd.xml_data
       , x.origin origin_date
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE' PASSING plsd.xml_data COLUMNS origin VARCHAR2(20) PATH 'ORIGIN_DATE') x
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')
  AND XMLEXISTS('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]' PASSING plsd.xml_data)
  AND NOT XMLEXISTS('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and normalize-space(TERM_NO) = "1" and not(DUMMY_EVENT = "true")]' PASSING plsd.xml_data)
)
SELECT s.licence_reference
     , s.origin_date
     , (SELECT LISTAGG(NVL(TRIM(p.phase), '?') || '@' || p.event_date, ', ') WITHIN GROUP (ORDER BY p.event_date)
        FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "P" and not(DUMMY_EVENT = "true")]'
               PASSING s.xml_data
               COLUMNS phase VARCHAR2(5) PATH 'PHASE', event_date VARCHAR2(20) PATH 'EVENT_DATE') p) phase_sequence
     , (SELECT LISTAGG(NVL(TRIM(t.term_no), '?') || '@' || t.event_date, ', ') WITHIN GROUP (ORDER BY t.event_date)
        FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]'
               PASSING s.xml_data
               COLUMNS term_no VARCHAR2(10) PATH 'TERM_NO', event_date VARCHAR2(20) PATH 'EVENT_DATE') t) term_sequence
     , (SELECT MAX(e.event_date)
        FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "E" and not(DUMMY_EVENT = "true")]'
               PASSING s.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') e) expiry_date
FROM sched s
ORDER BY s.licence_reference;
```

**Result (live, 27 October 2026):** 50 schedules (49 `P`, plus PEDL141). **None has any phase
event**, so the "last phase boundary" idea for pattern A is wrong.

| Shape | Licences |
|---|---|
| `2@x, 3@E`, no phases | 49 `P`: P2341, P2448–P2460, P2494, P2498, P2505, P2508, P2513, P2518, P2520, P2532, P2535, P2538, P2550, P2571, P2573–P2577, P2579, P2580, P2596, P2601, P2608, P2609, P2611–P2613, P2616, P2618–P2620, P2624, P2662, P2673, P2678, P2681 |
| `2@x`, `E` later | PEDL141: `2@2013-10-01`, `E` 2031-10-01 |

**Reading (decided by business rule):** terms may be skipped but must be in order. These schedules
have **no Initial term**. The first term present (Second) starts on `ORIGIN_DATE`, and each `T(n)`
ends term n, the same convention as every other schedule (e.g. P2244, P2522):

| Licence | `ORIGIN_DATE` | `T(2)` | `T(3)` = `E` | LMS terms |
|---|---|---|---|---|
| P2505 | 2020-12-01 | 2027-12-01 | 2042-12-01 | Second 2020→2027; Third 2027→2042 |
| P2449 | 2018-10-01 | 2030-10-01 | 2040-10-01 | Second 2018→2030; Third 2030→2040 |
| PEDL141 | 2004-10-01 | 2013-10-01 | `E` 2031-10-01 | Second 2004→2013; expiry 2031 kept as it is (pattern B: reported) |

Because none of these schedules has phases, LMS's phase checks (which look up the Initial term) are
never reached for them. (An earlier candidate read `T(n)` here as the **start** of term n, giving
Initial and Second. The business rule replaces it.)

### S8d. Schedules with no terms (pattern E): status and content

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
SELECT plm.licence_type || plm.licence_no licence_reference
     , cs.licence_status
     , x.origin_date
     , (SELECT MAX(e.event_date)
        FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "E" and not(DUMMY_EVENT = "true")]'
               PASSING plsd.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') e) expiry_date
     , (SELECT LISTAGG(c.event_mnem || ':' || c.cnt, ', ') WITHIN GROUP (ORDER BY c.event_mnem)
        FROM (SELECT ev.event_mnem, COUNT(*) cnt
              FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) != "" and not(DUMMY_EVENT = "true")]'
                     PASSING plsd.xml_data COLUMNS event_mnem VARCHAR2(10) PATH 'EVENT_MNEM') ev
              GROUP BY ev.event_mnem) c) events_by_type
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
, XMLTABLE('/SCHEDULE' PASSING plsd.xml_data COLUMNS origin_date VARCHAR2(20) PATH 'ORIGIN_DATE') x
WHERE plsd.end_datetime IS NULL
AND plm.licence_type IN ('P', 'PEDL')
AND NOT XMLEXISTS('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]' PASSING plsd.xml_data)
ORDER BY 1;
```

**Result (live, 27 October 2026):** 22 `P` schedules. **Every one holds a single `R` event and
nothing else.** Three also have an `E`; none has terms.

| Status | Licences |
|---|---|
| `E` (extant) | 14: P249, P251, P256, P257, P291, P292, P293, P295, P296, P324, P416 (1976–1982), and P2333, P2495, P2643 (2017–2024) |
| `S` (surrendered) | 7: P252 (`E` 2016-06-08), P258, P288 (`E` 2016-04-06), P404, P474, P2334, P2349 |
| `X` (expired) | 1: P294 (`E` 2018-03-15) |

**Reading:**

- These are rate-only schedules: one rate, presumably recurring yearly, with no term structure.
  The older extant ones are 1970s–80s licences with no expiry at all.
- LMS anchors every rate to a term or phase, so they cannot migrate as they are.
- **Decided:** these schedules are not migrated. They are reported with their licence status, and
  the licensing team creates their LMS schedules manually. Until then they have no
  `ped_lms_schedule_sync` row, so PEARS keeps owning them.
- An Initial term from `ORIGIN_DATE` to `E` was considered, but it only works for the 3 with an
  `E`, and none of those three is extant.

## S9. Phase structure (current versions)

Checks the phase rules in the plan ("Phase boundaries"): each `P` names the phase ending on its
date, phases are A, B, C in date order, phases may be skipped, and the last phase ends on the end of
the initial term. Groups schedules by the phases present and how the last phase relates to `T(1)`.

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type, plsd.xml_data
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')
  AND XMLEXISTS('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "P" and not(DUMMY_EVENT = "true")]' PASSING plsd.xml_data)
)
, phases AS (
  SELECT s.plsd_id
       , UPPER(TRIM(p.phase)) phase
       , TO_DATE(p.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') event_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "P" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS phase VARCHAR2(10) PATH 'PHASE', event_date VARCHAR2(20) PATH 'EVENT_DATE') p
)
, terms AS (
  SELECT s.plsd_id
       , MAX(CASE WHEN TRIM(t.term_no) = '1' THEN TO_DATE(t.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') END) t1_date
       , MIN(CASE WHEN TRIM(t.term_no) = '2' THEN TO_DATE(t.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') END) t2_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS term_no VARCHAR2(10) PATH 'TERM_NO', event_date VARCHAR2(20) PATH 'EVENT_DATE') t
  GROUP BY s.plsd_id
)
, per_schedule AS (
  SELECT s.plsd_id, s.licence_type
       , LISTAGG(NVL(p.phase, '?'), '') WITHIN GROUP (ORDER BY p.event_date, p.phase) phases_in_date_order
       , COUNT(*) p_events
       , COUNT(DISTINCT p.phase) distinct_phases
       , SUM(CASE WHEN p.phase IS NULL OR p.phase NOT IN ('A', 'B', 'C') THEN 1 ELSE 0 END) bad_phase_value
       , SUM(CASE WHEN p.event_date IS NULL THEN 1 ELSE 0 END) p_no_date
       , MAX(p.event_date) last_p_date
       , MAX(t.t1_date) t1_date
       , MAX(t.t2_date) t2_date
  FROM sched s
  JOIN phases p ON p.plsd_id = s.plsd_id
  LEFT JOIN terms t ON t.plsd_id = s.plsd_id
  GROUP BY s.plsd_id, s.licence_type
)
SELECT licence_type
     , phases_in_date_order
     , CASE
         WHEN bad_phase_value > 0 THEN 'PHASE_NOT_A_B_C'
         WHEN p_events > distinct_phases THEN 'DUPLICATE_PHASE'
         WHEN p_no_date > 0 THEN 'PHASE_WITHOUT_DATE'
         WHEN t1_date IS NOT NULL AND last_p_date = t1_date THEN 'LAST_P_EQUALS_T1'
         WHEN t1_date IS NULL AND t2_date IS NOT NULL AND last_p_date = t2_date THEN 'LAST_P_EQUALS_T2_NO_T1'
         WHEN t1_date IS NOT NULL AND last_p_date < t1_date THEN 'LAST_P_BEFORE_T1'
         WHEN t1_date IS NOT NULL AND last_p_date > t1_date THEN 'LAST_P_AFTER_T1'
         WHEN t1_date IS NULL AND t2_date IS NULL THEN 'NO_T1_OR_T2'
         ELSE 'OTHER'
       END last_phase_vs_initial_term_end
     , COUNT(*) schedules
FROM per_schedule
GROUP BY licence_type, phases_in_date_order
       , CASE
           WHEN bad_phase_value > 0 THEN 'PHASE_NOT_A_B_C'
           WHEN p_events > distinct_phases THEN 'DUPLICATE_PHASE'
           WHEN p_no_date > 0 THEN 'PHASE_WITHOUT_DATE'
           WHEN t1_date IS NOT NULL AND last_p_date = t1_date THEN 'LAST_P_EQUALS_T1'
           WHEN t1_date IS NULL AND t2_date IS NOT NULL AND last_p_date = t2_date THEN 'LAST_P_EQUALS_T2_NO_T1'
           WHEN t1_date IS NOT NULL AND last_p_date < t1_date THEN 'LAST_P_BEFORE_T1'
           WHEN t1_date IS NOT NULL AND last_p_date > t1_date THEN 'LAST_P_AFTER_T1'
           WHEN t1_date IS NULL AND t2_date IS NULL THEN 'NO_T1_OR_T2'
           ELSE 'OTHER'
         END
ORDER BY licence_type, schedules DESC;
```

`phases_in_date_order` shows which phases are present, in date order (e.g. `ABC`, `AC`, `A`; `?` =
no phase letter). The expected result is `LAST_P_EQUALS_T1` throughout, as on licence P2522.

**Result (live, 27 October 2026):**

| LICENCE_TYPE | PHASES (date order) | LAST PHASE vs INITIAL TERM END | SCHEDULES |
|---|---|---|---|
| P | AC | LAST_P_EQUALS_T1 | 222 |
| P | ABC | LAST_P_EQUALS_T1 | 56 |
| P | C | LAST_P_EQUALS_T1 | 16 |
| P | BC | LAST_P_EQUALS_T1 | 15 |

**Reading:**

- **309 current `P` schedules have phases, and every one passes:** the last phase ends exactly on
  `T(1)`. Phases are always in A → B → C order, with no duplicates, no missing dates and no other
  letters. No `PEDL` schedule has phases.
- **Skipped phases are common:** `AC` (no B) is the most frequent shape, as on licence P2522. The
  plan's rule (only phases with a `P` event are created, chained in A/B/C order) handles all four
  shapes.
- **`C` only (16) and `BC` (15):** under the rule, the first phase present starts on `ORIGIN_DATE`.
  So with `C` only, Phase C covers the whole initial term; with `BC`, Phase B starts on
  `ORIGIN_DATE`. **Decided (business rule):** phases may be missing (e.g. only A and C) but must be
  in order, so this is the intended meaning.
- The phase rule in the plan ("Phase boundaries") is confirmed by live data.
- **Other types (`PL`, `EXL`, `ML`, `DL`, `AL`, `CE`): no phase events at all**, in any version,
  current or historical (checked 27 October 2026 with a cut-down query: every `P` event on a
  licence type other than `P`/`PEDL`, grouped by licence; no rows).

## S10. `ORIGIN_DATE` (all versions)

Checks the schedule start date on every version: missing (the plan borrows the nearest version's
for history, and skips the licence if none has one), unparseable, and whether it matches the
licence's own start date in `xview_ped_ld_current`. The payments report uses the latter, and new
licences default the LMS start date from it.

```sql
WITH licence_start AS (
  SELECT ped_lm_id, MIN(licence_start_date) licence_start_date, COUNT(DISTINCT licence_start_date) start_dates
  FROM pedmgr.xview_ped_ld_current
  GROUP BY ped_lm_id
)
, versions AS (
  SELECT plm.licence_type
       , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
       , x.origin_text
       , TO_DATE(x.origin_text DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') origin_date
       , ls.licence_start_date
       , ls.start_dates
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  LEFT JOIN licence_start ls ON ls.ped_lm_id = plm.id
  , XMLTABLE('/SCHEDULE' PASSING plsd.xml_data COLUMNS origin_text VARCHAR2(30) PATH 'ORIGIN_DATE') x
  WHERE plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
SELECT licence_type
     , version_state
     , CASE
         WHEN TRIM(origin_text) IS NULL THEN 'ORIGIN_MISSING'
         WHEN origin_date IS NULL THEN 'ORIGIN_UNPARSEABLE'
         WHEN licence_start_date IS NULL THEN 'NO_LICENCE_START_DATE'
         WHEN start_dates > 1 THEN 'SEVERAL_LICENCE_START_DATES'
         WHEN origin_date = TRUNC(licence_start_date) THEN 'MATCHES_LICENCE_START'
         ELSE 'DIFFERS_FROM_LICENCE_START'
       END origin_state
     , COUNT(*) versions
FROM versions
GROUP BY licence_type, version_state
       , CASE
           WHEN TRIM(origin_text) IS NULL THEN 'ORIGIN_MISSING'
           WHEN origin_date IS NULL THEN 'ORIGIN_UNPARSEABLE'
           WHEN licence_start_date IS NULL THEN 'NO_LICENCE_START_DATE'
           WHEN start_dates > 1 THEN 'SEVERAL_LICENCE_START_DATES'
           WHEN origin_date = TRUNC(licence_start_date) THEN 'MATCHES_LICENCE_START'
           ELSE 'DIFFERS_FROM_LICENCE_START'
         END
ORDER BY 1, 2, 3;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | VERSION_STATE | ORIGIN_STATE | VERSIONS |
|---|---|---|---|
| P | current | MATCHES_LICENCE_START | 1,519 |
| P | current | DIFFERS_FROM_LICENCE_START | **5** |
| P | historical | MATCHES_LICENCE_START | 5,711 |
| P | historical | DIFFERS_FROM_LICENCE_START | 73 |
| P | historical | ORIGIN_MISSING | **1,004** |
| PEDL | current | MATCHES_LICENCE_START | 220 |
| PEDL | historical | MATCHES_LICENCE_START | 1,333 |
| PEDL | historical | DIFFERS_FROM_LICENCE_START | 307 |
| PEDL | historical | ORIGIN_MISSING | **67** |
| PL | current | MATCHES_LICENCE_START | 25 |
| PL | current | DIFFERS_FROM_LICENCE_START | **1** |
| PL | historical | MATCHES_LICENCE_START | 282 |
| PL | historical | DIFFERS_FROM_LICENCE_START | 2 |
| PL | historical | ORIGIN_MISSING | **26** |
| EXL | current | MATCHES_LICENCE_START | 12 |
| EXL | historical | MATCHES_LICENCE_START | 67 |
| EXL | historical | ORIGIN_MISSING | **12** |
| ML | current | MATCHES_LICENCE_START | 6 |
| ML | current | DIFFERS_FROM_LICENCE_START | **1** |
| ML | historical | MATCHES_LICENCE_START | 56 |
| ML | historical | DIFFERS_FROM_LICENCE_START | 10 |
| ML | historical | ORIGIN_MISSING | **7** |
| DL | current | MATCHES_LICENCE_START | 5 |
| DL | historical | MATCHES_LICENCE_START | 43 |
| DL | historical | ORIGIN_MISSING | **5** |
| AL | current | MATCHES_LICENCE_START | 3 |
| AL | historical | MATCHES_LICENCE_START | 24 |
| AL | historical | ORIGIN_MISSING | **3** |
| CE | current | MATCHES_LICENCE_START | 1 |
| CE | historical | MATCHES_LICENCE_START | 3 |
| CE | historical | ORIGIN_MISSING | **1** |

(Totals reconcile with S2 for every type: `P` 8,312, `PEDL` 1,927, `PL` 336, `EXL` 91, `ML` 80,
`DL` 53, `AL` 30, `CE` 5.)

**Reading:**

- **Current versions:** all have an `ORIGIN_DATE`, and all but **7** match the licence start date:
  5 `P`, PL220 and ML6 (S10b). The difference is legitimate (L3).
  ML6 has no terms (as no `ML`
  schedule does), so it isn't migrated (pattern E).
- **1,125 historical versions have no `ORIGIN_DATE`** (1,004 `P`, 67 `PEDL`, 54 other types),
  about 10% of the history. On the other types it is **exactly one per licence**: 26 `PL`, 12
  `EXL`, 7 `ML`, 5 `DL`, 3 `AL`, 1 `CE`. That fits the reading that these are the first versions
  saved when a template was attached at raise, before `licence_raise` set the start date. Their
  `EVENT_DATE`s were calculated from PEARS's `1900-01-01` base (as seen in S6), so the dates PEARS
  stored for them are not real.
- **392 historical versions differ** from today's licence start date (73 `P`, 307 `PEDL`, 2 `PL`,
  10 `ML`). The start date was changed later. History keeps the date each version had, which is
  what the migration copies.
- **Decided (27 October 2026)** for the versions with no start date: migrate them with the borrowed
  start date, and mark them "No start date in PEARS" in the comparison report rather than as date
  mismatches. See the plan, "Schedule history".

### S10b. Current versions whose start date differs from the licence start date

```sql
SELECT plm.licence_type || plm.licence_no licence_reference
     , plsd.id plsd_id
     , x.origin_text schedule_origin_date
     , (SELECT LISTAGG(TO_CHAR(xplc.licence_start_date, 'YYYY-MM-DD'), ', ') WITHIN GROUP (ORDER BY xplc.licence_start_date)
        FROM pedmgr.xview_ped_ld_current xplc WHERE xplc.ped_lm_id = plm.id) licence_start_dates
     , (SELECT LISTAGG(DISTINCT xplc.licence_status, ', ') WITHIN GROUP (ORDER BY xplc.licence_status)
        FROM pedmgr.xview_ped_ld_current xplc WHERE xplc.ped_lm_id = plm.id) licence_statuses
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE' PASSING plsd.xml_data COLUMNS origin_text VARCHAR2(30) PATH 'ORIGIN_DATE') x
WHERE plsd.end_datetime IS NULL
AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
AND NOT EXISTS (
  SELECT 1 FROM pedmgr.xview_ped_ld_current xplc
  WHERE xplc.ped_lm_id = plm.id
  AND TRUNC(xplc.licence_start_date) = TO_DATE(x.origin_text DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD')
)
ORDER BY 1;
```

**Result (live, 27 October 2026):**

| Licence | Schedule `ORIGIN_DATE` | Licence start date | Difference |
|---|---|---|---|
| P1064 | 2003-12-01 | 2003-10-01 | schedule 2 months later |
| P1930 | 2012-05-21 | 2012-07-01 | schedule ~6 weeks earlier |
| P2138 | 2015-05-10 | 2015-02-10 | schedule 3 months later (month 05 vs 02, possibly a typo) |
| P2139 | 2015-05-10 | 2015-02-10 | as P2138 |
| P2349 | 2018-07-01 | 2018-10-01 | schedule 3 months earlier (also pattern E: rate-only, no terms) |
| PL220 | 1982-08-09 | 1982-08-02 | schedule 1 week later (PL213 and PL215, from the same date, have 1982-08-09 on both); extant |
| ML6 | 1965-04-01 | 1964-12-27 | schedule ~3 months later (ML4 and ML7 start 1965-04-01, ML5 1964-12-27: possibly copied from a sibling); extant; no terms, so not migrated (pattern E) |

Each licence has several "current" licence-detail rows, one per data point, all with the same start
date (and statuses `E` extant and `S` surrendered across them, as noted earlier).

**Reading:**

- **The migration is unaffected.** It takes `ORIGIN_DATE` as the LMS start date, so LMS calculates
  the same dates PEARS shows. The write-back writes the same `ORIGIN_DATE` back.
- **Decided (28 October 2026):** a schedule start date that differs from the licence start date
  is a legitimate use case, not a data error (L3). Nothing to fix in PEARS, and the difference is
  not reported.

## S11. Rate values

Classifies every typed `R` event's `RATE` text by the plan's "Rate values" rules: whole number,
1–2 decimal places, more than 2 (to be rounded), or does not fit (empty, non-numeric, negative).

```sql
WITH rates AS (
  SELECT plm.licence_type
       , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
       , TRIM(r.rate) rate_text
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "R" and not(DUMMY_EVENT = "true")]'
      PASSING plsd.xml_data COLUMNS rate VARCHAR2(100) PATH 'RATE') r
  WHERE plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
, classified AS (
  SELECT licence_type, version_state, rate_text
       , CASE
           WHEN rate_text IS NULL THEN 'DOES_NOT_FIT_EMPTY'
           WHEN NOT REGEXP_LIKE(rate_text, '^-?[0-9]+(\.[0-9]+)?$') THEN 'DOES_NOT_FIT_NOT_NUMERIC'
           WHEN rate_text LIKE '-%' THEN 'DOES_NOT_FIT_NEGATIVE'
           WHEN INSTR(rate_text, '.') = 0 THEN 'WHOLE'
           WHEN LENGTH(rate_text) - INSTR(rate_text, '.') <= 2 THEN 'DECIMAL_1_2_DP'
           ELSE 'DECIMAL_MORE_THAN_2_DP'
         END rate_class
  FROM rates
)
SELECT licence_type, version_state, rate_class
     , COUNT(*) events
     , COUNT(DISTINCT rate_text) distinct_values
     , MIN(rate_text) KEEP (DENSE_RANK FIRST ORDER BY rate_text) example_value
FROM classified
GROUP BY licence_type, version_state, rate_class
ORDER BY 1, 2, 3;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | VERSION_STATE | RATE_CLASS | EVENTS | DISTINCT VALUES | EXAMPLE |
|---|---|---|---|---|---|
| P | current | WHOLE | 20,524 | 138 | 0 |
| P | current | DOES_NOT_FIT_EMPTY | **2** | 0 | |
| P | historical | WHOLE | 90,947 | 126 | 0 |
| P | historical | DOES_NOT_FIT_EMPTY | **10** | 0 | |
| PEDL | current | WHOLE | 4,532 | 16 | 100 |
| PEDL | historical | WHOLE | 34,989 | 29 | 100 |
| PL | current | WHOLE | 236 | 30 | 1 |
| PL | current | DOES_NOT_FIT_NOT_NUMERIC | **17** | 9 | `.06` |
| PL | historical | WHOLE | 2,702 | 30 | 1 |
| PL | historical | DOES_NOT_FIT_NOT_NUMERIC | **214** | 10 | `.06` |
| EXL | current | WHOLE | 252 | 16 | 100 |
| EXL | historical | WHOLE | 1,659 | 16 | 100 |
| ML | current | DECIMAL_1_2_DP | **50** | 10 | 111.12 |
| ML | current | DOES_NOT_FIT_NOT_NUMERIC | **18** | 11 | `.06` |
| ML | historical | DECIMAL_1_2_DP | **530** | 10 | 111.12 |
| ML | historical | DOES_NOT_FIT_NOT_NUMERIC | **180** | 11 | `.06` |
| DL | current | WHOLE | 6 | 2 | 1200 |
| DL | historical | WHOLE | 53 | 2 | 1200 |
| AL | current | WHOLE | 48 | 16 | 100 |
| AL | historical | WHOLE | 432 | 16 | 100 |

`CE` has no `R` events.

**Reading:**

- **Every populated `P` and `PEDL` rate is a whole number.** No decimals, nothing over 2 dp,
  nothing non-numeric and nothing negative. `EXL`, `DL` and `AL` are whole numbers too.
- **`PL` and `ML` are different:**
  - `ML` has rates with pence (e.g. 111.12).
  - Both have values the check calls "not numeric", such as `.06`. **S11c shows they are all
    decimals with no leading zero** (`.06` to `.5`). The check's pattern needs a digit before the
    point, but the plan's parser (`new BigDecimal(trimmed)`) reads them correctly (`.5` → 0.50). So
    they are valid rates, not bad data.
  - `ML` is not migrated anyway (no terms, pattern E).
- **On migrated schedules every rate fits.** The only values with more than 2 dp (`.175`, `.215`)
  are on ML5 and PL89, which have no terms and aren't migrated (S11c). So the rounding rule is
  still never triggered. The write-back writes whole numbers without decimals and pence with 2 dp
  (`.5` → `0.50`).
- **`0` is a real rate value** (the lowest `P` value). It is valid in LMS (0 or more) and must be
  migrated as a rate, not treated as empty.
- **12 `R` events have an empty `RATE`** (2 current, 10 historical, all `P`). Under the plan they
  are not migrated and are reported. S11b lists them.

### S11b. `R` events with an empty rate

```sql
SELECT plm.licence_type || plm.licence_no licence_reference
     , plsd.id plsd_id
     , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
     , plsd.start_datetime version_from
     , r.event_date, r.years || 'y ' || r.months || 'm ' || r.days || 'd' offset_from_start
     , r.recurring, r.recurrances, r.comments
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "R" and normalize-space(RATE) = "" and not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data
    COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE', years VARCHAR2(5) PATH 'YEARS'
          , months VARCHAR2(5) PATH 'MONTHS', days VARCHAR2(5) PATH 'DAYS'
          , recurring VARCHAR2(20) PATH 'RECURRING', recurrances VARCHAR2(10) PATH 'RECURRANCES'
          , comments VARCHAR2(4000) PATH 'COMMENTS') r
WHERE plm.licence_type IN ('P', 'PEDL')
ORDER BY 1, plsd.start_datetime, r.event_date;
```

**Result (live, 27 October 2026):** 12 events on **2 licences**, P293 (7 versions) and P295 (5
versions). Each is the schedule's only `R`, dated on the licence start (1979-04-27, 1979-01-17),
with no rate, offset, recurrence or comments. The earliest version of each also has an undated copy.

**Reading:** both licences are pattern E (S8d: rate-only, no terms), so their schedules are not
migrated anyway. Once these are excluded, every `R` event on a migrated schedule has a valid rate
(S11, S11c).

### S11c. Decimal and leading-dot rates on `PL` and `ML`

```sql
SELECT plm.licence_type
     , TRIM(r.rate) rate_text
     , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END version_state
     , COUNT(*) events
     , COUNT(DISTINCT plsd.ped_lm_id) licences
     , MIN(plm.licence_type || plm.licence_no) example_licence
     , MIN(r.comments) KEEP (DENSE_RANK FIRST ORDER BY plsd.end_datetime DESC NULLS FIRST) example_comment
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "R" and not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data
    COLUMNS rate VARCHAR2(100) PATH 'RATE', comments VARCHAR2(4000) PATH 'COMMENTS') r
WHERE plm.licence_type IN ('PL', 'ML')
AND (INSTR(TRIM(r.rate), '.') > 0 OR NOT REGEXP_LIKE(TRIM(r.rate), '^-?[0-9]+(\.[0-9]+)?$'))
GROUP BY plm.licence_type, TRIM(r.rate)
       , CASE WHEN plsd.end_datetime IS NULL THEN 'current' ELSE 'historical' END
ORDER BY 1, 2, 3;
```

**Result (live, 27 October 2026; current events, historical counts in brackets):**

| Type | Values | Licences | Migrated? |
|---|---|---|---|
| ML | `.06`, `.1`, `.14`, `.175`, `.21`, `.215`, `.25`, `.3`, `.35`, `.4`, `.45`: 1–2 each (8–20) | 2–3 licences, e.g. ML5, ML6 | No (no terms) |
| ML | `17.3`, `24.71`, `32.12`, `43.24`, `51.89`, `61.78`, `74.13`, `86.49`, `98.84`, `111.12`: 5 each (53) | 5 licences, e.g. ML18 | No (no terms) |
| PL | `.06`, `.1`, `.175`, `.215`, `.25`, `.3`, `.35`, `.4`: 1 each (13); `.45` historical only (11) | PL89 | No (no terms) |
| PL | `.5`: 9 (99) | 9 licences, e.g. PL116 | **Yes** |

No comments on any of them.

**Reading:**

- **Every value is a valid decimal.** None is text, and none is negative. The "not numeric" flag
  in S11 only came from the missing leading zero.
- **They look like stepped per-unit rentals** (e.g. `.06` rising to `.45` on ML5 and PL89, and a
  `17.3` → `111.12` scale on 5 `ML` licences). Like every PEARS rate, the payments report
  multiplies them by area. The value itself migrates as it is.
- **Only `.5` on 9 `PL` licences is actually migrated.** It becomes `rentalRate` 0.50. All the
  others are on `ML` or PL89, which have no terms and aren't migrated.
- **3 dp values** (`.175`, `.215`) occur only on ML5 and PL89, so the plan's rounding rule is not
  triggered on any migrated schedule.

## S12. Rate recurrence and gaps (current versions)

Classifies each current `R` event by recurrence and finds **gaps**: licence anniversaries with no
rate in force under PEARS's payments logic. A gap is either between two rates, where the next rate
starts more than a year after this rate's last occurrence, or at the end, where the last rate stops
before the licence's final boundary. The final boundary is the last `T` or `E`, whichever is later.
Gaps matter because LMS carries a rate on until the next one, so after write-back those years would
gain payments (see the plan, "Rates and the PEARS write-back").

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type, plm.licence_type || plm.licence_no licence_reference, plsd.xml_data
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
, rates AS (
  SELECT s.plsd_id, s.licence_type, s.licence_reference
       , TO_DATE(r.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') event_date
       , UPPER(TRIM(r.recurring)) recurring
       , TO_NUMBER(TRIM(r.recurrances) DEFAULT NULL ON CONVERSION ERROR) recurrances
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "R" and normalize-space(RATE) != "" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data
      COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE', recurring VARCHAR2(20) PATH 'RECURRING'
            , recurrances VARCHAR2(10) PATH 'RECURRANCES') r
)
, final_boundary AS (
  SELECT s.plsd_id
       , MAX(TO_DATE(b.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD')) boundary_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = ("T", "E") and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') b
  GROUP BY s.plsd_id
)
, rate_coverage AS (
  SELECT r.*
       , CASE
           WHEN r.recurring IS NULL THEN 'NOT_RECURRING'
           WHEN r.recurring = 'YEARLY' AND r.recurrances IS NULL THEN 'YEARLY_INDEFINITE'
           WHEN r.recurring = 'YEARLY' THEN 'YEARLY_WITH_COUNT'
           ELSE 'OTHER_RECURRING'
         END recurrence_class
       , CASE
           WHEN r.recurring = 'YEARLY' AND r.recurrances IS NULL THEN NULL
           ELSE ADD_MONTHS(r.event_date, 12 * NVL(CASE WHEN r.recurring = 'YEARLY' THEN r.recurrances END, 0))
         END last_occurrence
       , LEAD(r.event_date) OVER (PARTITION BY r.plsd_id ORDER BY r.event_date) next_rate_date
       , fb.boundary_date
  FROM rates r
  LEFT JOIN final_boundary fb ON fb.plsd_id = r.plsd_id
)
SELECT licence_type
     , recurrence_class
     , COUNT(*) r_events
     , COUNT(DISTINCT plsd_id) schedules
     , SUM(CASE WHEN next_rate_date IS NOT NULL AND last_occurrence IS NOT NULL
                 AND next_rate_date > ADD_MONTHS(last_occurrence, 12) THEN 1 ELSE 0 END) gap_before_next_rate
     , SUM(CASE WHEN next_rate_date IS NULL AND last_occurrence IS NOT NULL AND boundary_date IS NOT NULL
                 AND boundary_date > ADD_MONTHS(last_occurrence, 12) THEN 1 ELSE 0 END) gap_before_licence_end
     , SUM(CASE WHEN event_date IS NULL THEN 1 ELSE 0 END) no_date
FROM rate_coverage
GROUP BY ROLLUP(licence_type, recurrence_class)
ORDER BY GROUPING(licence_type), licence_type, GROUPING(recurrence_class), recurrence_class;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | RECURRENCE | R EVENTS | SCHEDULES | GAP BEFORE NEXT RATE | GAP BEFORE LICENCE END |
|---|---|---|---|---|---|
| P | NOT_RECURRING | 19,001 | 1,518 | **298** | **1** |
| P | YEARLY_INDEFINITE | 1,523 | 1,523 | 0 | 0 |
| PEDL | NOT_RECURRING | 4,312 | 220 | 0 | 0 |
| PEDL | YEARLY_INDEFINITE | 220 | 220 | 0 | 0 |
| PL | NOT_RECURRING | 209 | 22 | **13** | 0 |
| PL | YEARLY_INDEFINITE | 26 | 26 | 0 | 0 |
| PL | YEARLY_WITH_COUNT | **18** | **9** | 0 | 0 |
| EXL | NOT_RECURRING | 240 | 12 | 0 | 0 |
| EXL | YEARLY_INDEFINITE | 12 | 12 | 0 | 0 |
| ML | NOT_RECURRING | 61 | 7 | 1 | 0 |
| ML | YEARLY_INDEFINITE | 7 | 7 | 0 | 0 |
| DL | NOT_RECURRING | 1 | 1 | 1 | 0 |
| DL | YEARLY_INDEFINITE | 5 | 5 | 0 | 0 |
| AL | NOT_RECURRING | 42 | 3 | 0 | 0 |
| AL | YEARLY_INDEFINITE | 3 | 3 | 0 | 0 |
| AL | YEARLY_WITH_COUNT | **3** | **3** | 0 | 0 |
| **Total** | | **25,683** | **1,797** | **313** | **1** |

No `R` without a date, and no recurrence type other than `YEARLY`. `CE1` has no `R`.

**Reading:**

- **One shape on `P` and `PEDL`:** one non-recurring `R` per anniversary, ending in a single
  **indefinite yearly** rate. 1,523 of 1,524 `P` schedules and all 220 `PEDL` schedules have
  exactly one indefinite yearly rate. That matches licences P2244 and P2522, and the plan's
  write-back rule (the last LMS rate becomes an indefinite yearly `R`).
- **The other types end the same way.** Every schedule with rates has exactly one indefinite
  yearly rate.
- **But `RECURRANCES` with a count is used on `PL` and `AL`:** 18 events on 9 `PL` schedules, and
  1 on each of the 3 `AL`. They are yearly rates that stop after a set number of years, a shape
  the `P`/`PEDL` data never uses.
  - The plan's expansion (step 1, "stop at `k > RECURRANCES`") already handles them.
  - The write-back must be able to emit a `RECURRANCES` count, not just "non-recurring" and
    "indefinite".
  - S12b (other types) shows where they sit.
- **15 more gaps** before the next rate: 13 on `PL`, and 1 each on `ML` and `DL`. Those two aren't
  migrated (no terms).
- **`PEDL` has no gaps at all.**
- **298 gaps on `P` schedules**, each after a non-recurring rate: the next rate starts more than a
  year later, so the payments report shows no rate for the anniversaries in between. After the
  write-back, LMS carries the earlier rate on through those years, which would add payments. S12b
  lists them.
- **1 gap before the licence end:** a `P` schedule whose last rate is not recurring and stops
  before its final term or expiry. It is probably the one `P` schedule with no indefinite yearly
  rate. S12b shows it.
- `RECURRANCES` with a count is never used on `P` or `PEDL`, but it is on `PL` and `AL` (see above), so the write-back needs "non-recurring",
  "yearly, indefinite" and "yearly, N times".

### S12b. Rate gaps, one row per gap

Also flags an indefinite yearly rate that is **not** the last rate. The payments report would then
pay both the recurring rate and the later rates (its recurring and non-recurring branches are
independent).

```sql
WITH sched AS (
  SELECT plsd.id plsd_id, plm.licence_type || plm.licence_no licence_reference, plsd.xml_data
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL'), plus YEARLY_WITH_COUNT rows
)
, rates AS (
  SELECT s.plsd_id, s.licence_reference
       , TO_DATE(r.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') event_date
       , TRIM(r.rate) rate
       , UPPER(TRIM(r.recurring)) recurring
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "R" and normalize-space(RATE) != "" and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data
      COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE', rate VARCHAR2(50) PATH 'RATE'
            , recurring VARCHAR2(20) PATH 'RECURRING') r
)
, final_boundary AS (
  SELECT s.plsd_id, MAX(TO_DATE(b.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD')) boundary_date
  FROM sched s
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = ("T", "E") and not(DUMMY_EVENT = "true")]'
      PASSING s.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') b
  GROUP BY s.plsd_id
)
, ordered AS (
  SELECT r.*
       , LEAD(r.event_date) OVER (PARTITION BY r.plsd_id ORDER BY r.event_date) next_rate_date
       , LEAD(r.rate) OVER (PARTITION BY r.plsd_id ORDER BY r.event_date) next_rate
       , fb.boundary_date
  FROM rates r
  LEFT JOIN final_boundary fb ON fb.plsd_id = r.plsd_id
)
SELECT licence_reference
     , TO_CHAR(event_date, 'YYYY-MM-DD') rate_date
     , rate
     , NVL(recurring, '-') recurring
     , TO_CHAR(next_rate_date, 'YYYY-MM-DD') next_rate_date
     , next_rate
     , TO_CHAR(boundary_date, 'YYYY-MM-DD') final_boundary
     , CASE
         WHEN recurring = 'YEARLY' AND next_rate_date IS NOT NULL THEN 'INDEFINITE_RATE_NOT_LAST'
         WHEN next_rate_date IS NOT NULL THEN 'GAP_BEFORE_NEXT_RATE'
         ELSE 'GAP_BEFORE_LICENCE_END'
       END problem
     , TRUNC(MONTHS_BETWEEN(NVL(next_rate_date, boundary_date), event_date) / 12) - 1 missing_anniversaries
FROM ordered
WHERE (recurring = 'YEARLY' AND next_rate_date IS NOT NULL)
   OR (recurring IS NULL AND next_rate_date > ADD_MONTHS(event_date, 12))
   OR (recurring IS NULL AND next_rate_date IS NULL AND boundary_date > ADD_MONTHS(event_date, 12))
ORDER BY licence_reference, event_date;
```

`missing_anniversaries` is the approximate number of anniversaries with no rate in force.

**Result (live, 27 October 2026):** 299 rows (298 gaps before the next rate, 1 gap before licence
end, 1 indefinite rate that is not last). They are almost all **systematic, round-by-round
patterns**, not one-off errors:

| Pattern | Typical example | Licences |
|---|---|---|
| Rate at the licence start, next rate **6 years** later (5 anniversaries with no rate) | P516: `330@1985-06-14` → `400@1991-06-14` | Most licences from 1964–1998 rounds: P1–P99, P101–P244, P250, P254, P328–P363, P380, P449–P496, P516–P614, P622–P721, P724–P791, P803–P865, P886–P995, P1006–P1034 |
| Rate at start, next **4 years** later, then **3 years** later | P297: `100@1979` → `150@1983` → `250@1986` | P261–P287, P297–P317, P361 (1977–1980 rounds) |
| Rate at start, next **4 years** later (3 anniversaries with no rate) | P1083: `150@2003-10-01` → `300@2007-10-01` | P1083, P1084, P1154 (2003 round) |
| Next rate **2 years** later (1 with no rate) | P1715: `150@2011` → `300@2013`; P2644: `15@2024` → `15@2026`, `150@2028` → `300@2030` | P1715, P2644 |
| Other | P556: `330@1985` → `400@1993` (7); P799: `60@1991` → `20@1994` (2, rate goes down) | P556, P799 |
| Next rate less than a year after an off-anniversary date (0 missing) | P2490: `150@2025-02-15` → `300@2026-07-15`; P251, P295 (no final boundary) | P2490, P251, P295 |

**Anomaly: P2644.** The indefinite yearly rate `7500@2038-03-01` is followed by a non-recurring
`150@2039-03-01`, which in turn leaves a 12-year gap to the licence end (2052). The payments
report would pay 7500 every year from 2038 **and** 150 in 2039. That looks like a data error for the
licensing team.

**Reading:**

- The gaps appear to follow the **rental terms of each licensing round**: e.g. older Model Clauses with an
  initial rental, then annual rentals from the 6th anniversary. If so, PEARS deliberately has **no rate**
  in those years, and the payments report correctly shows no payment.
- **This affects the write-back design (Open question 7).** The plan's option A write-back turns
  each LMS rate into a yearly recurrence until the next rate. On its own, that would **invent
  payments** for every one of these gap years, on almost every pre-1999 licence.
- **Gap handling (plan, "Rates and yearly recurrence" step 4):** the licensing team confirms, per
  pattern, whether the gaps are:
  - (a) **errors**: the previous rate should have run on, so LMS keeps it in force and the
    write-back fills the gap years; or
  - (b) **legitimate no-payment years**: LMS uses a **£0 rate** for them, migrated in on the first
    missing anniversary and written back as 0.
  Either way, the write-back no longer invents payments. Checking the licence terms for one
  licence per pattern should settle it.

**Second run (other types).** The query was extended to also list every yearly rate with a count
(`YEARLY_WITH_COUNT`, shown as `xN`). 36 rows: 21 fixed-count rates and 15 gaps.

| Pattern | Licences | Example | Migrated? |
|---|---|---|---|
| **Fixed-count start-up rates:** `.5` yearly x2, then `1` yearly x2, then `20` | PL55, PL77, PL79, PL80, PL81, PL90, PL116, PL161, PL162 | PL116: `.5@1969-08-01 x2` → `1@1972-08-01 x2` → `20@1975-08-01` | Yes |
| **Fixed-count initial rate:** `25` yearly x6, then `50` | AL6, AL9, AL10 | AL10: `25@1995-06-26 x6` → `50@2001-06-26` | Yes |
| **Gap: `40` at start, `90` 4 years later** (3 anniversaries with no rate) | PL205, PL211, PL213, PL215, PL220, PL233, PL234, PL259 (1982–84) | PL205: `40@1982-02-15` → `90@1986-02-15` | Yes |
| **Gap: `110`, then `1200` 5 years later** | PL77, PL79, PL80, PL81 | PL77: `110@1983-01-13` → `1200@1988-01-13` | Yes |
| Gap: `1500`, then `1200` 20 years later | DL5 | `1500@1994-03-01` → `1200@2014-03-01` | No (no terms) |
| Gap: stepped per-unit rates | ML5 (`.1@1970` → `.175@1972`), PL89 (`.1@1974` → `.175@1976`) | | No (no terms) |

No indefinite rate is followed by another rate (no P2644-style anomaly), and there is no gap
before the licence end.

**Reading (other types):**

- **The `PL` fixed-count rates are contiguous.** `x2` from 1969 covers 1969, 1970 and 1971 (the
  first year plus two recurrences, as `PED012X` counts them), and the next rate starts in 1972.
  So "yearly until the next rate" is the same series. The plan's write-back would emit the same
  `x2`.
- **The `AL` count reaches the next rate's date.** `25` x6 from 1995 covers 1995 to 2001
  inclusive, and `50` also starts in 2001. So PEARS may pay both 25 and 50 in 2001, depending on
  how the payments report bounds the recurrence. The plan's write-back would emit `x5` (5 further
  anniversaries before the next rate) and drop the overlap. These are 1990s dates, so it only
  matters if the payments report is run for those years. The round-trip test should include an
  `AL` licence.
- **12 more deliberate-looking gaps on migrated `PL` schedules**, on round patterns like `P`'s.
  They join the 298 for the licensing team to confirm (L12).

## S13. Mandatory relinquishment percentages (current versions)

Input to the plan's decision on the mandatory relinquishment percentage (decided: its own LMS field). Counts `MR` events by:
- whether `PERCENTAGE` holds a number, other text, or nothing;
- whether the event is on the `T(1)` date. That one feeds `MAND_SURRENDER_AREA` in licence
  documents (`template_schedule.get_mand_relinquishment_perc`).
- whether the `COMMENTS` already state a percentage.

```sql
WITH mr AS (
  SELECT plm.licence_type
       , plsd.id plsd_id
       , TRIM(m.percentage) percentage
       , m.event_date
       , m.comments
       , (SELECT MAX(t.event_date)
          FROM XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "T" and normalize-space(TERM_NO) = "1" and not(DUMMY_EVENT = "true")]'
                 PASSING plsd.xml_data COLUMNS event_date VARCHAR2(20) PATH 'EVENT_DATE') t) t1_date
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "MR" and not(DUMMY_EVENT = "true")]'
      PASSING plsd.xml_data
      COLUMNS percentage VARCHAR2(4000) PATH 'PERCENTAGE', event_date VARCHAR2(20) PATH 'EVENT_DATE'
            , comments VARCHAR2(4000) PATH 'COMMENTS') m
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
SELECT licence_type
     , CASE
         WHEN percentage IS NULL THEN 'EMPTY'
         WHEN REGEXP_LIKE(percentage, '^[0-9]+(\.[0-9]+)?%?$') THEN 'NUMBER'
         ELSE 'OTHER_TEXT'
       END percentage_state
     , CASE WHEN event_date = t1_date THEN 'ON_T1' ELSE 'NOT_ON_T1' END t1_position
     , CASE WHEN REGEXP_LIKE(comments, '[0-9]+(\.[0-9]+)? ?%') THEN 'COMMENT_HAS_PERCENT' ELSE 'NO_PERCENT_IN_COMMENT' END comment_state
     , COUNT(*) mr_events
     , COUNT(DISTINCT plsd_id) schedules
     , LISTAGG(DISTINCT CASE WHEN REGEXP_LIKE(percentage, '^[0-9]+(\.[0-9]+)?%?$') THEN percentage END, ', ')
         WITHIN GROUP (ORDER BY CASE WHEN REGEXP_LIKE(percentage, '^[0-9]+(\.[0-9]+)?%?$') THEN percentage END) percentage_values
FROM mr
GROUP BY licence_type
       , CASE
           WHEN percentage IS NULL THEN 'EMPTY'
           WHEN REGEXP_LIKE(percentage, '^[0-9]+(\.[0-9]+)?%?$') THEN 'NUMBER'
           ELSE 'OTHER_TEXT'
         END
       , CASE WHEN event_date = t1_date THEN 'ON_T1' ELSE 'NOT_ON_T1' END
       , CASE WHEN REGEXP_LIKE(comments, '[0-9]+(\.[0-9]+)? ?%') THEN 'COMMENT_HAS_PERCENT' ELSE 'NO_PERCENT_IN_COMMENT' END
ORDER BY 1, 2, 3, 4;
```

**Result (live, 27 October 2026; current versions):**

| Type | `PERCENTAGE` | On `T(1)`? | % in comments? | `MR` events | Values |
|---|---|---|---|---|---|
| P | empty | no | yes | 8 | |
| P | empty | no | no | 267 | |
| P | empty | **yes** | yes | **713** | |
| P | empty | yes | no | 41 | |
| P | number | no | yes | 8 | 50, 75 |
| P | number | no | no | 95 | 0, 50, 75, 87.5 |
| P | number | **yes** | yes | **90** | 0, 50, 87.5, 100 |
| P | number | **yes** | no | **338** | 0, 50, `50%`, 75, 87.5 |
| P | other text | no | yes | 1 | |
| P | other text | no | no | **301** | |
| P | other text | yes | no | 1 | |
| PEDL | number | no | no | 6 | 50 |
| PEDL | number | **yes** | no | **149** | 50 |
| PL | number | **yes** | no | **8** | 50 |

Totals: `P` 1,863 (1,029 empty, 531 number, 303 other text); `PEDL` 155 (all number); `PL` 8 (all
number, all 50, on `T(1)`). No other type has an `MR` on its current schedule.

**Reading:**

- **585 `MR` events drive licence documents today:** a number on the `T(1)` date (428 `P`, 149
  `PEDL`, 8 `PL`). **Decided (28 October 2026):** the percentage is migrated into its own LMS field
  and written back, so these documents keep the "Mandatory Surrender Area is … X%" sentence.
- **713 `P` events hold the percentage only in comments**, with an empty `PERCENTAGE` on the `T(1)`
  date. Their licence documents already lack the sentence today. They migrate with an empty
  percentage, and the comment becomes the description, so the figure stays visible in LMS.
- **Values:** 0, 50, 75, 87.5, 100, plus one `50%` with a percent sign. **Noted in the plan's
  decision:** the LMS field allows 1 decimal place, and the migration strips a trailing `%`.
- **303 `P` events have other text in `PERCENTAGE`.** Likely the Fox validation message "You must
  enter this item" saved as data (as seen in S6). S13b checks.

### S13b. Non-numeric `PERCENTAGE` values

```sql
SELECT SUBSTR(TRIM(m.percentage), 1, 60) percentage_text
     , COUNT(*) mr_events
     , COUNT(DISTINCT plsd.ped_lm_id) licences
     , MIN(plm.licence_type || plm.licence_no) example_licence
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
, XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = "MR" and not(DUMMY_EVENT = "true")]'
    PASSING plsd.xml_data COLUMNS percentage VARCHAR2(4000) PATH 'PERCENTAGE') m
WHERE plsd.end_datetime IS NULL
AND plm.licence_type IN ('P', 'PEDL')
AND TRIM(m.percentage) IS NOT NULL
AND NOT REGEXP_LIKE(TRIM(m.percentage), '^[0-9]+(\.[0-9]+)?%?$')
GROUP BY SUBSTR(TRIM(m.percentage), 1, 60)
ORDER BY 2 DESC;
```

**Result (live, 27 October 2026):** all 303 are the single character **`x`**. There are no Fox
validation messages on current versions.

**Reading:** `x` is a placeholder (e.g. "percentage not set", or "not applicable"), not a value.
**Decided (27 October 2026):** the `x` value is not migrated. The relinquishment event is still
migrated with an empty percentage, and the `x` values are counted once in the report rather than
listed 303 times. Any other non-numeric text is still reported per event.

## S14. LMS vs PEARS date arithmetic

Input to the plan's Open question 6. For every typed event with a years/months/days offset (`RD`/`PA`
excluded) on current versions, it compares three dates:

- **stored**: `EVENT_DATE` as PEARS saved it;
- **PEARS formula**: `ADD_MONTHS(origin + days, years*12 + months)`
  (`template_schedule.get_julian_or_event_date`);
- **LMS formula**: `origin.plusYears(y).plusMonths(m).plusDays(d)`, emulated in SQL. Java clamps the
  day to the target month's length, while `ADD_MONTHS` also snaps a month-end date to month-end.

It counts events where the stored date is not the PEARS formula (a stale stored date), and events
where the LMS formula would land on a different day from the PEARS formula (the arithmetic
difference).

```sql
WITH ev AS (
  SELECT plm.licence_type
       , e.event_mnem
       , TO_DATE(x.origin DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') origin_date
       , TO_DATE(e.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD') stored_date
       , NVL(TO_NUMBER(TRIM(e.years)  DEFAULT NULL ON CONVERSION ERROR), 0) y
       , NVL(TO_NUMBER(TRIM(e.months) DEFAULT NULL ON CONVERSION ERROR), 0) m
       , NVL(TO_NUMBER(TRIM(e.days)   DEFAULT NULL ON CONVERSION ERROR), 0) d
       , TRIM(e.years) || TRIM(e.months) || TRIM(e.days) offset_text
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE' PASSING plsd.xml_data COLUMNS origin VARCHAR2(20) PATH 'ORIGIN_DATE') x
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) != "" and not(EVENT_MNEM = ("RD", "PA")) and not(DUMMY_EVENT = "true")]'
      PASSING plsd.xml_data
      COLUMNS event_mnem VARCHAR2(10) PATH 'EVENT_MNEM', event_date VARCHAR2(20) PATH 'EVENT_DATE'
            , years VARCHAR2(10) PATH 'YEARS', months VARCHAR2(10) PATH 'MONTHS', days VARCHAR2(10) PATH 'DAYS') e
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
, calc AS (
  SELECT ev.*
       , ADD_MONTHS(origin_date + d, y * 12 + m) pears_date
       -- Java LocalDate.plusYears().plusMonths(): same day of month, clamped to the target month's length
       , CASE
           WHEN EXTRACT(DAY FROM origin_date) <= EXTRACT(DAY FROM LAST_DAY(ADD_MONTHS(TRUNC(origin_date, 'MM'), y * 12 + m)))
             THEN ADD_MONTHS(TRUNC(origin_date, 'MM'), y * 12 + m) + (EXTRACT(DAY FROM origin_date) - 1)
           ELSE LAST_DAY(ADD_MONTHS(TRUNC(origin_date, 'MM'), y * 12 + m))
         END + d lms_date
  FROM ev
  WHERE origin_date IS NOT NULL
  AND offset_text IS NOT NULL
)
SELECT licence_type
     , event_mnem
     , COUNT(*) events_with_offset
     , SUM(CASE WHEN stored_date IS NULL THEN 1 ELSE 0 END) stored_date_missing
     , SUM(CASE WHEN stored_date != pears_date THEN 1 ELSE 0 END) stored_differs_from_pears_formula
     , SUM(CASE WHEN lms_date != pears_date THEN 1 ELSE 0 END) lms_formula_differs
     , MIN(CASE WHEN lms_date != pears_date THEN lms_date - pears_date END) min_day_difference
     , MAX(CASE WHEN lms_date != pears_date THEN lms_date - pears_date END) max_day_difference
FROM calc
GROUP BY ROLLUP(licence_type, event_mnem)
ORDER BY GROUPING(licence_type), licence_type, GROUPING(event_mnem), event_mnem;
```

The day differences are LMS date minus PEARS date: positive means LMS lands later.

**Result (live, 27 October 2026; current versions):**

| Type | Event | With offset | Stored ≠ PEARS formula | LMS formula ≠ PEARS | Day difference |
|---|---|---|---|---|---|
| P | 2M | 36 | 0 | 0 | |
| P | BM | 37 | 0 | 8 | −3 to −1 |
| P | DD | 215 | 0 | 2 | −1 |
| P | E | 1,339 | 0 | 1 | +3 |
| P | MR | 1,863 | 0 | 7 | −3 to +1 |
| P | P | 658 | 0 | 0 | |
| P | R | 20,524 | **1** | 61 | −1 to +3 |
| P | T | 4,075 | 0 | 15 | −3 to +1 |
| **P total** | | **28,747** | **1** | **94** | −3 to +3 |
| PEDL | all | 5,592 | 0 | 0 | |
| PL | all (`E`, `MR`, `R`, `T`) | 319 | 0 | 0 | |
| EXL | `E` | 12 | 0 | **1** | +1 |
| EXL | `T` | 34 | 0 | **1** | +1 |
| EXL | `R` | 252 | 0 | 0 | |
| ML | all (`E`, `R`) | 75 | 0 | 0 | |
| DL | all (`E`, `R`) | 11 | 0 | 0 | |
| AL | all (`E`, `R`, `T`) | 60 | 0 | 0 | |
| **Total** | | **35,102** | **1** | **96** | −3 to +3 |

No stored date is missing.

**Reading:**

- **Stored dates are reliable:** all but one match PEARS's own formula, so the migration can trust
  `EVENT_DATE`. One `P` rate has a stored date that differs, probably left over from an older
  calculation. It will show up in the comparison report.
- **Copying offsets would move 96 events (0.3%)** by up to 3 days: 94 on `P`, 2 on `EXL` (a `T` and
  an `E`, +1 day each), none on `PEDL` or the other types. The `P` ones include 15 terms, 1 expiry,
  8 bespoke milestones and 61 rates.
- **This supports the plan's recommendation for Open question 6:** LMS offsets and durations are
  worked out from the PEARS **dates** (`Period.between`), not copied, so every event lands on the
  PEARS date and none of these 96 differ. Term and phase durations already follow that rule.

## S15. `RD`/`PA` events (current versions)

Checks what the plan's phase 1 copies as is into `licence_schedule_area_events`:
- subtype;
- `RETENTION_AREA` block (name, short name, type, version);
- `EVENT_ID`;
- code against licence type (`RD` onshore, `PA` offshore).

```sql
WITH ev AS (
  SELECT plm.licence_type
       , e.event_mnem
       , TRIM(e.event_subtype) event_subtype
       , TRIM(e.event_id) event_id
       , TRIM(e.ra_name) ra_name
       , TRIM(e.ra_short_name) ra_short_name
       , TRIM(e.ra_type) ra_type
       , TRIM(e.ra_version) ra_version
       , TRIM(e.event_date) event_date
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[EVENT_MNEM = ("RD", "PA") and not(DUMMY_EVENT = "true")]'
      PASSING plsd.xml_data
      COLUMNS event_mnem VARCHAR2(10) PATH 'EVENT_MNEM', event_subtype VARCHAR2(50) PATH 'EVENT_SUBTYPE'
            , event_id VARCHAR2(20) PATH 'EVENT_ID', event_date VARCHAR2(20) PATH 'EVENT_DATE'
            , ra_name VARCHAR2(200) PATH 'RETENTION_AREA/NAME'
            , ra_short_name VARCHAR2(50) PATH 'RETENTION_AREA/SHORT_NAME'
            , ra_type VARCHAR2(50) PATH 'RETENTION_AREA/TYPE'
            , ra_version VARCHAR2(10) PATH 'RETENTION_AREA/VERSION') e
  WHERE plsd.end_datetime IS NULL
  AND plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
SELECT licence_type AS lic_type
     , event_mnem AS mnem
     , NVL(event_subtype, '(none)') AS subtype_label
     , NVL(ra_type, '(none)') AS area_type_label
     , COUNT(*) AS events
     , SUM(CASE WHEN event_id IS NULL THEN 1 ELSE 0 END) AS no_event_id
     , SUM(CASE WHEN ra_name IS NULL OR ra_short_name IS NULL OR ra_version IS NULL THEN 1 ELSE 0 END) AS incomplete_area_block
     , SUM(CASE WHEN event_date IS NULL THEN 1 ELSE 0 END) AS no_date
     , SUM(CASE WHEN (licence_type = 'P' AND event_mnem = 'RD') OR (licence_type != 'P' AND event_mnem = 'PA') THEN 1 ELSE 0 END) AS wrong_code_for_licence_type
     , COUNT(DISTINCT event_id) AS distinct_event_ids
FROM ev
GROUP BY licence_type, event_mnem, NVL(event_subtype, '(none)'), NVL(ra_type, '(none)')
ORDER BY 1, 2, 3, 4;
```

`distinct_event_ids` lower than `events` means some `EVENT_ID`s repeat within the group.

**Result (live, 27 October 2026; current versions):**

| Type | Code | Subtype | Area type | Events | No `EVENT_ID` | Incomplete area block |
|---|---|---|---|---|---|---|
| P | PA | **(none)** | **(none)** | **51** | **51** | **51** |
| P | PA | APPROVAL | PROSPECTIVE | 15 | 0 | 0 |
| P | PA | **END** | PROSPECTIVE | 7 | 0 | 0 |
| P | PA | EXPIRY | PROSPECTIVE | 13 | 0 | 0 |
| P | PA | WORK_PROGRAMME | PROSPECTIVE | 65 | 0 | 0 |
| PEDL | RD | APPROVAL | DEVELOPMENT | 4 | 0 | 0 |
| PEDL | RD | APPROVAL | RETENTION | 46 | 0 | 0 |
| PEDL | RD | **END** | RETENTION | 15 | 0 | 0 |
| PEDL | RD | EXPIRY | RETENTION | 33 | 0 | 0 |
| PEDL | RD | WORK_PROGRAMME | DEVELOPMENT | 1 | 0 | 0 |
| PEDL | RD | WORK_PROGRAMME | RETENTION | 105 | 0 | 0 |
| PL | RD | APPROVAL | DEVELOPMENT | 13 | 0 | 0 |
| PL | RD | APPROVAL | RETENTION | 7 | 0 | 0 |
| PL | RD | **END** | RETENTION | 3 | 0 | 0 |
| PL | RD | EXPIRY | RETENTION | 4 | 0 | 0 |
| PL | RD | WORK_PROGRAMME | DEVELOPMENT | 5 | 0 | 0 |
| PL | RD | WORK_PROGRAMME | RETENTION | 14 | 0 | 0 |
| ML | RD | APPROVAL | DEVELOPMENT | 3 | 0 | 0 |
| ML | RD | WORK_PROGRAMME | DEVELOPMENT | 1 | 0 | 0 |
| DL | RD | APPROVAL | DEVELOPMENT | 3 | 0 | 0 |
| DL | RD | APPROVAL | RETENTION | 1 | 0 | 0 |
| DL | RD | WORK_PROGRAMME | DEVELOPMENT | 3 | 0 | 0 |
| DL | RD | WORK_PROGRAMME | RETENTION | 1 | 0 | 0 |
| AL | RD | APPROVAL | RETENTION | 1 | 0 | 0 |
| AL | RD | WORK_PROGRAMME | RETENTION | 1 | 0 | 0 |

No event has a missing date or the wrong code for its licence type, and every `EVENT_ID` is unique.

**Reading:**

- **A fourth subtype, `END`** (25 events: 22 `P`/`PEDL`, 3 `PL`), alongside `APPROVAL`, `EXPIRY` and `WORK_PROGRAMME`. It
  is presumably written when an area is ended or removed, and it was not in the code read for the
  plan. The LMS subtype must allow it.
- **51 `P` `PA` events have no subtype, no area block and no `EVENT_ID`.** They look like
  pre-producing area events entered by hand on the schedule screen, before the retention area
  functionality generated them. The LMS table must allow these fields to be empty. The timeline
  caption falls back to the event's comments (or "Pre-producing area"). The write-back reproduces
  them without those elements, and PEARS acknowledgements match an empty `EVENT_ID` to an empty one,
  so they still line up.
- **Area types in use:** `PROSPECTIVE` on `P` licences; `RETENTION` and `DEVELOPMENT` on `PEDL` and
  every other type (all `RD`, as expected onshore).
  `PRODUCING` is never used.
- **Other types (second run): 60 `RD` events, all complete.** Every one has a subtype, a full area
  block, a date and a unique `EVENT_ID`, and none has the wrong code. 46 are on `PL`, 8 on `DL`,
  4 on `ML` and 2 on `AL`. The 12 on `DL` and `ML` sit on schedules with no terms, which aren't
  migrated (pattern E). They stay in PEARS with the rest of those schedules.

## S16. `EVENT_ID` and ambiguous cross-version matches

Input to the plan's "Schedule history" matching rules. Across **all** versions, per event type, it
counts:
- how many events carry an `EVENT_ID` (matching rule 1);
- duplicates **within one version** that make matching ambiguous: same type with the same offset
  (rule 4), and same type with the same comments (rule 5). `T` duplicates by term number and
  `P` duplicates by phase are covered in S8 and S9.

```sql
WITH ev AS (
  SELECT plsd.id plsd_id
       , e.event_mnem
       , TRIM(e.event_id) event_id
       , TRIM(e.comments) comments
       , NVL(TRIM(e.years), '-') || '/' || NVL(TRIM(e.months), '-') || '/' || NVL(TRIM(e.days), '-') offset_key
  FROM pedmgr.ped_licence_schedule_details plsd
  JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
  , XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT[normalize-space(EVENT_MNEM) != "" and not(DUMMY_EVENT = "true")]'
      PASSING plsd.xml_data
      COLUMNS event_mnem VARCHAR2(10) PATH 'EVENT_MNEM', event_id VARCHAR2(20) PATH 'EVENT_ID'
            , comments VARCHAR2(4000) PATH 'COMMENTS'
            , years VARCHAR2(10) PATH 'YEARS', months VARCHAR2(10) PATH 'MONTHS', days VARCHAR2(10) PATH 'DAYS') e
  WHERE plm.licence_type IN ('P', 'PEDL')   -- second run: NOT IN ('P', 'PEDL')
)
, dup_comments AS (
  SELECT event_mnem, COUNT(*) groups, SUM(cnt) events
  FROM (SELECT plsd_id, event_mnem, comments, COUNT(*) cnt
        FROM ev WHERE comments IS NOT NULL
        GROUP BY plsd_id, event_mnem, comments HAVING COUNT(*) > 1)
  GROUP BY event_mnem
)
, dup_offsets AS (
  SELECT event_mnem, COUNT(*) groups, SUM(cnt) events
  FROM (SELECT plsd_id, event_mnem, offset_key, COUNT(*) cnt
        FROM ev WHERE offset_key != '-/-/-'
        GROUP BY plsd_id, event_mnem, offset_key HAVING COUNT(*) > 1)
  GROUP BY event_mnem
)
SELECT a.event_mnem
     , a.events
     , a.with_event_id
     , NVL(dc.groups, 0) same_comment_groups
     , NVL(dc.events, 0) same_comment_events
     , NVL(dof.groups, 0) same_offset_groups
     , NVL(dof.events, 0) same_offset_events
FROM (SELECT event_mnem, COUNT(*) events, SUM(CASE WHEN event_id IS NOT NULL THEN 1 ELSE 0 END) with_event_id
      FROM ev GROUP BY event_mnem) a
LEFT JOIN dup_comments dc ON dc.event_mnem = a.event_mnem
LEFT JOIN dup_offsets dof ON dof.event_mnem = a.event_mnem
ORDER BY a.events DESC;
```

**Result (live, 27 October 2026; all versions; first run `P`/`PEDL`, second run the other types):**

| Event | Events | With `EVENT_ID` | Same-comment groups (events) | Same-offset groups (events) |
|---|---|---|---|---|
| R | 151,004 | 0 | 472 (1,477) | 0 |
| T | 26,730 | 0 | 246 (495) | 0 |
| MR | 10,705 | 0 | 107 (214) | 0 |
| E | 8,732 | 0 | 0 | 0 |
| P | 4,384 | 0 | 148 (299) | 0 |
| RD | 1,634 | **1,634** | 133 (307) | 0 |
| PA | 1,449 | **956** | 76 (169) | 0 |
| DD | 1,004 | 0 | 0 | 0 |
| 2M | 251 | 0 | 0 | 0 |
| BM | 185 | 0 | 0 | 0 |
| *Other types:* | | | | |
| R | 6,397 | 0 | 29 (58) | 0 |
| T | 704 | 0 | 13 (35) | 0 |
| E | 584 | 0 | 0 | 0 |
| RD | 457 | **457** | 10 (20) | 0 |
| MR | 94 | 0 | 0 | 0 |

**Reading:**

- **`EVENT_ID` is only used on `RD`/`PA` events**: every `RD` (on every licence type) and 956 of 1,449 `PA` (the rest
  are the older hand-entered ones, S15). Matching rule 1 only ever helps area events.
- **The same offset never repeats within a version** for any event type, on any licence type. Type + offset identifies an event
  uniquely inside a version.
- **The same comments often repeat**, e.g. several rates sharing a "Direction … rates affected" note,
  or several terms sharing an extension note. Comments are a poor first key.
- **Change made in the plan ("Schedule history"):** match by type + **offset before** type +
  comments. Comments are only used when the offset changed between versions (e.g. a date moved by an
  extension) and the comment is unique within the version.

## S17. Licences with no PEARS schedule

Licences in `ped_licence_master` with no `ped_licence_schedule_details` row at all, by licence type.
For PEARS-managed licence types, the plan says these get no schedule from the migration: a user creates
the schedule in LMS, and the comparison report lists them as "No PEARS schedule".

```sql
SELECT plm.licence_type, COUNT(*) licences_without_schedule
FROM pedmgr.ped_licence_master plm
WHERE plm.licence_type IS NOT NULL
AND NOT EXISTS (
  SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = plm.id
)
GROUP BY plm.licence_type
ORDER BY 1;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | LICENCES WITHOUT SCHEDULE |
|---|---|
| A | 103 |
| AL | 7 |
| B | 13 |
| EXL | 283 |
| ML | 25 |
| NA | 1 |
| **P** | **1,114** |
| **PEDL** | **124** |
| PL | 240 |
| XL | 211 |

**Reading:**

- **1,114 `P` and 124 `PEDL` licences have no schedule.** That's more than the 1,525 `P` and 220
  `PEDL` licences that do have one. It is too many to be only new licences waiting for their first
  schedule.
- They don't affect the migration, which only reads licences with a schedule. An **extant** one
  would need its schedule created by hand in LMS at go-live, so S17b breaks them down by status.
  **None is extant:** they are surrendered or revoked licences, or records with no live data point.
- **The other PEARS-managed types also have licences with no schedule:** 283 `EXL`, 240 `PL`, 25 `ML`,
  7 `AL`, and 103 `A`, 13 `B`, 1 `NA`, 211 `XL` (types that never have a schedule, S2).
  **None of them is extant either** (S17b).

### S17b. Licences with no PEARS schedule, by status

Uses the same "latest live data point" status lookup as S8d (status codes under Conventions).
`(no status)` means the licence has no live data point, e.g. an application that was never awarded.
The licence numbers show the range in each group.

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
  SELECT plm.id ped_lm_id, MAX(xplc.licence_status) licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
  GROUP BY plm.id
)
SELECT plm.licence_type
     , NVL(cs.licence_status, '(no status)') licence_status
     , COUNT(*) licences
     , MIN(TO_NUMBER(plm.licence_no DEFAULT NULL ON CONVERSION ERROR)) lowest_licence_no
     , MAX(TO_NUMBER(plm.licence_no DEFAULT NULL ON CONVERSION ERROR)) highest_licence_no
FROM pedmgr.ped_licence_master plm
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
WHERE plm.licence_type IN ('P', 'PEDL')
AND NOT EXISTS (
  SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = plm.id
)
GROUP BY plm.licence_type, NVL(cs.licence_status, '(no status)')
ORDER BY 1, 3 DESC;
```

Second run, for the other PEARS-managed types. It lists the extant licences instead
of the number range:

```sql
-- Other PEARS-managed types with no PEARS schedule, by status (as S17b, other types)
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
  SELECT plm.id ped_lm_id, MAX(xplc.licence_status) licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
  GROUP BY plm.id
)
SELECT plm.licence_type
     , NVL(cs.licence_status, '(no status)') licence_status
     , COUNT(*) licences
     , LISTAGG(CASE WHEN cs.licence_status = 'E' THEN plm.licence_type || plm.licence_no END, ', ')
         WITHIN GROUP (ORDER BY plm.licence_no) extant_licences
FROM pedmgr.ped_licence_master plm
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
WHERE plm.licence_type NOT IN ('P', 'PEDL')
AND NOT EXISTS (
  SELECT 1 FROM pedmgr.ped_licence_schedule_details plsd WHERE plsd.ped_lm_id = plm.id
)
GROUP BY plm.licence_type, NVL(cs.licence_status, '(no status)')
ORDER BY 1, 3 DESC;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | LICENCE_STATUS | LICENCES | LOWEST NO | HIGHEST NO |
|---|---|---|---|---|
| P | `S` | 1,099 | 2 | 1555 |
| P | (no status) | 10 | 1261 | 1735 |
| P | `R` | 5 | 124 | 1065 |
| PEDL | `S` | 70 | 2 | 230 |
| PEDL | (no status) | 54 | 3 | 151 |
| EXL | (no status) | 281 | | |
| EXL | `S` | 2 | | |
| PL | (no status) | 238 | | |
| PL | `S` | 2 | | |
| ML | (no status) | 25 | | |
| AL | (no status) | 7 | | |
| A | (no status) | 103 | | |
| B | (no status) | 13 | | |
| NA | (no status) | 1 | | |
| XL | (no status) | 211 | | |

(The second run's counts match S17 for every type. Its extant-licence column was empty throughout.)

**Reading:**

- **None is extant, on any licence type.** No licence without a schedule has status `E`, so no
  existing licence without a schedule needs one created by hand in LMS at go-live. Only licences
  raised after go-live will. (The 40 schedules with no terms are a separate manual step, L9.)
- **1,173 have status `S` (Surrendered):** 1,099 `P`, 70 `PEDL`, 2 `EXL`, 2 `PL`. They are mostly
  older licences (`P` up to 1555, `PEDL` up to 230) that were surrendered before PEARS held
  schedules.
- **5 `P` have status `R` (Revoked)** (numbers 124–1065).
- **Everything else has no status** (no live data point): 64 `P`/`PEDL`, and almost all of the
  other types (281 `EXL`, 238 `PL`, 25 `ML`, 7 `AL`, and every `A`, `B`, `NA` and `XL`
  licence). These are probably records that never became licences, e.g. applications that weren't
  awarded, or older licences never loaded as PEARS positions.
- The comparison report's "No PEARS schedule" case only needs to cover licences raised after
  go-live.

## S18. Licences with a schedule, by status

What the migration will carry, by current licence status (codes under Conventions), with each
group's schedule versions. It was run twice. The first run used `IN ('P', 'PEDL')`. The second used
`NOT IN ('P', 'PEDL')` and added a `licence_list` column
(`LISTAGG(plm.licence_type || plm.licence_no, ', ') WITHIN GROUP (ORDER BY plm.licence_no)`).
Both results are combined below.

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
  SELECT plm.id ped_lm_id, MAX(xplc.licence_status) licence_status
  FROM pedmgr.ped_licence_master plm
  JOIN latest_dp ld ON ld.licence_type = plm.licence_type AND ld.licence_no = plm.licence_no
  JOIN pedmgr.ped_licences pl ON pl.ped_dp_id = ld.ped_dp_id AND pl.end_datetime IS NULL
  JOIN pedmgr.xview_ped_ld_current xplc ON xplc.ped_licence_id = pl.id AND xplc.ped_lm_id = plm.id
  GROUP BY plm.id
)
, versions AS (
  SELECT ped_lm_id, COUNT(*) schedule_versions
  FROM pedmgr.ped_licence_schedule_details
  GROUP BY ped_lm_id
)
SELECT plm.licence_type
     , NVL(cs.licence_status, '(no status)') licence_status
     , COUNT(*) licences
     , SUM(v.schedule_versions) schedule_versions
FROM pedmgr.ped_licence_master plm
JOIN versions v ON v.ped_lm_id = plm.id
LEFT JOIN current_status cs ON cs.ped_lm_id = plm.id
WHERE plm.licence_type IN ('P', 'PEDL')
GROUP BY plm.licence_type, NVL(cs.licence_status, '(no status)')
ORDER BY 1, 3 DESC;
```

**Result (live, 27 October 2026):**

| LICENCE_TYPE | LMS TYPE | STATUS | LICENCES | SCHEDULE VERSIONS | LICENCE LIST (other types) |
|---|---|---|---|---|---|
| P | `SEAWARD_PRODUCTION` | `S` Surrendered | 946 | 3,625 | |
| P | | `E` Extant | 345 | 3,666 | |
| P | | `X` Expired | 211 | 926 | |
| P | | (no status) | 20 | 87 | |
| P | | `R` Revoked | 3 | 8 | |
| PEDL | `LANDWARD_PRODUCTION` | `S` Surrendered | 124 | 915 | |
| PEDL | | `X` Expired | 42 | 434 | |
| PEDL | | `E` Extant | 36 | 472 | |
| PEDL | | `R` Revoked | 13 | 79 | |
| PEDL | | (no status) | 5 | 27 | |
| PL | `LANDWARD_PRODUCTION` | `E` Extant | 21 | 285 | PL81, PL89, PL90, PL116, PL161, PL162, PL178, PL179, PL182, PL199, PL205, PL211, PL213, PL215, PL220, PL233, PL235, PL240, PL241, PL249, PL259 |
| PL | | `X` Expired | 3 | 29 | PL55, PL77, PL80 |
| PL | | `S` Surrendered | 2 | 22 | PL79, PL234 |
| EXL | `LANDWARD_PRODUCTION` | `S` Surrendered | 6 | 34 | EXL189, EXL273, EXL276, EXL284, EXL288, EXL290 |
| EXL | | `E` Extant | 5 | 53 | EXL141, EXL250, EXL253, EXL269, EXL294 |
| EXL | | `R` Revoked | 1 | 4 | EXL203 |
| ML | `LANDWARD_PRODUCTION` | `E` Extant | 7 | 80 | ML3, ML4, ML5, ML6, ML7, ML18, ML21 |
| DL | `LANDWARD_PRODUCTION` | `E` Extant | 4 | 38 | DL1, DL2, DL3, DL4 |
| DL | | `X` Expired | 1 | 15 | DL5 |
| AL | `LANDWARD_PRODUCTION` | `E` Extant | 1 | 8 | AL10 |
| AL | | `S` Surrendered | 1 | 14 | AL6 |
| AL | | `X` Expired | 1 | 8 | AL9 |
| CE | `LANDWARD_PRODUCTION` | `E` Extant | 1 | 5 | CE1 |

(Totals reconcile with S2 for every type: `P` 1,525/8,312, `PEDL` 220/1,927, `PL` 26/336, `EXL`
12/91, `ML` 7/80, `DL` 5/53, `AL` 3/30, `CE` 1/5.)

**Reading:**

- **420 of the 1,799 typed licences are extant (23%)**: 381 `P`/`PEDL` and 39 of the other types.
  They hold 4,607 of the 10,834 schedule versions (43%), because live licences are edited more
  often.
- **The other types are mostly live:** 39 of 54 are extant, against 22% of `P` and `PEDL`. All 7
  `ML` licences are extant, and 21 of the 26 `PL`.
- **6 of the 9 `AL`, `DL` and `CE` licences are extant** (AL10, CE1, DL1–DL4). The `DL` and `CE`
  schedules have no terms, so they are not migrated (pattern E). Since 28 October 2026 LMS maps all
  three types to `LANDWARD_PRODUCTION`, so the 3 `AL` licences (one extant, AL10) migrate with
  their terms, and the licensing team can create the `DL` and `CE` schedules in LMS (plan, Open
  question 14, resolved).
- **1,354 have ended:** 1,079 surrendered, 258 expired and 17 revoked. Between them they hold 6,113
  versions.
- **25 have a schedule but no status** (20 `P`, 5 `PEDL`), meaning no live data point. Unlike the
  no-status licences without a schedule (S17b), these did get as far as having a schedule. Worth
  listing for the licensing team.
- **What this means for the plan:**
  - The design migrates all of them, so LMS has the full history of ended licences too.
  - The ongoing parts, though (schedule edits in LMS, the `RD`/`PA` round trip and the write-back),
    only realistically apply to the 420 extant licences.
  - The PEARS fixes and rule-based skips matter most where they hit extant licences. The pattern
    lists (S8b) don't show status yet. For the other types, S18 and S8b together show which
    licences are which, e.g. 17 of the 18 schedules with no terms are extant (L9).

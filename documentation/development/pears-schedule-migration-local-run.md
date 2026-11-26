# Running the PEARS schedule migration locally

The migration copies every licence schedule in PEARS, with its version history, into LMS. Locally it
runs against **dev PEARS**, writing to your local LMS database. The design, decisions and test
results are in the
[migration plan](../investigations/pears-migration/pears-schedule-migration-plan.md), sections 4 to 7.

## You'll need

- LMS running locally as usual (see the README, "Development setup").
- Credentials for dev PEARS (`db-ogadev1`). The connection string is in `.drone.yml`, in the
  `pears-migration-validation` step. Ask the team for the password, and don't commit it.

## 1. Set up LMS

1. In your IntelliJ run configuration, add `migration` to the active profiles, next to `development`.
2. Add these environment variables:
   - `PEARS_DATASOURCE_URL`, `PEARS_DATASOURCE_USERNAME` and `PEARS_DATASOURCE_PASSWORD`, for dev PEARS.
   - Optionally, `PEARS_SCHEDULE_MIGRATION_CLEARDOWN_ENABLED=true`, so you can clear down and run it
     again.
3. Run these scripts from `src/main/resources/manualscripts/pearsschedulemigration/` against your
   local LMS database, in order. They aren't Flyway migrations, so you only run them once.
   1. `0001_pears_schedule_migration_tables.sql`
   2. `0002_pears_schedule_migration_runs.sql`
   3. `../pearsworkprogrammemigration/0002_pears_work_programme_migration_runs.sql`, which adds the
      column runs of either migration need
4. Start LMS and sign in. The endpoints below use your normal login.

All the URLs below are under `http://localhost:8080/lms`.

## 2. Dry run (optional, changes nothing)

- Start: `/migration/pears-schedules/validate`. Add `?licenceReference=P1556` for one licence. It
  returns a run ID.
- Each licence is migrated and compared with PEARS, then rolled back. Follow it and get its results
  as for a migration (step 3).

## 3. Migrate

- Start: `/migration/pears-schedules`. Add `?licenceReference=…` for one licence. It returns a run ID.
- Follow it: `/migration/pears-schedules/{runId}`.
- Every licence of the run as a CSV: `/migration/pears-schedules/{runId}/report`.
- Retry the failed licences, from the browser console on any LMS page:

  ```js
  fetch('/lms/migration/pears-schedules/{runId}/retry-failed', { method: 'POST' })
    .then(r => r.json()).then(console.log)
  ```

- Cancel a run the same way, with `/lms/migration/pears-schedules/{runId}/cancel`.
- If LMS restarts during a run, the run isn't picked up again by itself. Resume it the same way,
  with `/lms/migration/pears-schedules/{runId}/resume`, or cancel it.

Only one run can be active at a time. It works through the licences in batches of 10 in the
background. The background worker doesn't touch the run tables from start-up: it only works
once a run is started, its failed licences are retried, or it is resumed.

## 4. Compare with PEARS

`/migration/pears-schedules/compare` reads a licence's schedule from PEARS and from LMS and lists them
side by side, event by event. It's read-only, so you can run it at any time.

| Parameter | Values | Default |
|---|---|---|
| `licences` (required) | Licence references separated by commas, such as `P1556,PEDL236`, or `all` for every licence the migration wrote a schedule for | |
| `versions` | `all`: every version, oldest first, each PEARS version against the LMS version migrated from it | PEARS's current version against LMS's active version |
| `issues` | `only`: just the licences with an issue to look into (red rows) | Every licence given |
| `notMigrated` | `only`: just the licences with an event not migrated (yellow rows). With `issues=only` too, the licences with either | Every licence given |
| `format` | `csv`: download the rows as `pears-schedule-comparison.csv` | A page |

The parameters combine. For example:

- After a run, list the migrated licences with an issue. It should list none:
  `/migration/pears-schedules/compare?licences=all&issues=only`.
- List the migrated licences with events the migration left out, such as rates after the last term:
  `/migration/pears-schedules/compare?licences=all&notMigrated=only`.
- Look at one or two licences in full: `/migration/pears-schedules/compare?licences=P1556,PL211`.
- Look at a licence's whole history: `/migration/pears-schedules/compare?licences=P1556&versions=all`.
- Download every migrated licence for a spreadsheet:
  `/migration/pears-schedules/compare?licences=all&format=csv`.

An unknown option or licence reference gives a 400 that says which.

**Reading the page.** Each licence lists its PEARS events and the LMS events they became, with a
result for each row:

| Result | Issue? | Means |
|---|---|---|
| Match | | Same event, date and value |
| Dated from its offset | | PEARS's stored date is out of date (often a 1900s date); LMS's date, worked out from the offset, is the one compared |
| Carried on | | A further LMS rate made from one PEARS yearly rate, where it carries on into a later term or phase |
| No start date in PEARS | | The version has no start date in PEARS, so LMS's comes from another version |
| No expiry in PEARS | | PEARS has no expiry event, so LMS has none either |
| Not migrated | | An event the migration leaves out by its rules, or every event of a licence it doesn't migrate; the row says why |
| Differs | Yes | The dates differ, by the number of days shown |
| Value differs | Yes | Same event and date, different value (rate or percentage) |
| Original event differs | Yes | LMS links the event to a different earlier version's event than the migration would |
| Missing in LMS | Yes | A PEARS event with no LMS event |
| LMS only | Yes | An LMS event with no PEARS event: added in LMS, or from another PEARS version |

Rows with an issue are red, and "Not migrated" rows are yellow. A licence the migration doesn't
migrate says why at the top, such as "No terms in PEARS".

## 5. Run it again

With cleardown enabled, start a cleardown from the browser console:

```js
fetch('/lms/migration/pears-schedules/cleardown', { method: 'POST' })
  .then(r => r.json()).then(console.log)
```

It deletes only the schedules the migration created, and leaves schedules made in LMS alone. Follow
it like any run, then migrate again. It clears any work programme migrated onto them too (step 6).

## 6. Migrate work programmes

The work programme migration puts each licence's current PEARS work programme onto the schedule the
schedule migration wrote for it, so migrate schedules first. Its design is in the
[work programme migration plan](../investigations/pears-migration/pears-work-programme-migration-plan.md).

1. Run `src/main/resources/manualscripts/pearsworkprogrammemigration/0001_pears_work_programme_migration_tables.sql`
   and then `0003_pears_work_programme_schedule_comment_links_cascade.sql` from the same folder against
   your local LMS database, after the schedule migration's scripts (step 1 ran its `0002`). `0003` lets
   a migrated schedule comment be removed in LMS; run it on its own if you ran `0001` before it existed.
2. Optionally, dry run first: `/migration/pears-work-programmes/validate`, with `?licenceReference=…`
   for one licence. Each licence's work programme is migrated onto its migrated schedule and compared
   with PEARS, then rolled back, so nothing changes. Follow it as for a migration (next step).
3. Start: `/migration/pears-work-programmes`. Add `?licenceReference=…` for one licence. It returns a
   run ID. A licence with no migrated schedule is skipped and says so.
4. Follow it, get its CSV, retry its failed licences, resume it and cancel it as for schedules (step 3), under
   `/migration/pears-work-programmes/{runId}`.
5. Compare with PEARS: `/migration/pears-work-programmes/compare`, with the same `licences`, `issues`,
   `notMigrated` and `format` parameters as the schedule comparison (step 4), but no `versions`: it
   compares the current PEARS work programmes with the `ACTIVE` schedule. For example, after a run,
   `/migration/pears-work-programmes/compare?licences=all&issues=only` should list none.

   Each licence lists its PEARS activities, each with the LMS activity it became, then the schedule
   comments:

   | Result | Issue? | Means |
   |---|---|---|
   | Match | | As the migration makes it |
   | Mapped | | As the migration makes it, through a mapping that changes the PEARS status, such as `NOT_STARTED` to Open |
   | Not migrated | | An activity the migration leaves out by its rules, such as "None", or every activity of a licence it doesn't migrate; the row says why |
   | Differs | Yes | The LMS activity isn't what the migration makes of the PEARS one; the row names what differs |
   | Missing in LMS | Yes | A PEARS activity or general note with nothing in LMS |
   | LMS only | Yes | An LMS activity, or a schedule comment the migration wrote, with no PEARS counterpart |
   | Added in LMS | | A schedule comment someone added in LMS |

Work programme runs share the schedule migration's queue, so a work programme run can't start while a
schedule run is active, and the other way round.

To run it again, clear the work programmes down with the same cleardown switch
(`PEARS_SCHEDULE_MIGRATION_CLEARDOWN_ENABLED=true`):

```js
fetch('/lms/migration/pears-work-programmes/cleardown', { method: 'POST' })
  .then(r => r.json()).then(console.log)
```

It deletes only what the work programme migration wrote, with its audit history: the activities,
their statuses, comments and reminders, and its schedule comments. The schedules stay, with any
schedule comments added in LMS, ready to migrate the work programmes onto again.

## What to expect against dev PEARS

About 1,109 licences, taking about 13 minutes:

- **About 892 migrated**, all matching PEARS.
- **About 114 failed**, as designed:
  - no terms in PEARS (`ML`, `DL`, CE1 and some `P` and `PL`);
  - no schedule start date (`PEDL`, dev data only);
  - P1200, P1950 and P1929 (event types not in the catalogue, dev data only);
  - P1948 (a mistyped term, dev data only).
- **About 103 skipped** as "Not a licence in LMS": draft licences, and a few with no data point in
  PEARS's live series.

A licence that already has a schedule in your LMS is skipped by the migration, and compared as it is
by the dry run.

For work programmes, after the schedule migration: about 2,204 licences, taking about 20 minutes.

- **About 879 migrated**, all matching PEARS: the comparison with `issues=only` lists none.
- **About 1,325 skipped**, as designed:
  - about 1,137 "Has no schedule from the schedule migration": licences with no PEARS schedule
    (mostly surrendered), and those whose schedule failed above;
  - about 182 "Not a licence in LMS";
  - 6 with two Initial work programmes (EXL253, PEDL120, PEDL133, PEDL180, PEDL181, PEDL225), until
    the licensing team chooses one.
- **None failed.**

## Alternative: the Gradle sweep

This migrates and compares every licence in a test database, so it doesn't need a running LMS. It also
covers licences that aren't in your LMS.

- In IntelliJ, create a **Gradle** run configuration, not the green arrow beside the test (that runs
  the `test` task, which leaves this test out):

  ```
  pearsMigrationTest --tests '*PearsScheduleMigrationValidationTest'
  ```

- Give it the same three `PEARS_DATASOURCE_*` variables.
- To narrow it, set `PEARS_MIGRATION_LICENCES=P1556,PL211`.
- The report is written to `build/reports/pears-migration/schedule-comparison.txt`.
- For work programmes, run `pearsMigrationTest --tests '*PearsWorkProgrammeMigrationValidationTest'`.
  It migrates each licence's schedule and then its work programme, compares the work programme, and
  writes `build/reports/pears-migration/work-programme-comparison.txt`.

## Good to know

- **Reminders:** migrated schedules feed the reminder job (daily at 07:00) when the LMS1 release phase
  is on. Emails aren't sent locally.
- **Other environments:** the same endpoints work in any environment running with the `migration`
  profile and the PEARS datasource, under that environment's LMS URL. The two scripts have to be run
  there first.

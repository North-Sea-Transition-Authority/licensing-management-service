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
3. Run the two scripts in `src/main/resources/manualscripts/pearsschedulemigration/` against your
   local LMS database, in order. They aren't Flyway migrations, so you only run them once.
   1. `0001_pears_schedule_migration_tables.sql`
   2. `0002_pears_schedule_migration_runs.sql`
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

Only one run can be active at a time. It works through the licences in batches of 10 in the
background.

## 4. Check the result

- Migrated licences with issues, which should be none: `/migration/pears-schedules/compare?licences=all&issues=only`.
- Add `&format=csv` for a CSV, or `&versions=all` to compare every version, not just the current one.
- Compare particular licences: `/migration/pears-schedules/compare?licences=P1556,PL211`.

## 5. Run it again

With cleardown enabled, start a cleardown from the browser console:

```js
fetch('/lms/migration/pears-schedules/cleardown', { method: 'POST' })
  .then(r => r.json()).then(console.log)
```

It deletes only the schedules the migration created, and leaves schedules made in LMS alone. Follow
it like any run, then migrate again.

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

## Good to know

- **Reminders:** migrated schedules feed the reminder job (daily at 07:00) when the LMS1 release phase
  is on. Emails aren't sent locally.
- **Other environments:** the same endpoints work in any environment running with the `migration`
  profile and the PEARS datasource, under that environment's LMS URL. The two scripts have to be run
  there first.

package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

public enum PearsScheduleMigrationQueueStatus {
  PENDING,
  MIGRATED,
  CLEARED,
  /**
   * Dry run: LMS would hold what PEARS holds.
   */
  MATCHED,
  /**
   * Dry run: the comparison found issues to look into.
   */
  DIFFERS,
  SKIPPED,
  FAILED
}

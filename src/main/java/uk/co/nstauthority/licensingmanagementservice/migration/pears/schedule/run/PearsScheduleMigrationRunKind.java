package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

public enum PearsScheduleMigrationRunKind {
  MIGRATE,
  CLEARDOWN,
  /**
   * A dry run: each licence is migrated and compared with PEARS, then rolled back.
   */
  VALIDATE
}

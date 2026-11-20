package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

public enum PearsScheduleMigrationRunStatus {
  QUEUED,
  RUNNING,
  COMPLETE,
  CANCELLED;

  boolean isActive() {
    return this == QUEUED || this == RUNNING;
  }
}

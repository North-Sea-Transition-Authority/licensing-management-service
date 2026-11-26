package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

/**
 * The LMS status a PEARS activity status becomes: the {@code WorkProgrammeStatus} of the same name, which the write
 * step sets. It leaves out {@code TRANSFERRED}, which no PEARS status becomes.
 */
public enum MigratedStatus {
  OPEN,
  IN_PROGRESS,
  COMPLETE,
  FULL_WAIVER,
  DROPPED
}

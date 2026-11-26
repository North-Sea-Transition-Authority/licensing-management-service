package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * What happened to one licence's work programme.
 *
 * @param reason why it was skipped or failed; null when it was migrated
 * @param notes  what the mapper decided along the way, counted by reason; empty when it wasn't mapped
 */
public record PearsWorkProgrammeMigrationOutcome(
    PearsLicenceReference licence,
    Result result,
    @Nullable String reason,
    MigrationNotes notes
) {

  public enum Result {
    MIGRATED,
    SKIPPED,
    FAILED
  }

  static PearsWorkProgrammeMigrationOutcome migrated(PearsLicenceReference licence, MigrationNotes notes) {
    return new PearsWorkProgrammeMigrationOutcome(licence, Result.MIGRATED, null, notes);
  }

  static PearsWorkProgrammeMigrationOutcome skipped(
      PearsLicenceReference licence,
      String reason,
      MigrationNotes notes
  ) {
    return new PearsWorkProgrammeMigrationOutcome(licence, Result.SKIPPED, reason, notes);
  }

  static PearsWorkProgrammeMigrationOutcome failed(PearsLicenceReference licence, String reason, MigrationNotes notes) {
    return new PearsWorkProgrammeMigrationOutcome(licence, Result.FAILED, reason, notes);
  }
}

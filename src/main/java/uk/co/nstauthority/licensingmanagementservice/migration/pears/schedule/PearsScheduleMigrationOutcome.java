package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * What happened to one licence's schedule.
 *
 * @param reason why it was skipped or failed; null when it was migrated
 * @param notes  what the mapper decided along the way, counted by reason; empty when it wasn't mapped
 */
public record PearsScheduleMigrationOutcome(
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

  static PearsScheduleMigrationOutcome migrated(PearsLicenceReference licence, MigrationNotes notes) {
    return new PearsScheduleMigrationOutcome(licence, Result.MIGRATED, null, notes);
  }

  static PearsScheduleMigrationOutcome skipped(PearsLicenceReference licence, String reason, MigrationNotes notes) {
    return new PearsScheduleMigrationOutcome(licence, Result.SKIPPED, reason, notes);
  }

  static PearsScheduleMigrationOutcome failed(PearsLicenceReference licence, String reason, MigrationNotes notes) {
    return new PearsScheduleMigrationOutcome(licence, Result.FAILED, reason, notes);
  }
}

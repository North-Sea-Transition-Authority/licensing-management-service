package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;

/**
 * What happened to one licence's migrated schedule in a cleardown.
 *
 * @param reason why it was skipped or put off; null when it was cleared
 */
public record PearsScheduleCleardownOutcome(Result result, @Nullable String reason) {

  public enum Result {
    CLEARED,
    SKIPPED,
    /**
     * Not cleared yet, and to be tried again: nothing was changed.
     */
    POSTPONED
  }

  static PearsScheduleCleardownOutcome cleared() {
    return new PearsScheduleCleardownOutcome(Result.CLEARED, null);
  }

  static PearsScheduleCleardownOutcome skipped(String reason) {
    return new PearsScheduleCleardownOutcome(Result.SKIPPED, reason);
  }

  static PearsScheduleCleardownOutcome postponed(String reason) {
    return new PearsScheduleCleardownOutcome(Result.POSTPONED, reason);
  }
}

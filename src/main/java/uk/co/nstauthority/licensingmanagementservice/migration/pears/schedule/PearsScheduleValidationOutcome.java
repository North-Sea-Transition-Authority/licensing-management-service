package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * What a dry run made of one licence.
 *
 * @param reason why it was skipped or failed, or the issues found, a line each; null when it matched
 * @param notes  the mapper's notes, and how many comparison rows had each result, counted by reason
 */
public record PearsScheduleValidationOutcome(Result result, @Nullable String reason, Map<String, Integer> notes) {

  public PearsScheduleValidationOutcome {
    notes = Map.copyOf(notes);
  }

  public enum Result {
    /**
     * LMS holds what PEARS holds, in every version.
     */
    MATCHED,
    /**
     * The comparison found issues to look into.
     */
    DIFFERS,
    SKIPPED,
    FAILED
  }
}

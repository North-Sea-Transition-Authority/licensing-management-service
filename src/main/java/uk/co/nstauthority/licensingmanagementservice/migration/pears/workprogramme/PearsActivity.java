package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * One activity of a work programme's current version, as PEARS holds it.
 *
 * @param position    the activity's position in the programme, counting from 1, across the flat and phase lists
 * @param phaseType   the enclosing phase, for example {@code Phase A}; null on the flat list
 * @param statusSince when the activity first had its current status: the start of the earliest version of its
 *                    programme that holds it with that status
 */
public record PearsActivity(
    int position,
    ListKind listKind,
    @Nullable String phaseType,
    @Nullable String activityId,
    @Nullable String activityType,
    @Nullable String title,
    @Nullable String status,
    @Nullable String competedBlocks,
    @Nullable String description,
    @Nullable LocalDateTime statusSince
) {

  /**
   * Which list holds the activity: the flat list of "general organisation" rounds, or a phase's list.
   */
  public enum ListKind {
    FLAT,
    PHASE
  }
}

package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;

/**
 * A phase of the initial term of a mapped schedule version, from a PEARS {@code P} event.
 *
 * @param eventKey  see {@link MappedTerm#eventKey}
 * @param phaseType the phase the {@code P} event's {@code PHASE} names, the one ending on its date
 * @param startDate the day the phase starts: the initial term's start for the first phase present, otherwise the
 *                  previous phase's boundary
 * @param endDate   the day before the {@code P} event's date, as LMS works it out from the duration
 * @param duration  the LMS phase duration that ends the phase on {@code endDate}
 * @param comment   the {@code P} event's comments, or null when it has none
 * @param source    the {@code P} event
 */
public record MappedPhase(
    int eventKey,
    PhaseType phaseType,
    LocalDate startDate,
    LocalDate endDate,
    ThreeFieldDuration duration,
    @Nullable MappedComment comment,
    PearsScheduleEvent source
) {

  /**
   * The date PEARS gives the phase's end: the day after its LMS end date, where the next phase starts.
   */
  public LocalDate boundary() {
    return endDate.plusDays(1);
  }
}

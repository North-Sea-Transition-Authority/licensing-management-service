package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

/**
 * A term of a mapped schedule version, from a PEARS {@code T} event.
 *
 * @param eventKey  the same for the same event in every version of the licence, and unique to it otherwise. The write
 *                  step gives events sharing a key one {@code originalEventId}, which LMS keeps comments and the event
 *                  tracker on
 * @param termNo    PEARS's term number, 1 to 4. The write step turns it into a {@code TermType}: 4 is the Fourth term,
 *                  which only the four-term licences have
 * @param startDate the day the term starts: the schedule start date for the first term present, otherwise the previous
 *                  term's boundary
 * @param endDate   the day before the {@code T} event's date, as LMS works it out from the duration
 * @param duration  the LMS term duration that ends the term on {@code endDate}
 * @param comment   the {@code T} event's comments, or null when it has none
 * @param source    the {@code T} event
 */
public record MappedTerm(
    int eventKey,
    int termNo,
    LocalDate startDate,
    LocalDate endDate,
    ThreeFieldDuration duration,
    @Nullable MappedComment comment,
    PearsScheduleEvent source
) {

  /**
   * The date PEARS gives the term's end: the day after its LMS end date, where the next term starts.
   */
  public LocalDate boundary() {
    return endDate.plusDays(1);
  }
}

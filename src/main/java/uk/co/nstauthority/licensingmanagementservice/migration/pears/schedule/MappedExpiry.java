package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;

/**
 * The expiry of a mapped schedule version, from its PEARS {@code E} event.
 *
 * @param eventKey  see {@link MappedTerm#eventKey}
 * @param pearsDate the date PEARS gives the expiry: the anniversary the licence ends on, normally the final term's
 *                  boundary. The write step stores the day before, where LMS ends the final term (the plan's open
 *                  question 8, decided for now)
 * @param comments  the event's comments, trimmed; null when it has none. LMS holds them on the expiry itself, so they
 *                  don't become event comments
 * @param source    the {@code E} event
 */
public record MappedExpiry(
    int eventKey,
    LocalDate pearsDate,
    @Nullable String comments,
    PearsScheduleEvent source
) {
}

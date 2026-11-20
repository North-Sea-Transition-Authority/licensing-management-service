package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One saved version of a licence's schedule: a row of {@code PED_LICENCE_SCHEDULE_DETAILS}. PEARS saves the whole
 * schedule each time, so a version is complete rather than a change to the one before.
 *
 * @param plsdId     the version's {@code PED_LICENCE_SCHEDULE_DETAILS.ID}
 * @param start      when the version was saved
 * @param end        when it was replaced; null for the current version
 * @param originDate the schedule's start date ({@code ORIGIN_DATE}); null when the version has none, or one that is not
 *                   a date
 * @param events     the version's events in the order it holds them, without the schedule screen's padding rows
 */
public record PearsScheduleVersion(
    long plsdId,
    LocalDateTime start,
    @Nullable LocalDateTime end,
    @Nullable LocalDate originDate,
    List<PearsScheduleEvent> events
) {

  public PearsScheduleVersion {
    events = List.copyOf(events);
  }

  public boolean isCurrent() {
    return end == null;
  }
}

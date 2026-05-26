package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;

public record TimelinePhaseView(
    List<ScheduleEvent> events,
    List<ScheduleEvent> endOfPhaseEvents,
    PhaseType phaseType,
    LocalDate startDate,
    String dateDurationString,
    String endDateString,
    String updateUrl,
    String deleteUrl,
    String addCommentUrl
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.PHASE;
  }

  @Override
  public LocalDate getSortingDate() {
    return startDate;
  }
}

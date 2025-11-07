package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;

public record TimelinePhaseView(
    List<ScheduleEvent> events,
    PhaseType phaseType,
    String dateDurationString,
    String endDateString,
    String updateUrl,
    String deleteUrl
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.PHASE;
  }
}

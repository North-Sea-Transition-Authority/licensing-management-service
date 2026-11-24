package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;
import java.util.Optional;

public interface ScheduleEvent {

  ScheduleEventType getEventType();

  LocalDate getSortingDate();

  default Optional<ScheduleTimelineFilterOption> getFilterOption() {
    return ScheduleTimelineFilterOption.forEventType(getEventType());
  }
}

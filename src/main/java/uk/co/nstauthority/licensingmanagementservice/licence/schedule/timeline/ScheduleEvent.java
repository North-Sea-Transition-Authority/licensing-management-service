package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;

public interface ScheduleEvent {

  ScheduleEventType getEventType();

  LocalDate getSortingDate();
}

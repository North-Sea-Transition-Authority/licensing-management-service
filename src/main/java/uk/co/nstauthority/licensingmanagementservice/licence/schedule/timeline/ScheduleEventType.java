package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

public enum ScheduleEventType {
  PHASE(999),
  RATE(1),
  WORK_PROGRAMME_ACTIVITY(2),
  OTHER(3);

  private final Integer eventTypeOrder;

  ScheduleEventType(Integer eventTypeOrder) {
    this.eventTypeOrder = eventTypeOrder;
  }

  public Integer getEventTypeOrder() {
    return eventTypeOrder;
  }
}

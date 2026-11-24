package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ScheduleEventType implements Displayable {
  TERM("Terms", 999),
  PHASE("Phases", 999),
  RATE("Rates", 1),
  WORK_PROGRAMME_ACTIVITY("Work programme activities", 2),
  OTHER("Other schedule events", 3),
  AREA("Retention, development and pre-producing areas", 4),
  EXPIRY("Expiry", 999);

  private final String displayName;
  private final Integer eventTypeOrder;

  ScheduleEventType(
      String displayName,
      Integer eventTypeOrder
  ) {
    this.displayName = displayName;
    this.eventTypeOrder = eventTypeOrder;
  }

  public Integer getEventTypeOrder() {
    return eventTypeOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return eventTypeOrder;
  }
}

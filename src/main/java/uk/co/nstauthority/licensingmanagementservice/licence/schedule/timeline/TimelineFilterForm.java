package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class TimelineFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 8739049260187569611L;

  private List<String> eventTypes = List.of();

  public List<String> getEventTypes() {
    return eventTypes;
  }

  public void setEventTypes(List<String> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public void clearFilter() {
    this.setEventTypes(ScheduleEventType.getFilterDefaults());
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.web.bind.annotation.SessionAttributes;

@SessionAttributes("scheduleTimelineSession")
public class ScheduleTimelineSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 6276611143143850105L;

  private ScheduleTimelineFilterForm scheduleTimelineFilterForm;
  private boolean filterInvoked;

  public ScheduleTimelineSession(ScheduleTimelineFilterForm scheduleTimelineFilterForm) {
    this.scheduleTimelineFilterForm = scheduleTimelineFilterForm;
    this.filterInvoked = false;
  }

  public boolean hasFilterBeenInvoked() {
    return filterInvoked;
  }

  public void update(ScheduleTimelineFilterForm scheduleTimelineFilterForm) {
    this.scheduleTimelineFilterForm = scheduleTimelineFilterForm;
    this.filterInvoked = true;
  }

  public ScheduleTimelineFilterForm getTimelineFilterForm() {
    return scheduleTimelineFilterForm;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.web.bind.annotation.SessionAttributes;

@SessionAttributes("timelineSession")
public class TimelineSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 6276611143143850105L;

  private TimelineFilterForm timelineFilterForm;
  private boolean filterInvoked;

  public TimelineSession(TimelineFilterForm timelineFilterForm) {
    this.timelineFilterForm = timelineFilterForm;
    this.filterInvoked = false;
  }

  public boolean hasFilterBeenInvoked() {
    return filterInvoked;
  }

  public void update(TimelineFilterForm timelineFilterForm) {
    this.timelineFilterForm = timelineFilterForm;
    this.filterInvoked = true;
  }

  public TimelineFilterForm getTimelineFilterForm() {
    return timelineFilterForm;
  }
}

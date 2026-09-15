package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import java.io.Serial;
import java.io.Serializable;

public class EventTrackerFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 197821687048806958L;

  private EventTrackerForm filterForm;

  public EventTrackerFilterSession(EventTrackerForm filterForm) {
    this.filterForm = filterForm;
  }

  public void update(EventTrackerForm filterForm) {
    this.filterForm = filterForm;
  }

  public EventTrackerForm getFilterForm() {
    return filterForm;
  }
}

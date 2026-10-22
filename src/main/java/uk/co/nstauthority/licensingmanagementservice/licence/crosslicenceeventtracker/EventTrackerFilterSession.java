package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import java.io.Serial;
import java.io.Serializable;

public class EventTrackerFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 197821687048806958L;

  private EventTrackerForm filterForm;
  private boolean defaultFiltersApplied;

  public EventTrackerFilterSession(EventTrackerForm filterForm) {
    this(filterForm, false);
  }

  public EventTrackerFilterSession(EventTrackerForm filterForm, boolean defaultFiltersApplied) {
    this.filterForm = filterForm;
    this.defaultFiltersApplied = defaultFiltersApplied;
  }

  public void update(EventTrackerForm filterForm) {
    this.filterForm = filterForm;
    this.defaultFiltersApplied = false;
  }

  public EventTrackerForm getFilterForm() {
    return filterForm;
  }

  public boolean isDefaultFiltersApplied() {
    return defaultFiltersApplied;
  }
}

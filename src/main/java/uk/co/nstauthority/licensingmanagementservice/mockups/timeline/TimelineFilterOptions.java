package uk.co.nstauthority.licensingmanagementservice.mockups.timeline;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum TimelineFilterOptions implements Displayable {
  RATES("Rates", 10),
  SCHEDULE_EVENTS("Schedule events", 20),
  WORK_PROGRAMME_EVENTS("Work programme elements", 30);

  private final String displayName;
  private final int displayOrder;

  TimelineFilterOptions(String displayName, int displayOrder) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  public static Map<String, String> getFilterOptions() {
    Instant.now().plus(1, ChronoUnit.DAYS);
    return DisplayableEnumOptionUtil.getDisplayableOptions(TimelineFilterOptions.class);
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getEnumName() {
    return this.name();
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum ScheduleTimelineFilterOption implements Displayable {
  RATE("Rates", 1),
  WORK_PROGRAMME_ACTIVITY("Work programme activities", 2),
  MANDATORY_RELINQUISHMENT("Mandatory relinquishments", 3),
  AREA("Retention, development and pre-producing areas", 4),
  OTHER("Other schedule events", 5);

  private final String displayName;
  private final int displayOrder;

  ScheduleTimelineFilterOption(String displayName, int displayOrder) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public static Optional<ScheduleTimelineFilterOption> forEventType(ScheduleEventType scheduleEventType) {
    return switch (scheduleEventType) {
      case RATE -> Optional.of(RATE);
      case WORK_PROGRAMME_ACTIVITY -> Optional.of(WORK_PROGRAMME_ACTIVITY);
      case OTHER -> Optional.of(OTHER);
      case AREA -> Optional.of(AREA);
      case TERM, PHASE, EXPIRY -> Optional.empty();
    };
  }

  public static List<String> getDefaults() {
    return Arrays.stream(values())
        .map(ScheduleTimelineFilterOption::name)
        .toList();
  }

  public static Map<String, String> getOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(ScheduleTimelineFilterOption.class);
  }
}

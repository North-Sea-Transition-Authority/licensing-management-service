package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum ScheduleEventType implements Displayable {
  TERM("Terms", 999, false),
  PHASE("Phases", 999, false),
  RATE("Rates", 1, true),
  WORK_PROGRAMME_ACTIVITY("Work programme activities", 2, true),
  OTHER("Other schedule events", 3, true),
  EXPIRY("Expiry", 999, false);

  private final String displayName;
  private final Integer eventTypeOrder;
  private final boolean isFilterable;

  ScheduleEventType(
      String displayName,
      Integer eventTypeOrder,
      boolean isFilterable
  ) {
    this.displayName = displayName;
    this.eventTypeOrder = eventTypeOrder;
    this.isFilterable = isFilterable;
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

  public boolean isFilterable() {
    return isFilterable;
  }

  public static List<String> getFilterDefaults() {
    return Arrays.stream(ScheduleEventType.values())
        .filter(ScheduleEventType::isFilterable)
        .map(ScheduleEventType::name)
        .toList();
  }

  public static Map<String, String> getFilterableEventTypeOptions() {
    var filterableEventTypes = Arrays.stream(ScheduleEventType.values())
        .filter(ScheduleEventType::isFilterable)
        .toList();

    return DisplayableEnumOptionUtil.getDisplayableOptions(filterableEventTypes);
  }
}

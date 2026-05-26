package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum ScheduleEventType implements Displayable {
  TERM("Terms", 999, false, "term"),
  PHASE("Phases", 999, false, "phase"),
  RATE("Rates", 1, true, "rate"),
  WORK_PROGRAMME_ACTIVITY("Work programme activities", 2, true, "work-programme-activity"),
  OTHER("Other schedule events", 3, true, "other-event");

  private final String displayName;
  private final Integer eventTypeOrder;
  private final boolean isFilterable;
  private final String urlSlug;

  ScheduleEventType(
      String displayName,
      Integer eventTypeOrder,
      boolean isFilterable,
      String urlSlug
  ) {
    this.displayName = displayName;
    this.eventTypeOrder = eventTypeOrder;
    this.isFilterable = isFilterable;
    this.urlSlug = urlSlug;
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

  public String getUrlSlug() {
    return urlSlug;
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

  public static ScheduleEventType getFromSlugOrThrow(@NotNull String slug) {
    return Arrays.stream(values())
        .filter(et -> et.getUrlSlug().equals(slug))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid event type slug: " + slug));
  }
}

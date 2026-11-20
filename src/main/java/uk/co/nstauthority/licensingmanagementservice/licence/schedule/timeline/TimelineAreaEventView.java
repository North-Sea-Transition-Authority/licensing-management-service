package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;

public record TimelineAreaEventView(
    String heading,
    String description,
    LocalDate eventDate,
    String eventDateString,
    boolean showProgress
) implements ScheduleEvent {

  static final String PRE_PRODUCING_AREA = "Pre-producing area";
  static final String RETENTION_OR_DEVELOPMENT_AREA = "Retention or development area";

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.AREA;
  }

  @Override
  public LocalDate getSortingDate() {
    return eventDate;
  }

  public static ScheduleEvent getScheduleEventFrom(LicenceScheduleAreaEvent areaEvent, LocalDate finalProgressDate) {
    return new TimelineAreaEventView(
        heading(areaEvent),
        Objects.requireNonNullElse(areaEvent.getComments(), ""),
        areaEvent.getEventDate(),
        DateFormatUtil.convertToDisplayText(areaEvent.getEventDate()),
        !areaEvent.getEventDate().isAfter(finalProgressDate)
    );
  }

  /**
   * As PEARS shows it: the area name with its short name, suffixed with the version from version 2 on
   * ({@code licensing_retention_areas.get_versioned_short_name}). Older hand-entered events have no area.
   */
  private static String heading(LicenceScheduleAreaEvent areaEvent) {
    if (StringUtils.isBlank(areaEvent.getAreaName())) {
      return "PA".equals(areaEvent.getEventMnem()) ? PRE_PRODUCING_AREA : RETENTION_OR_DEVELOPMENT_AREA;
    }

    var version = areaEvent.getAreaVersion();
    var shortName = StringUtils.isBlank(version) || "1".equals(version.trim())
        ? areaEvent.getAreaShortName()
        : "%sv%s".formatted(areaEvent.getAreaShortName(), version.trim());

    return StringUtils.isBlank(shortName)
        ? areaEvent.getAreaName()
        : "%s (%s)".formatted(areaEvent.getAreaName(), shortName);
  }
}

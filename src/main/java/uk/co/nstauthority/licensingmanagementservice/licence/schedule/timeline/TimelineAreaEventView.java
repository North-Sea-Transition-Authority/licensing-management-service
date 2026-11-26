package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;

public record TimelineAreaEventView(
    String heading,
    String areaType,
    String description,
    LocalDate eventDate,
    String eventDateString,
    boolean showProgress
) implements ScheduleEvent {

  static final String PRE_PRODUCING_AREA = "Pre-producing area";
  static final String RETENTION_OR_DEVELOPMENT_AREA = "Retention or development area";
  static final String RETENTION_AREA = "Retention area";
  static final String DEVELOPMENT_AREA = "Development area";

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.AREA;
  }

  @Override
  public LocalDate getSortingDate() {
    return eventDate;
  }

  public static ScheduleEvent getScheduleEventFrom(LicenceScheduleAreaEvent areaEvent, LocalDate finalProgressDate) {
    var heading = heading(areaEvent);
    return new TimelineAreaEventView(
        heading,
        areaType(areaEvent, heading),
        Objects.requireNonNullElse(areaEvent.getComments(), ""),
        areaEvent.getEventDate(),
        DateFormatUtil.convertToDisplayText(areaEvent.getEventDate()),
        !areaEvent.getEventDate().isAfter(finalProgressDate)
    );
  }

  /**
   * The event in one line, for the cross licence event tracker: the heading, the area type where the heading doesn't
   * already say it, and what the event is ({@code Expiry}, {@code Approval}) where PEARS holds it.
   */
  public static String getSummary(LicenceScheduleAreaEvent areaEvent) {
    var heading = heading(areaEvent);
    var subtype = StringUtils.capitalize(
        StringUtils.trimToEmpty(areaEvent.getSubtype()).toLowerCase(Locale.ROOT).replace('_', ' ')
    );
    return Stream.of(heading, areaType(areaEvent, heading), subtype)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.joining(" - "));
  }

  /**
   * The area type, from the area's own type or else the event's code. Empty where the heading already says it, as for an event
   * with no area.
   */
  private static String areaType(LicenceScheduleAreaEvent areaEvent, String heading) {
    var type = switch (StringUtils.trimToEmpty(areaEvent.getAreaType()).toUpperCase(Locale.ROOT)) {
      case "RETENTION" -> RETENTION_AREA;
      case "DEVELOPMENT" -> DEVELOPMENT_AREA;
      case "PROSPECTIVE" -> PRE_PRODUCING_AREA;
      default -> "PA".equals(areaEvent.getEventMnem()) ? PRE_PRODUCING_AREA : RETENTION_OR_DEVELOPMENT_AREA;
    };
    return type.equals(heading) ? "" : type;
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

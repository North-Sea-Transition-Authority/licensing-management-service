package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;

/**
 * One event of a saved schedule version, as PEARS holds it. Apart from the date, values are the text PEARS stores,
 * untrimmed and unparsed: PEARS holds rates such as {@code .5}, percentages such as {@code x} or {@code 50%}, and events
 * with no type at all, and deciding what each means is the mapper's job.
 *
 * @param index         the event's position in the version's {@code EVENT_LIST}, counting from 1, with the schedule
 *                      screen's padding rows counted but not returned
 * @param eventMnem     the event type, for example {@code T}, {@code R} or {@code MR}; null or blank on an untyped event
 * @param eventDate     the date PEARS calculated for the event; null when it holds none, or one that is not a date
 * @param percentage    the mandatory relinquishment percentage ({@code PERCENTAGE})
 * @param area          the area an {@code RD} or {@code PA} event is about; null when the event holds no area
 */
public record PearsScheduleEvent(
    int index,
    @Nullable String eventMnem,
    @Nullable String termNo,
    @Nullable String phase,
    @Nullable String years,
    @Nullable String months,
    @Nullable String days,
    @Nullable LocalDate eventDate,
    @Nullable String recurring,
    @Nullable String recurrances,
    @Nullable String recurEndDate,
    @Nullable String rate,
    @Nullable String percentage,
    @Nullable String comments,
    @Nullable String eventSubtype,
    @Nullable String eventId,
    @Nullable Area area
) {

  /**
   * The area an {@code RD} or {@code PA} event is about, from its {@code RETENTION_AREA} block. Despite PEARS's name for
   * the block, it holds every kind of area: retention and development areas on onshore licences ({@code RD} events),
   * and pre-producing areas on offshore ones ({@code PA} events). {@code type} says which.
   *
   * @param type      {@code RETENTION} or {@code DEVELOPMENT} onshore, {@code PROSPECTIVE} (pre-producing) offshore
   * @param shortName the area's short name, for example {@code A1}
   * @param version   the area's version, which PEARS raises each time the area is changed
   */
  public record Area(
      @Nullable String name,
      @Nullable String shortName,
      @Nullable String type,
      @Nullable String version
  ) {

    /**
     * The area, or null when the event holds no part of one.
     */
    @Nullable
    static Area of(
        @Nullable String name,
        @Nullable String shortName,
        @Nullable String type,
        @Nullable String version
    ) {
      if (name == null && shortName == null && type == null && version == null) {
        return null;
      }
      return new Area(name, shortName, type, version);
    }
  }
}

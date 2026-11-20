package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * The PEARS version being mapped, with its start date and its notes, which join the licence's once the version is
 * mapped. Works out its events' dates the way PEARS does.
 *
 * @param startDate         the version's start date, or another version's when it has none
 * @param startDateBorrowed whether {@code startDate} is another version's
 */
record VersionContext(
    PearsLicenceReference licence,
    PearsScheduleVersion version,
    LocalDate startDate,
    boolean startDateBorrowed,
    MigrationNotes notes
) {

  private static final String TERM = "T";

  /**
   * Whether every event's date is worked out from its offset rather than taken from PEARS: the version has no start date
   * of its own, so its dates are from PEARS's 1900 base, or its stored dates are stale ({@link #hasStaleDates}).
   */
  boolean datesFromOffsets() {
    return startDateBorrowed || hasStaleDates();
  }

  /**
   * Whether the version's stored dates can't be used: a numbered term dated on or before the version's own start date.
   * PEARS worked these versions' dates out from its 1900 base and never recalculated them once the start date was set
   * (data survey S19), but their offsets are right.
   */
  private boolean hasStaleDates() {
    return version.events().stream()
        .anyMatch(event -> TERM.equals(event.eventMnem())
            && StringUtils.isNotBlank(event.termNo())
            && event.eventDate() != null
            && !event.eventDate().isAfter(startDate));
  }

  /**
   * The event's date. PEARS's own date, unless the version's dates can't be used ({@link #datesFromOffsets}) or the
   * event has none: then it is worked out from the event's offset the way PEARS does. Empty when there is neither a
   * usable date nor an offset.
   */
  Optional<LocalDate> eventDate(PearsScheduleEvent event) {
    if (!datesFromOffsets() && event.eventDate() != null) {
      return Optional.of(event.eventDate());
    }

    var offsetDate = offset(event).map(offset -> offset.dateFrom(startDate, 0));

    if (offsetDate.isPresent() && !datesFromOffsets()) {
      notes.note(PearsScheduleMapper.EVENT_DATE_FROM_OFFSET);
    }

    return offsetDate;
  }

  /**
   * The event's offset from the schedule start date as years, months and days, a blank part counting as 0. Empty when
   * every part is blank or any part is not a whole number.
   */
  static Optional<Offset> offset(PearsScheduleEvent event) {
    if (Stream.of(event.years(), event.months(), event.days()).allMatch(StringUtils::isBlank)) {
      return Optional.empty();
    }

    try {
      return Optional.of(new Offset(offsetPart(event.years()), offsetPart(event.months()), offsetPart(event.days())));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private static int offsetPart(String pearsValue) {
    return StringUtils.isBlank(pearsValue) ? 0 : Integer.parseInt(pearsValue.trim());
  }

  PearsScheduleMappingException cannotMap(String reason) {
    return new PearsScheduleMappingException(licence, "%s (version %d)".formatted(reason, version.plsdId()));
  }

  /**
   * An event's offset from the schedule start date, as PEARS holds it.
   */
  record Offset(int years, int months, int days) {

    /**
     * The date PEARS gives the offset from {@code startDate}, {@code extraYears} years on: PEARS places each yearly
     * recurrence of a rate at its offset with the years raised by one each time.
     */
    LocalDate dateFrom(LocalDate startDate, int extraYears) {
      return PearsDates.eventDate(startDate, years + extraYears, months, days);
    }
  }
}

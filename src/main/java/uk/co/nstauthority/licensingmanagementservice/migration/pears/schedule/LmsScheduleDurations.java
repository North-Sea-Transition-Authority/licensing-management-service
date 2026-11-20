package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

/**
 * Works out the LMS durations that land a schedule on the dates PEARS holds. LMS stores durations, not dates, and
 * works the dates out itself ({@code LicenceScheduleCalculationService}), so a duration copied from PEARS's offset could
 * land a few days away. These durations are derived from the PEARS dates instead, so LMS lands on them exactly.
 */
final class LmsScheduleDurations {

  private LmsScheduleDurations() {
  }

  /**
   * The duration of a term or phase that starts on {@code startDate} and ends the day before {@code boundary}, the date
   * PEARS gives its end, where the next term or phase starts.
   *
   * <p>LMS ends a period of whole years or months the day before {@code start + duration}, and a period of days alone
   * on {@code start + days} ({@code calculateDurationEndDate}). So a duration of years or months reaches the boundary
   * itself, and one of days alone stops the day before it. LMS also adds years before months, which differs from
   * {@link Period#between} from a 29 February, so the duration is checked against LMS's own arithmetic rather than
   * taken from {@code Period.between} as it is.
   *
   * @throws IllegalArgumentException if the boundary is not after the start date
   */
  static ThreeFieldDuration untilBoundary(LocalDate startDate, LocalDate boundary) {
    if (!boundary.isAfter(startDate)) {
      throw new IllegalArgumentException("%s is not after %s".formatted(boundary, startDate));
    }

    var duration = untilDate(startDate, boundary);
    if (duration.years() > 0 || duration.months() > 0) {
      return duration;
    }
    return new ThreeFieldDuration(0, 0, (int) ChronoUnit.DAYS.between(startDate, boundary.minusDays(1)));
  }

  /**
   * The relative duration that LMS adds to {@code startDate} to land on {@code date}, as it places a rate relative to
   * the start of its term or phase ({@code calculateRelativeStartDueDate}: years, then months, then days). Whole years
   * and months where they fit, the rest in days.
   *
   * @throws IllegalArgumentException if the date is before the start date
   */
  static ThreeFieldDuration untilDate(LocalDate startDate, LocalDate date) {
    if (date.isBefore(startDate)) {
      throw new IllegalArgumentException("%s is before %s".formatted(date, startDate));
    }

    for (var totalMonths = Period.between(startDate, date).toTotalMonths(); totalMonths > 0; totalMonths--) {
      var years = (int) (totalMonths / 12);
      var months = (int) (totalMonths % 12);
      var afterMonths = startDate.plusYears(years).plusMonths(months);

      if (!afterMonths.isAfter(date)) {
        return new ThreeFieldDuration(years, months, (int) ChronoUnit.DAYS.between(afterMonths, date));
      }
    }

    return new ThreeFieldDuration(0, 0, (int) ChronoUnit.DAYS.between(startDate, date));
  }
}

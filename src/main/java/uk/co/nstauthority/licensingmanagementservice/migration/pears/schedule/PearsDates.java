package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.time.LocalDate;

/**
 * PEARS's schedule date arithmetic, ported from Oracle so the migration can place an event the way PEARS does. LMS adds
 * offsets differently (months before days, and no month-end rule), so the two only agree away from month ends.
 */
final class PearsDates {

  private PearsDates() {
  }

  /**
   * Oracle's {@code ADD_MONTHS}: adds whole months, and keeps a month end on the month end. A date on the last day of
   * its month lands on the last day of the new month (28-Feb-2015 plus one month is 31-Mar-2015), and a day the new
   * month doesn't have becomes its last day (31-Jan-2015 plus one month is 28-Feb-2015).
   */
  static LocalDate addMonths(LocalDate date, int months) {
    var result = date.plusMonths(months);

    return date.getDayOfMonth() == date.lengthOfMonth()
        ? result.withDayOfMonth(result.lengthOfMonth())
        : result;
  }

  /**
   * The date PEARS gives an event at an offset from the schedule start date, as {@code get_julian_or_event_date} works
   * it out: the days are added first, then the years and months with {@link #addMonths}. So a start of 30-Jan plus
   * 1 month and 1 day is 31-Jan, then 28-Feb, where LMS gives 01-Mar.
   */
  static LocalDate eventDate(LocalDate startDate, int years, int months, int days) {
    return addMonths(startDate.plusDays(days), years * 12 + months);
  }
}

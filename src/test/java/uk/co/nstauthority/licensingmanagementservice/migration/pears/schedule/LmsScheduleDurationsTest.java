package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;

@ExtendWith(MockitoExtension.class)
class LmsScheduleDurationsTest {

  /**
   * LMS's own date arithmetic, which the durations are for. The methods used need none of its services.
   */
  @InjectMocks
  private LicenceScheduleCalculationService calculationService;

  @Test
  void untilBoundary_wholeYears() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);
    var boundary = LocalDate.of(2018, Month.DECEMBER, 1);

    var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);

    assertThat(duration).isEqualTo(new ThreeFieldDuration(4, 0, 0));
    assertThat(calculationService.calculateDurationEndDate(startDate, duration))
        .isEqualTo(LocalDate.of(2018, Month.NOVEMBER, 30));
  }

  @Test
  void untilBoundary_yearsMonthsAndDays() {
    var startDate = LocalDate.of(2009, Month.APRIL, 1);
    var boundary = LocalDate.of(2016, Month.DECEMBER, 31);

    var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);

    assertThat(duration).isEqualTo(new ThreeFieldDuration(7, 8, 30));
    assertThat(calculationService.calculateDurationEndDate(startDate, duration))
        .isEqualTo(LocalDate.of(2016, Month.DECEMBER, 30));
  }

  @Test
  void untilBoundary_fromA29February_addsTheYearsBeforeTheMonthsAsLmsDoes() {
    var startDate = LocalDate.of(2016, Month.FEBRUARY, 29);
    var boundary = LocalDate.of(2017, Month.MARCH, 29);

    var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);

    // Period.between gives 1 year 1 month, which LMS would end on 27-Mar-2017
    assertThat(duration).isEqualTo(new ThreeFieldDuration(1, 1, 1));
    assertThat(calculationService.calculateDurationEndDate(startDate, duration))
        .isEqualTo(LocalDate.of(2017, Month.MARCH, 28));
  }

  @Test
  void untilBoundary_underAMonth_isDaysAloneThatStopTheDayBefore() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);
    var boundary = LocalDate.of(2020, Month.JANUARY, 21);

    var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);

    assertThat(duration).isEqualTo(new ThreeFieldDuration(0, 0, 19));
    assertThat(calculationService.calculateDurationEndDate(startDate, duration))
        .isEqualTo(LocalDate.of(2020, Month.JANUARY, 20));
  }

  @Test
  void untilBoundary_whenTheBoundaryIsNotAfterTheStart_thenThrows() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);

    assertThatThrownBy(() -> LmsScheduleDurations.untilBoundary(startDate, startDate))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void untilBoundary_endsTheDayBeforeTheBoundaryForEveryStartDateAndLength() {
    var mismatches = new ArrayList<String>();
    var startDates = LocalDate.of(2015, Month.DECEMBER, 1).datesUntil(LocalDate.of(2017, Month.APRIL, 1)).toList();
    var lengthsInDays = List.of(1, 2, 27, 28, 29, 30, 31, 59, 60, 61, 365, 366, 1_461, 3_000, 9_131);

    for (var startDate : startDates) {
      for (var lengthInDays : lengthsInDays) {
        var boundary = startDate.plusDays(lengthInDays);
        var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);
        var endDate = calculationService.calculateDurationEndDate(startDate, duration);

        if (!endDate.equals(boundary.minusDays(1))) {
          mismatches.add("%s to %s: %s ends %s".formatted(startDate, boundary, duration, endDate));
        }
      }
    }

    assertThat(mismatches).isEmpty();
  }

  @Test
  void untilDate_onTheStartDate_isNothing() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);

    assertThat(LmsScheduleDurations.untilDate(startDate, startDate)).isEqualTo(new ThreeFieldDuration(0, 0, 0));
  }

  @Test
  void untilDate_fromA29February_addsTheYearsBeforeTheMonthsAsLmsDoes() {
    var startDate = LocalDate.of(2016, Month.FEBRUARY, 29);
    var date = LocalDate.of(2017, Month.MARCH, 29);

    var duration = LmsScheduleDurations.untilDate(startDate, date);

    // Period.between gives 1 year 1 month, which LMS would place on 28-Mar-2017
    assertThat(duration).isEqualTo(new ThreeFieldDuration(1, 1, 1));
    assertThat(calculationService.calculateRelativeStartDueDate(startDate, duration)).isEqualTo(date);
  }

  @Test
  void untilDate_whenTheDateIsBeforeTheStart_thenThrows() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);
    var dayBefore = startDate.minusDays(1);

    assertThatThrownBy(() -> LmsScheduleDurations.untilDate(startDate, dayBefore))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void untilDate_landsOnTheDateForEveryStartDateAndLength() {
    var mismatches = new ArrayList<String>();
    var startDates = LocalDate.of(2015, Month.DECEMBER, 1).datesUntil(LocalDate.of(2017, Month.APRIL, 1)).toList();
    var lengthsInDays = List.of(0, 1, 27, 28, 29, 30, 31, 59, 60, 61, 365, 366, 1_461, 3_000, 9_131);

    for (var startDate : startDates) {
      for (var lengthInDays : lengthsInDays) {
        var date = startDate.plusDays(lengthInDays);
        var duration = LmsScheduleDurations.untilDate(startDate, date);
        var placed = calculationService.calculateRelativeStartDueDate(startDate, duration);

        if (!placed.equals(date)) {
          mismatches.add("%s to %s: %s lands on %s".formatted(startDate, date, duration, placed));
        }
      }
    }

    assertThat(mismatches).isEmpty();
  }
}

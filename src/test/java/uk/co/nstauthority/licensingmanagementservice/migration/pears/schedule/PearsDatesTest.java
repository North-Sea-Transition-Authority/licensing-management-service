package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PearsDatesTest {

  @Test
  void addMonths_inTheMiddleOfAMonth_keepsTheDay() {
    assertThat(PearsDates.addMonths(LocalDate.of(2014, Month.DECEMBER, 15), 2))
        .isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 15));
  }

  @Test
  void addMonths_toAShorterMonth_landsOnItsLastDay() {
    assertThat(PearsDates.addMonths(LocalDate.of(2015, Month.JANUARY, 31), 1))
        .isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 28));
    assertThat(PearsDates.addMonths(LocalDate.of(2016, Month.JANUARY, 30), 1))
        .isEqualTo(LocalDate.of(2016, Month.FEBRUARY, 29));
  }

  @Test
  void addMonths_fromTheLastDayOfAMonth_landsOnTheLastDayOfTheNewMonth() {
    assertThat(PearsDates.addMonths(LocalDate.of(2015, Month.FEBRUARY, 28), 1))
        .isEqualTo(LocalDate.of(2015, Month.MARCH, 31));
    assertThat(PearsDates.addMonths(LocalDate.of(2015, Month.APRIL, 30), 1))
        .isEqualTo(LocalDate.of(2015, Month.MAY, 31));
    assertThat(PearsDates.addMonths(LocalDate.of(2016, Month.FEBRUARY, 29), 12))
        .isEqualTo(LocalDate.of(2017, Month.FEBRUARY, 28));
  }

  @Test
  void addMonths_backwards_followsTheSameRules() {
    assertThat(PearsDates.addMonths(LocalDate.of(2015, Month.MARCH, 31), -1))
        .isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 28));
    assertThat(PearsDates.addMonths(LocalDate.of(2015, Month.FEBRUARY, 28), -1))
        .isEqualTo(LocalDate.of(2015, Month.JANUARY, 31));
  }

  @Test
  void eventDate_addsTheDaysBeforeTheMonths() {
    // The plan's example: LMS gives 01-Mar, adding the month first
    assertThat(PearsDates.eventDate(LocalDate.of(2015, Month.JANUARY, 30), 0, 1, 1))
        .isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 28));
  }

  @Test
  void eventDate_reproducesP2244sBespokeMilestones() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);

    assertThat(PearsDates.eventDate(startDate, 8, 11, 29)).isEqualTo(LocalDate.of(2023, Month.NOVEMBER, 30));
    assertThat(PearsDates.eventDate(startDate, 9, 3, 30)).isEqualTo(LocalDate.of(2024, Month.MARCH, 31));
  }

  @Test
  void eventDate_withNoOffset_isTheStartDate() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);

    assertThat(PearsDates.eventDate(startDate, 0, 0, 0)).isEqualTo(startDate);
  }
}

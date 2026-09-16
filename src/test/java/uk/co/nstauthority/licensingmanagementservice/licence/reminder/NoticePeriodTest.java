package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NoticePeriodTest {

  @Test
  void getLatestDeadlineDate() {
    assertThat(NoticePeriod.SIX_MONTHS.getLatestDeadlineDate(LocalDate.of(2026, Month.MARCH, 15)))
        .isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 15));
  }

  @ParameterizedTest
  @CsvSource({
      "2026-09-14, 2026-03-15, true",
      "2026-09-15, 2026-03-15, true",
      "2026-03-15, 2026-03-15, true",
      "2026-03-14, 2026-03-15, false",
      "2026-09-16, 2026-03-15, false"
  })
  void isWithinNoticeWindow(LocalDate deadlineDate, LocalDate today, boolean expected) {
    assertThat(NoticePeriod.SIX_MONTHS.isWithinNoticeWindow(deadlineDate, today)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "2026-08-31, 2026-03-01",
      "2026-08-29, 2026-03-01",
      "2028-08-30, 2028-03-01",
      "2026-10-31, 2026-05-01",
      "2026-09-15, 2026-03-15"
  })
  void isWithinNoticeWindow_whenTodayIsTheNoticeDate_thenTheDeadlineEntersTheWindowThatDayAndNotBefore(
      LocalDate deadlineDate,
      LocalDate noticeDate
  ) {
    assertThat(NoticePeriod.SIX_MONTHS.isWithinNoticeWindow(deadlineDate, noticeDate)).isTrue();
    assertThat(NoticePeriod.SIX_MONTHS.isWithinNoticeWindow(deadlineDate, noticeDate.minusDays(1))).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
      "2026-02-28, 2026-08-28",
      "2026-03-01, 2026-09-01",
      "2028-02-29, 2028-08-29",
      "2026-08-31, 2027-02-28",
      "2026-09-01, 2027-03-01"
  })
  void getLatestDeadlineDate_whenTheMonthsAreDifferentLengths_thenTheWindowNeverShrinksAsTodayAdvances(
      LocalDate today,
      LocalDate expectedLatestDeadlineDate
  ) {
    assertThat(NoticePeriod.SIX_MONTHS.getLatestDeadlineDate(today))
        .isEqualTo(expectedLatestDeadlineDate)
        .isAfterOrEqualTo(NoticePeriod.SIX_MONTHS.getLatestDeadlineDate(today.minusDays(1)));
  }
}

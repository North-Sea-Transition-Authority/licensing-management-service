package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NoticePeriodTest {

  @Test
  void getNoticeDateFor() {
    assertThat(NoticePeriod.SIX_MONTHS.getNoticeDateFor(LocalDate.of(2026, Month.SEPTEMBER, 15)))
        .isEqualTo(LocalDate.of(2026, Month.MARCH, 15));
  }

  @ParameterizedTest
  @CsvSource({
      "2026-08-31, 2026-02-27, false",
      "2026-08-31, 2026-02-28, true",
      "2026-08-31, 2026-03-01, true",
      "2026-10-31, 2026-04-29, false",
      "2026-10-31, 2026-04-30, true",
      "2028-08-30, 2028-02-28, false",
      "2028-08-30, 2028-02-29, true",
      "2026-09-15, 2026-03-14, false",
      "2026-09-15, 2026-03-15, true"
  })
  void isDueBy(LocalDate deadlineDate, LocalDate today, boolean expected) {
    assertThat(NoticePeriod.SIX_MONTHS.isDueBy(deadlineDate, today)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "2026-08-31, 2026-02-28",
      "2026-10-31, 2026-04-30",
      "2028-08-30, 2028-02-29",
      "2026-09-15, 2026-03-15"
  })
  void isDueBy_whenTodayIsTheNoticeDate_thenTheReminderIsDueRatherThanADayLate(
      LocalDate deadlineDate,
      LocalDate noticeDate
  ) {
    assertThat(NoticePeriod.SIX_MONTHS.isDueBy(deadlineDate, noticeDate)).isTrue();
    assertThat(NoticePeriod.SIX_MONTHS.isDueBy(deadlineDate, noticeDate.minusDays(1))).isFalse();
    assertThat(noticeDate.plusMonths(NoticePeriod.SIX_MONTHS.getMonths()))
        .isBeforeOrEqualTo(deadlineDate);
  }
}

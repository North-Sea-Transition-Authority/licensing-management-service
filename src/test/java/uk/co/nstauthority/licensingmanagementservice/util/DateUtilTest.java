package uk.co.nstauthority.licensingmanagementservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DateUtilTest {

  @Test
  void getStartOfYear_returnsExpectedInstant() {
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    Instant actual = DateUtil.getStartOfYear(clock, 2020);
    assertThat(actual).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
  }

  @Test
  void formatLongDateWithOrder_whenOrderIsOne_returnsDateWithoutOrder() {
    var date = LocalDate.of(2026, Month.AUGUST, 5);

    assertThat(DateUtil.formatLongDateWithOrder(date, 1)).isEqualTo(DateUtil.formatLongDate(date));
  }

  @Test
  void formatLongDateWithOrder_whenOrderGreaterThanOne_appendsOrderInBrackets() {
    var date = LocalDate.of(2026, Month.AUGUST, 5);

    assertThat(DateUtil.formatLongDateWithOrder(date, 2))
        .isEqualTo("%s (2)".formatted(DateUtil.formatLongDate(date)));
  }

  @Test
  void getEndOfYear_returnsExpectedInstant() {
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    Instant actual = DateUtil.getEndOfYear(clock, 2020);
    assertThat(actual).isEqualTo(Instant.parse("2020-12-31T23:59:59.999999999Z"));
  }
}
package uk.co.nstauthority.licensingmanagementservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

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

  @Test
  void filterByDateRange_includesBoundariesAndExcludesOutOfRangeAndNullDates() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 10);
    var endDate = LocalDate.of(2020, Month.JANUARY, 20);

    var beforeRange = LocalDate.of(2020, Month.JANUARY, 9);
    var onStartDate = LocalDate.of(2020, Month.JANUARY, 10);
    var withinRange = LocalDate.of(2020, Month.JANUARY, 15);
    var onEndDate = LocalDate.of(2020, Month.JANUARY, 20);
    var afterRange = LocalDate.of(2020, Month.JANUARY, 21);

    var dates = Arrays.asList(beforeRange, onStartDate, withinRange, onEndDate, afterRange, null);

    var result = DateUtil.filterByDateRange(dates, Function.identity(), startDate, endDate);

    assertThat(result).containsExactly(onStartDate, withinRange, onEndDate);
  }

  @Test
  void validateDateStrict_whenDateBlank_thenReturnsNullAndHasNoErrors() {
    var bindingResult = ValidatorTestingUtil.getBindingResult(new DateForm());

    var result = DateUtil.validateDateStrict(null, "date", "Date", bindingResult);

    assertThat(result).isNull();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void validateDateStrict_whenDateValid_thenReturnsParsedDateAndHasNoErrors() {
    var bindingResult = ValidatorTestingUtil.getBindingResult(new DateForm());

    var result = DateUtil.validateDateStrict("05/08/2026", "date", "Date", bindingResult);

    assertThat(result).isEqualTo(LocalDate.of(2026, Month.AUGUST, 5));
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void validateDateStrict_whenDateNotReal_thenRejectsFieldAndReturnsNull() {
    var bindingResult = ValidatorTestingUtil.getBindingResult(new DateForm());

    var result = DateUtil.validateDateStrict("31/02/2026", "date", "Date", bindingResult);

    assertThat(result).isNull();
    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "date", "date.invalid", "Date must be a real date in the format dd/mm/yyyy");
  }

  @Test
  void validateDateStrict_whenDateNotParseable_thenRejectsFieldAndReturnsNull() {
    var bindingResult = ValidatorTestingUtil.getBindingResult(new DateForm());

    var result = DateUtil.validateDateStrict("not-a-date", "date", "Date", bindingResult);

    assertThat(result).isNull();
    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "date", "date.invalid", "Date must be a real date in the format dd/mm/yyyy");
  }

  private static class DateForm {

    private String date;

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }
  }
}

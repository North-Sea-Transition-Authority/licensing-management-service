package uk.co.nstauthority.licensingmanagementservice.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

public final class DateUtil {

  private static final DateTimeFormatter LONG_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

  // STRICT resolution so an impossible date (e.g. 31/02/2030) is rejected instead of being
  // silently clamped to the last valid day of the month, which is the SMART (default) behaviour.
  // "uuuu" (year, not "yyyy" year-of-era) is required for STRICT resolution to work with ISO dates.
  public static final DateTimeFormatter STRICT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
      .withResolverStyle(ResolverStyle.STRICT);

  private DateUtil() {
    throw new IllegalUtilClassInstantiationException(DateUtil.class);
  }

  public static Instant getStartOfYear(Clock clock, int year) {
    return LocalDate.of(year, 1, 1)
        .atStartOfDay(clock.getZone())
        .toInstant();
  }

  public static Instant getEndOfYear(Clock clock, int year) {
    return LocalDate.of(year, 12, 31)
        .atTime(LocalTime.MAX)
        .atZone(clock.getZone())
        .toInstant();
  }

  public static String formatLongDate(Temporal temporal) {
    return format(temporal, LONG_DATE_FORMATTER);
  }

  public static String formatLongDateWithOrder(LocalDate date, int order) {
    return order > 1
        ? "%s (%s)".formatted(formatLongDate(date), order)
        : formatLongDate(date);
  }

  private static String format(Temporal temporal, DateTimeFormatter dateTimeFormatter) {
    if (temporal instanceof Instant instant) {
      temporal = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    return dateTimeFormatter.format(temporal);
  }

  /**
   * Parses a date string in dd/MM/uuuu format, rejecting the given field on the {@link BindingResult} if it is
   * not a real date. A blank date is treated as absent rather than invalid.
   *
   * @param date          the date string to parse
   * @param field         the name of the form field to reject on failure
   * @param label         the display label for the field, used in the rejection message
   * @param bindingResult the binding result to reject the field on
   * @return the parsed date, or null if the date was blank or invalid
   */
  public static LocalDate validateDateStrict(String date, String field, String label, BindingResult bindingResult) {
    if (StringUtils.isBlank(date)) {
      return null;
    }

    try {
      return LocalDate.parse(date, STRICT_DATE_FORMAT);
    } catch (DateTimeParseException e) {
      bindingResult.rejectValue(field, "%s.invalid".formatted(field),
          "%s must be a real date in the format dd/mm/yyyy".formatted(label));
      return null;
    }
  }

  public static <T> List<T> filterByDateRange(
      List<T> items,
      Function<T, LocalDate> dateExtractor,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return items.stream()
        .filter(item -> {
          var date = dateExtractor.apply(item);
          return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
        })
        .toList();
  }
}
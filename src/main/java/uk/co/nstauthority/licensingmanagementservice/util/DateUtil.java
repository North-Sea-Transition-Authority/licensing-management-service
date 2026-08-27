package uk.co.nstauthority.licensingmanagementservice.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Function;

public final class DateUtil {

  private static final DateTimeFormatter LONG_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

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
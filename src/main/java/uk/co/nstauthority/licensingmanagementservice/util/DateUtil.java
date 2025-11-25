package uk.co.nstauthority.licensingmanagementservice.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class DateUtil {

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
}
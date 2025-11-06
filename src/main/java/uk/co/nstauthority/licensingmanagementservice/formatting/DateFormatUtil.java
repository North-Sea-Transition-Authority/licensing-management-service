package uk.co.nstauthority.licensingmanagementservice.formatting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateFormatUtil {

  private static final String DISPLAY_DATE_FORMAT = "d MMMM yyyy";
  private static final String DISPLAY_TIME_FORMAT = "HH:mm:ss";

  private DateFormatUtil() {
  }

  public static String convertToDisplayText(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT));
  }

  public static String convertToDisplayTextWithTime(Instant instant) {
    return DateTimeFormatter.ofPattern(String.format("%s %s", DISPLAY_DATE_FORMAT, DISPLAY_TIME_FORMAT))
        .withZone(ZoneId.systemDefault())
        .format(instant);
  }

}

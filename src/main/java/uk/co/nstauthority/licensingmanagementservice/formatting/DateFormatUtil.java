package uk.co.nstauthority.licensingmanagementservice.formatting;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatUtil {

  private static final String DISPLAY_DATE_FORMAT = "d MMMM yyyy";

  private DateFormatUtil() {
  }

  public static String convertToDisplayText(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT));
  }

}

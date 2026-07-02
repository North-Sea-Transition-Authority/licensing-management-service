package uk.co.nstauthority.licensingmanagementservice.components.duration;

import java.time.LocalDate;
import java.time.Period;

public class ThreeFieldDurationDisplayUtil {

  private static final String YEAR = " year";
  private static final String MONTH = " month";
  private static final String DAY = " day";

  private ThreeFieldDurationDisplayUtil() {
  }

  public static String convertToDisplayText(ThreeFieldDuration duration) {

    if (duration == null) {
      return "";
    }

    String durationDisplayText = "";

    durationDisplayText += formatDurationText(duration.years(), YEAR);
    durationDisplayText += formatDurationText(duration.months(), MONTH);
    durationDisplayText += formatDurationText(duration.days(), DAY);

    return durationDisplayText.trim();
  }

  private static String formatDurationText(Integer duration, String displayText) {
    if (duration < 1) {
      return "";
    }

    var plural = duration > 1 ? "s " : " ";

    return duration + displayText + plural;
  }

  public static String convertDatesToDurationDisplayText(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      return "";
    }
    var period = Period.between(startDate, endDate.plusDays(1));
    return convertToDisplayText(new ThreeFieldDuration(period.getYears(), period.getMonths(), period.getDays()));
  }
}
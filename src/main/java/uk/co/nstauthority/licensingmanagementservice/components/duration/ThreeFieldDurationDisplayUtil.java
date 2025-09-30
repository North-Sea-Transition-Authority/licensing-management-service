package uk.co.nstauthority.licensingmanagementservice.components.duration;

public class ThreeFieldDurationDisplayUtil {

  private static final String YEAR = " year";
  private static final String MONTH = " month";
  private static final String DAY = " day";

  private ThreeFieldDurationDisplayUtil() {
  }

  public static String convertToDisplayText(ThreeFieldDuration duration) {
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
}

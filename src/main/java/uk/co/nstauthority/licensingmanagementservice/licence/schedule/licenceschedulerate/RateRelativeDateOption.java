package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum RateRelativeDateOption implements Displayable {
  ON_START_DATE("On the date of the event", 1),
  RELATIVE_TO_START_DATE("On a date relative to the event", 2);

  private final String displayName;
  private final int displayOrder;

  RateRelativeDateOption(
      String displayName,
      int displayOrder
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public static Map<String, String> getRateRelativeDateOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(RateRelativeDateOption.class);
  }
}

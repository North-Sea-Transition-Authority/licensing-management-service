package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum DurationChangeType implements Displayable {
  MAINTAIN("Maintain", 10),
  REDUCE("Reduce", 20),
  EXTEND("Extend", 30),
  ;

  private final String displayName;
  private final int displayOrder;

  DurationChangeType(String displayName, int displayOrder) {
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

  @Override
  public String getEnumName() {
    return name();
  }

  public static Map<String, String> getOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(DurationChangeType.class);
  }
}

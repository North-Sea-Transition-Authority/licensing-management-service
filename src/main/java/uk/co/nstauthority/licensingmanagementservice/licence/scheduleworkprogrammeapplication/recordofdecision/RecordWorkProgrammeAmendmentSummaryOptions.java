package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum RecordWorkProgrammeAmendmentSummaryOptions implements Displayable {
  YES_NOW("Yes, I want to add it now", 10),
  NO_LATER("No, I will add one later", 20),
  NO_ALL_ADDED("No, I have added all work programme activities I need to", 30),
  ;

  private final String displayName;
  private final int displayOrder;

  RecordWorkProgrammeAmendmentSummaryOptions(String displayName, int displayOrder) {
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
    return DisplayableEnumOptionUtil.getDisplayableOptions(RecordWorkProgrammeAmendmentSummaryOptions.class);
  }
}

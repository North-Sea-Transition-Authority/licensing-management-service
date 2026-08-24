package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum WorkProgrammeAmendmentDecision implements Displayable {
  AMEND("Amend duration or text", 10),
  WAIVE("Waive", 20),
  COMPLETE_ON_ANOTHER_LICENCE("To be completed on another licence", 30),
  ACKNOWLEDGE("Acknowledge - no further action", 40),
  ;

  private final String displayName;
  private final int displayOrder;

  WorkProgrammeAmendmentDecision(String displayName, int displayOrder) {
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
    return DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeAmendmentDecision.class);
  }
}

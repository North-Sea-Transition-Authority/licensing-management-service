package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum RecordOfDecisionResponse implements Displayable {
  GRANTED("Yes", 10),
  REJECTED("No - not approved", 20),
  NOT_REQUESTED("No - not requested", 30),
  ;

  private final String displayName;
  private final int displayOrder;

  RecordOfDecisionResponse(String displayName, int displayOrder) {
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
    return DisplayableEnumOptionUtil.getDisplayableOptions(RecordOfDecisionResponse.class);
  }
}

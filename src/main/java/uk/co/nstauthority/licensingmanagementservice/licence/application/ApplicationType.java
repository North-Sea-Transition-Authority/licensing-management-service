package uk.co.nstauthority.licensingmanagementservice.licence.application;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ApplicationType implements Displayable {

  SCHEDULE_AMENDMENT_APPLICATION("Schedule and work programme amendment application", 10),
  CONTINUATION_APPLICATION("Licence continuation application", 20),
  ;


  private final String displayName;
  private final int displayOrder;

  ApplicationType(
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
}
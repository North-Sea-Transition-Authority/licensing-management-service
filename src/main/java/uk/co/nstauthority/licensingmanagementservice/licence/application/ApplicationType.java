package uk.co.nstauthority.licensingmanagementservice.licence.application;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ApplicationType implements Displayable {

  SCHEDULE_AMENDMENT_APPLICATION("Schedule and work programme amendment application", 10);

  private final String displayName;
  private final Integer displayOrder;

  ApplicationType(
      String displayName,
      Integer displayOrder
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
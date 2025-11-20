package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum RateDefinitionOption implements Displayable {
  TERM("Term", 1),
  PHASE("Phase", 2),
  CUSTOM_PERIOD("Custom period", 3);

  private final String displayName;
  private final int displayOrder;

  RateDefinitionOption(
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

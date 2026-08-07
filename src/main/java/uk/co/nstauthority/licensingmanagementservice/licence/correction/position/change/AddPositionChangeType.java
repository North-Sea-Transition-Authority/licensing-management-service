package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum AddPositionChangeType implements Displayable {
  ADMINISTRATOR_CHANGE(10, "Administrator change"),
  SET_EQUITY(20, "Set equity"),
  TRANSFER_EQUITY(30, "Transfer equity");

  private final int displayOrder;
  private final String displayName;

  AddPositionChangeType(int displayOrder, String displayName) {
    this.displayOrder = displayOrder;
    this.displayName = displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}
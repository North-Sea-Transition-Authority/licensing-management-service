package uk.co.nstauthority.licensingmanagementservice.mockups.eventtracker;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum EventTrackerRequestType implements Displayable {
  CONTINUATION("Continuation", 10),
  EXTENSION("Extension", 20),
  RELINQUISHMENT("Relinquishment", 30)
  ;

  private final String displayName;
  private final int displayOrder;

  EventTrackerRequestType(String displayName, int displayOrder) {
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

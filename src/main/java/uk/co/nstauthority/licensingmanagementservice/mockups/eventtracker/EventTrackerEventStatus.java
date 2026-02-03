package uk.co.nstauthority.licensingmanagementservice.mockups.eventtracker;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum EventTrackerEventStatus implements Displayable {
  REQUEST_PENDING("Request Pending", 10),
  REQUEST_SUBMITTED("Request Submitted", 20),
  FRAMING("Framing", 30),
  CONSULT("Consult", 40),
  DSP_READY("DSP Ready", 50),
  ISSUE_DECISION("Issue Decision", 60)
  ;

  private final String displayName;
  private final int displayOrder;

  EventTrackerEventStatus(String displayName, int displayOrder) {
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

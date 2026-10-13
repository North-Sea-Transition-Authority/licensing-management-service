package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum EventTrackerRequestType implements Displayable {
  CONTINUATION("Continuation", 10, ApplicationType.CONTINUATION_APPLICATION),
  EXTENSION("Extension", 20, ApplicationType.SCHEDULE_AMENDMENT_APPLICATION)
  ;

  private final String displayName;
  private final int displayOrder;
  private final ApplicationType applicationType;

  EventTrackerRequestType(String displayName, int displayOrder, ApplicationType applicationType) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.applicationType = applicationType;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }
}

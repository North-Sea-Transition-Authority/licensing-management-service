package uk.co.nstauthority.licensingmanagementservice.caseevent;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum CaseEventType implements Displayable {
  APPLICATION_SUBMITTED("Application submitted"),
  CASE_NOTE_ADDED("Case note added");

  private final String displayName;

  CaseEventType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}

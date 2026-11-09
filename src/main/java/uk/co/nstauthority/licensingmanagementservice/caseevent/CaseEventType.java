package uk.co.nstauthority.licensingmanagementservice.caseevent;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum CaseEventType implements Displayable {
  APPLICATION_SUBMITTED("Application submitted"),
  APPLICATION_UPDATE_REQUESTED("Application update requested"),
  CASE_NOTE_ADDED("Case note added"),
  DECISION_RECORDED("Decision recorded"),
  STEWARD_ALLOCATED("Steward allocated");

  private final String displayName;

  CaseEventType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}

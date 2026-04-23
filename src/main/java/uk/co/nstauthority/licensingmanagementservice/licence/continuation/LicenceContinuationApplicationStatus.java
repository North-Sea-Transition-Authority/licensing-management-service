package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceContinuationApplicationStatus implements Displayable {
  DRAFT("Draft"),
  SUBMITTED("Submitted"),
  ISSUE_DECISION("Issue decision"),
  COMPLETE("Complete"),
  WITHDRAWN("Withdrawn");

  private final String displayName;

  LicenceContinuationApplicationStatus(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ScheduleWorkProgrammeApplicationStatus implements Displayable {
  DRAFT("Draft"),
  DELETED("Deleted"),
  SUBMITTED("Submitted");

  private final String displayName;

  ScheduleWorkProgrammeApplicationStatus(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}
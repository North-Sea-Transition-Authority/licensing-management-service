package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

public enum LicenceScheduleTimelineAction {
ADD_A_TERM("Add a term", 1),
ADD_A_PHASE("Add a phase", 2);

  private final String displayText;
  private final int displayOrder;

  LicenceScheduleTimelineAction(
      String displayText,
      int displayOrder
  ) {
    this.displayText = displayText;
    this.displayOrder = displayOrder;
  }

  public String getDisplayText() {
    return displayText;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }
}

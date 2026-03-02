package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum OtherScheduleEventDateOption implements Displayable {
  WITHIN_A_TERM("Within a licence term", 1),
  WITHIN_A_PHASE("Within a licence phase", 2),
  RELATIVE_DATE("A date relative to another schedule event", 3);

  private final String displayName;
  private final Integer displayOrder;

  OtherScheduleEventDateOption(
      String displayName,
      Integer displayOrder
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

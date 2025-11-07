package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum WorkProgrammeActivityDateOption implements Displayable {
  FIXED_DATE("A fixed date", 1),
  WITHIN_A_TERM("Within a licence term", 2),
  WITHIN_A_PHASE("Within a licence phase", 3);

  private final String displayName;
  private final Integer displayOrder;

  WorkProgrammeActivityDateOption(
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

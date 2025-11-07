package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum WorkProgrammeActivityCommitment implements Displayable {
  FIRM("Firm", 1),
  CONTINGENT("Contingent", 2),
  CONDITIONAL("Conditional", 3);

  private final String displayName;
  private final Integer displayOrder;

  WorkProgrammeActivityCommitment(
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

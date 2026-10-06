package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum PartialSurrenderSubareaOutcome implements Displayable {
  RELINQUISHED("Relinquished", 10),
  CROPPED("Cropped", 20),
  UNCHANGED("Unchanged", 30);

  private final String displayName;
  private final int displayOrder;

  PartialSurrenderSubareaOutcome(String displayName, int displayOrder) {
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

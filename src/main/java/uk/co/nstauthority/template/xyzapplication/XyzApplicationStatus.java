package uk.co.nstauthority.template.xyzapplication;

import uk.co.nstauthority.template.util.enumutil.Displayable;

public enum XyzApplicationStatus implements Displayable {

  DRAFT("Draft", 10),
  SUBMITTED("Submitted", 20),
  APPROVED("Approved", 30);

  private final String displayName;
  private final int displayOrder;

  XyzApplicationStatus(String displayName, int displayOrder) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return this.displayName;
  }

  @Override
  public int getDisplayOrder() {
    return this.displayOrder;
  }
}

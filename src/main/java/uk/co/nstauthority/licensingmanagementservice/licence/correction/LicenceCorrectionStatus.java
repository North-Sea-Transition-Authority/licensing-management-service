package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceCorrectionStatus implements Displayable {
  IN_PROGRESS("In progress", 10),
  COMPLETE("Complete", 20),
  CANCELLED("Cancelled", 30),
  ;

  private final String displayName;
  private final Integer displayOrder;

  LicenceCorrectionStatus(final String displayName, final Integer displayOrder) {
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

package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceCorrectionStatus implements Displayable {
  IN_PROGRESS("In progress", 10, "govuk-tag--light-blue"),
  COMPLETE("Complete", 20, "govuk-tag--green"),
  CANCELLED("Cancelled", 30, "govuk-tag--grey"),
  ;

  private final String displayName;
  private final Integer displayOrder;
  private final String tagClass;

  LicenceCorrectionStatus(final String displayName, final Integer displayOrder, final String tagClass) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.tagClass = tagClass;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getTagClass() {
    return tagClass;
  }
}

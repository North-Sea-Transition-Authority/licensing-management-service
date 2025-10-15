package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceWorkProgrammeAmendmentSummaryOptions implements Displayable {

  YES_NOW("Yes, I want to request to amend it now", 10),
  YES_LATER("Yes, but I will request to amend it later", 20),
  NO("No, I have requested to amend all work programme activities I need to", 30);

  private final String displayName;
  private final int displayOrder;

  LicenceWorkProgrammeAmendmentSummaryOptions(String displayName, int displayOrder) {
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

  @Override
  public String getEnumName() {
    return this.name();
  }
}
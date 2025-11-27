package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum SwpApplicationRequestPurposeOption implements Displayable {
  EXTEND_A_PHASE_OR_TERM("Extend a phase or term", 10),
  EXTEND_A_TERM("Extend a term", 20),
  AMEND_THE_WORK_PROGRAMME("Amend the work programme", 30),
  ;

  private final String displayName;
  private final int displayOrder;

  SwpApplicationRequestPurposeOption(String displayName, int displayOrder) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return this.displayOrder;
  }

  @Override
  public String getEnumName() {
    return this.name();
  }
}

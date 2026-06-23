package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicencePositionChangeStatus implements Displayable {
  CONSENTED("Consented"),
  NOT_CONSENTED("Not consented")
  ;

  private final String displayName;

  LicencePositionChangeStatus(final String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}

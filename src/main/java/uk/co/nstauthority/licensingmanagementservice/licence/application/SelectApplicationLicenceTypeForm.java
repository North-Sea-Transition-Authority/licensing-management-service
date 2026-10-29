package uk.co.nstauthority.licensingmanagementservice.licence.application;

import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

public class SelectApplicationLicenceTypeForm {

  private LicenceType selectedLicenceType;

  public LicenceType getSelectedLicenceType() {
    return selectedLicenceType;
  }

  public void setSelectedLicenceType(LicenceType selectedLicenceType) {
    this.selectedLicenceType = selectedLicenceType;
  }
}

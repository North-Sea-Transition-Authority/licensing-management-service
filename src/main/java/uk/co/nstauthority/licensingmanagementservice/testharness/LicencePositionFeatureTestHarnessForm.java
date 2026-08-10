package uk.co.nstauthority.licensingmanagementservice.testharness;

import uk.co.fivium.formlibrary.input.StringInput;

class LicencePositionFeatureTestHarnessForm {

  private final StringInput licenceId = new StringInput("licenceId", "a licence");

  public StringInput getLicenceId() {
    return licenceId;
  }
}

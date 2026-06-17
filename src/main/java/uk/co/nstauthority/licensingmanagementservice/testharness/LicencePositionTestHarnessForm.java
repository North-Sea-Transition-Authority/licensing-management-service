package uk.co.nstauthority.licensingmanagementservice.testharness;

import uk.co.fivium.formlibrary.input.StringInput;

public class LicencePositionTestHarnessForm {

  private final StringInput licenceId = new StringInput("licenceId", "a licence");

  private final StringInput secondaryLicenceId = new StringInput("secondaryLicenceId", "a second licence");

  public StringInput getLicenceId() {
    return licenceId;
  }

  public StringInput getSecondaryLicenceId() {
    return secondaryLicenceId;
  }

}

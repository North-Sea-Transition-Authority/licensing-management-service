package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.admin;

import uk.co.fivium.formlibrary.input.StringInput;

public class LicenceAdminCorrectionForm {

  private final StringInput adminId = new StringInput("adminId", "a licence");

  public StringInput getAdminId() {
    return adminId;
  }
}

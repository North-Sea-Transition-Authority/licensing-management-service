package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import uk.co.fivium.formlibrary.input.StringInput;

public class AdministratorChangeForm {
  private final StringInput adminId = new StringInput("adminId", "a licence administrator");

  public StringInput getAdminId() {
    return adminId;
  }
}

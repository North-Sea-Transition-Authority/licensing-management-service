package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import com.google.common.annotations.VisibleForTesting;
import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public class AdministratorChangeForm implements OrganisationChangeForm {
  private final StringInput adminId = new StringInput("id", "a licence administrator");

  public AdministratorChangeForm() {
  }

  @VisibleForTesting
  public AdministratorChangeForm(String adminId) {
    this.adminId.setInputValue(adminId);
  }

  @Override
  public StringInput getId() {
    return adminId;
  }

  @Override
  public Class<? extends LicenceOperation> operationType() {
    return AdministratorOperation.class;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import com.google.common.annotations.VisibleForTesting;
import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;

public class StorageOperatorChangeForm implements OrganisationChangeForm {
  private final StringInput operatorId = new StringInput("id", "a storage operator");

  public StorageOperatorChangeForm() {
  }

  @VisibleForTesting
  public StorageOperatorChangeForm(String operatorId) {
    this.operatorId.setInputValue(operatorId);
  }

  @Override
  public StringInput getId() {
    return operatorId;
  }

  @Override
  public Class<? extends LicenceOperation> operationType() {
    return StorageOperatorOperation.class;
  }

}

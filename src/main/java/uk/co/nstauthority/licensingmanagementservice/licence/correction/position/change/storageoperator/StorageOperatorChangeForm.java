package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import uk.co.fivium.formlibrary.input.StringInput;

public class StorageOperatorChangeForm {
  private final StringInput operatorId = new StringInput("operatorId", "a storage operator");

  public StringInput getOperatorId() {
    return operatorId;
  }
}

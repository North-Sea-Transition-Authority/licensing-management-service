package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;

public class LicencePositionStorageOperatorChangeUtil {

  private LicencePositionStorageOperatorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static StorageOperatorOperation storageOperatorOperation(Integer operatorId) {
    return LicenceOperation.newStorageOperatorOperation()
        .withOperator(operatorId)
        .build();
  }
}

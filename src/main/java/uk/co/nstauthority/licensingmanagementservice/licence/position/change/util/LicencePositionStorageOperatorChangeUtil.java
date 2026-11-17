package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;

public class LicencePositionStorageOperatorChangeUtil {

  private LicencePositionStorageOperatorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static List<LicencePositionChangeType> removeStorageOperatorChange(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.removeChangesOf(changes, StorageOperatorOperation.class);
  }

  public static StorageOperatorOperation storageOperatorOperation(Integer operatorId) {
    return LicenceOperation.newStorageOperatorOperation()
        .withOperator(operatorId)
        .build();
  }
}

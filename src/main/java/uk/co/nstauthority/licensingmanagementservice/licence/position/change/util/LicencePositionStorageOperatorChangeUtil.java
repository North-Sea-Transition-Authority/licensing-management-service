package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;

public class LicencePositionStorageOperatorChangeUtil {

  private LicencePositionStorageOperatorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static List<LicencePositionChangeType> upsertAddStorageOperatorChange(
      List<LicencePositionChangeType> changes,
      Integer operatorId
  ) {
    if (storageOperatorChangeExists(changes)) {
      return replaceStorageOperatorChange(changes, operatorId);
    }
    var updatedChanges = new ArrayList<>(changes);
    updatedChanges.add(addStorageOperatorChange(operatorId, updatedChanges.size() + 1));
    return updatedChanges;
  }

  public static boolean storageOperatorChangeExists(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.changeExists(changes, StorageOperatorOperation.class);
  }

  public static List<LicencePositionChangeType> replaceStorageOperatorChange(
      List<LicencePositionChangeType> changes,
      Integer operatorId
  ) {
    return LicencePositionChangeOperationUtil.replaceOperation(
        changes, StorageOperatorOperation.class, storageOperatorOperation(operatorId));
  }

  public static StorageOperatorOperation storageOperatorOperation(Integer operatorId) {
    return LicenceOperation.newStorageOperatorOperation()
        .withOperator(operatorId)
        .build();
  }

  private static AddChange addStorageOperatorChange(Integer operatorId, int changeOrder) {
    return AddChange.buildOperationsChange(List.of(storageOperatorOperation(operatorId)), changeOrder);
  }
}

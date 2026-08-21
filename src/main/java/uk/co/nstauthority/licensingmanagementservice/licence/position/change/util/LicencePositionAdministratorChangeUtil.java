package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

public final class LicencePositionAdministratorChangeUtil {

  private LicencePositionAdministratorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static boolean adminChangeExists(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.changeExists(changes, AdministratorOperation.class);
  }

  public static boolean containsAdminOperation(LicencePositionChange liveChange) {
    return LicencePositionChangeOperationUtil.containsOperation(liveChange, AdministratorOperation.class);
  }

  public static boolean containsAdminOperation(LicencePositionChangeType change) {
    return LicencePositionChangeOperationUtil.containsOperation(change, AdministratorOperation.class);
  }

  public static List<LicencePositionChangeType> upsertAddAdminChange(
      List<LicencePositionChangeType> changes, Integer administratorId
  ) {
    if (adminChangeExists(changes)) {
      return replaceAdminChange(changes, administratorId);
    }
    var updatedChanges = new ArrayList<>(changes);
    updatedChanges.add(addAdminChange(administratorId, updatedChanges.size() + 1));
    return updatedChanges;
  }

  public static List<LicencePositionChangeType> replaceAdminChange(
      List<LicencePositionChangeType> changes, Integer administratorId
  ) {
    return LicencePositionChangeOperationUtil.replaceOperation(
        changes, AdministratorOperation.class, administratorOperation(administratorId));
  }

  public static List<LicencePositionChangeType> removeAdminChange(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.removeChangesOf(changes, AdministratorOperation.class);
  }

  public static boolean adminIdNotChanged(LicencePositionChange change, Integer administratorId) {
    return change.getOperations().stream()
        .filter(AdministratorOperation.class::isInstance)
        .map(operation -> ((AdministratorOperation) operation).operatorId())
        .anyMatch(operatorId -> operatorId.equals(administratorId));
  }

  private static AddChange addAdminChange(Integer administratorId, int changeOrder) {
    return AddChange.buildOperationsChange(List.of(administratorOperation(administratorId)), changeOrder);
  }

  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}
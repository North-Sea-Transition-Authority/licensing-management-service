package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

public final class LicencePositionAdministratorChangeUtil {

  private LicencePositionAdministratorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static boolean adminChangeExists(List<LicencePositionChangeType> changes) {
    return changes.stream().anyMatch(LicencePositionAdministratorChangeUtil::containsAdminOperation);
  }

  public static boolean containsAdminOperation(LicencePositionChange liveChange) {
    return liveChange.getOperations() != null
        && liveChange.getOperations().stream().anyMatch(AdministratorOperation.class::isInstance);
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
    return changes.stream()
        .map(change -> containsAdminOperation(change) ? rebuildWithAdmin(change, administratorId) : change)
        .toList();
  }

  public static List<LicencePositionChangeType> removeAdminChange(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .filter(change -> !containsAdminOperation(change))
        .toList();
  }

  public static boolean adminIdNotChanged(LicencePositionChange change, Integer administratorId) {
    return change.getOperations().stream()
        .filter(AdministratorOperation.class::isInstance)
        .map(operation -> ((AdministratorOperation) operation).operatorId())
        .anyMatch(operatorId -> operatorId.equals(administratorId));
  }

  public static boolean containsAdminOperation(LicencePositionChangeType change) {
    return operationsOf(change).stream()
        .map(changeOperation -> switch (changeOperation) {
          case LicencePositionAddOperation addOperation -> addOperation.operation();
          case LicencePositionUpdateOperation updateOperation -> updateOperation.operation();
        })
        .anyMatch(AdministratorOperation.class::isInstance);
  }

  private static LicencePositionChangeType rebuildWithAdmin(LicencePositionChangeType change, Integer administratorId) {
    return switch (change) {
      case AddChange addChange -> addAdminChange(administratorId, addChange.changeOrder());
      case UpdateChangeOperations updateChange -> UpdateChangeOperations.buildUpdateAdminChange(
          updateChange.changeId(),
          administratorId
      );
      case RemoveChange ignored -> change;
    };
  }

  private static AddChange addAdminChange(Integer administratorId, int changeOrder) {
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();

    return AddChange.buildOperationsChange(List.of(administratorOperation), changeOrder);
  }

  private static List<LicencePositionChangeOperation> operationsOf(LicencePositionChangeType change) {
    return switch (change) {
      case AddChange addChange -> addChange.operations();
      case UpdateChangeOperations updateChangeOperations -> updateChangeOperations.operations();
      case RemoveChange ignored -> List.of();
    };
  }
}
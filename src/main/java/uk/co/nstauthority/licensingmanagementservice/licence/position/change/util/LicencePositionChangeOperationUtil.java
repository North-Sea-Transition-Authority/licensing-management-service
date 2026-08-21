package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

public final class LicencePositionChangeOperationUtil {

  private LicencePositionChangeOperationUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static boolean containsOperation(
      LicencePositionChangeType change,
      Class<? extends LicenceOperation> operationType
  ) {
    return change.operations().stream()
        .map(LicencePositionChangeOperation::operation)
        .anyMatch(operationType::isInstance);
  }

  public static boolean containsOperation(
      LicencePositionChange liveChange,
      Class<? extends LicenceOperation> operationType
  ) {
    return findOperation(liveChange, operationType).isPresent();
  }

  public static <T extends LicenceOperation> Optional<T> findOperation(
      LicencePositionChange liveChange,
      Class<T> operationType
  ) {
    if (liveChange.getOperations() == null) {
      return Optional.empty();
    }

    return liveChange.getOperations().stream()
        .filter(operationType::isInstance)
        .map(operationType::cast)
        .findFirst();
  }

  public static boolean changeExists(
      List<LicencePositionChangeType> changes,
      Class<? extends LicenceOperation> operationType
  ) {
    return changes.stream().anyMatch(change -> containsOperation(change, operationType));
  }

  public static <T extends LicenceOperation> List<T> findOperations(
      List<LicencePositionChangeType> changes,
      Class<T> operationType
  ) {
    return changes.stream()
        .map(LicencePositionChangeType::operations)
        .flatMap(List::stream)
        .map(LicencePositionChangeOperation::operation)
        .filter(operationType::isInstance)
        .map(operationType::cast)
        .toList();
  }

  public static Optional<LicencePositionChangeType> findChange(
      List<LicencePositionChangeType> changes,
      Class<? extends LicenceOperation> operationType
  ) {
    return changes.stream()
        .filter(change -> containsOperation(change, operationType))
        .findFirst();
  }

  public static List<LicencePositionChangeType> replaceOperation(
      List<LicencePositionChangeType> changes,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    return changes.stream()
        .map(change -> containsOperation(change, operationType) ? rebuildWith(change, operation) : change)
        .toList();
  }

  public static List<LicencePositionChangeType> upsertUpdateChange(
      List<LicencePositionChangeType> changes,
      Class<? extends LicenceOperation> operationType,
      String liveChangeId,
      LicenceOperation operation
  ) {
    if (changeExists(changes, operationType)) {
      return replaceOperation(changes, operationType, operation);
    }

    return Stream.concat(
            changes.stream(),
            Stream.of(UpdateChangeOperations.buildUpdateChange(liveChangeId, operation)))
        .toList();
  }

  public static List<LicencePositionChangeType> removeChangesOf(
      List<LicencePositionChangeType> changes,
      Class<? extends LicenceOperation> operationType
  ) {
    return changes.stream()
        .filter(change -> !containsOperation(change, operationType))
        .toList();
  }

  private static LicencePositionChangeType rebuildWith(
      LicencePositionChangeType change,
      LicenceOperation operation
  ) {
    return switch (change) {
      case AddChange addChange -> AddChange.buildOperationsChange(List.of(operation), addChange.changeOrder());
      case UpdateChangeOperations updateChange ->
          UpdateChangeOperations.buildUpdateChange(updateChange.changeId(), operation);
      case RemoveChange ignored -> change;
    };
  }
}

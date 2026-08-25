package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

public record PositionChange(
    String changeId,
    Integer changeOrder,
    @Nullable String changeType,
    List<LicenceOperation> operations
) {

  public static List<PositionChange> fromLicencePositionChanges(List<LicencePositionChange> changes) {
    return changes.stream()
        .map(licencePositionChange -> new PositionChange(
            licencePositionChange.getId().toString(),
            licencePositionChange.getChangeOrder(),
            null,
            licencePositionChange.getOperations()
        ))
        .sorted(Comparator.comparingLong(PositionChange::changeOrder))
        .toList();
  }

  public static List<PositionChange> fromPayload(LicencePositionPayload payload) {
    return fromCorrectionChanges(payload.changes());
  }

  public static List<PositionChange> fromCorrectionChanges(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .map(PositionChange::fromCorrectionChange)
        .sorted(Comparator.comparingLong(PositionChange::changeOrder))
        .toList();
  }

  public boolean isOrderable() {
    return !Objects.equals(changeType, LicencePositionChangeType.REMOVE_CHANGE) && !operations.isEmpty();
  }

  public List<PositionValidationError> validate(PositionValidationContext positionValidationContext) {
    if (Objects.equals(changeType, LicencePositionChangeType.REMOVE_CHANGE)) {
      return List.of();
    }

    return operations.stream()
        .map(licenceOperation -> licenceOperation.validate(positionValidationContext))
        .filter(Objects::nonNull)
        .map(positionValidationError -> positionValidationError.withChangeId(changeId))
        .distinct()
        .toList();
  }

  private static PositionChange fromCorrectionChange(LicencePositionChangeType change) {
    return switch (change) {
      case AddChange addChange -> new PositionChange(
          addChange.changeId(),
          addChange.changeOrder(),
          addChange.type(),
          toOperations(addChange.operations())
      );
      case UpdateChangeOperations updateChangeOperations -> new PositionChange(
          updateChangeOperations.changeId(),
          // as this is an update change, this will be overwritten with the changeOrder of the existing change
          Integer.MAX_VALUE,
          updateChangeOperations.type(),
          toOperations(updateChangeOperations.operations())
      );
      case RemoveChange removeChange -> new PositionChange(
          removeChange.changeId(),
          // as this is a remove change, this will be overwritten with the changeOrder of the existing change for display
          Integer.MAX_VALUE,
          removeChange.type(),
          List.of()
      );
      case UpdateChangeOrder updateChangeOrder -> new PositionChange(
          updateChangeOrder.changeId(),
          updateChangeOrder.changeOrder(),
          updateChangeOrder.type(),
          List.of()
      );
    };
  }

  private static List<LicenceOperation> toOperations(List<LicencePositionChangeOperation> changeOperations) {
    return changeOperations.stream()
        .map(LicencePositionChangeOperation::operation)
        .toList();
  }
}

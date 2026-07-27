package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
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
    };
  }

  private static List<LicenceOperation> toOperations(List<LicencePositionChangeOperation> changeOperations) {
    return changeOperations.stream()
        .map(PositionChange::toOperation)
        .toList();
  }

  private static LicenceOperation toOperation(LicencePositionChangeOperation changeOperation) {
    // TODO: update to handle other operation types
    return switch (changeOperation) {
      case LicencePositionAddOperation addOperation -> addOperation.operation();
      case LicencePositionUpdateOperation updateOperation -> updateOperation.operation();
    };
  }
}

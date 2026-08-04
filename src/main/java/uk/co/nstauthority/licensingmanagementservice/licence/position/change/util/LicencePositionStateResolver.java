package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

public final class LicencePositionStateResolver {

  private LicencePositionStateResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<UUID, LicencePositionState> resolveStatesByChronologicalPositionId(
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var statesByChronologicalPositionId = new LinkedHashMap<UUID, LicencePositionState>();

    var currentState = LicencePositionState.EMPTY;
    for (var chronologicalPosition : chronologicalPositions) {
      currentState = applyChanges(currentState, chronologicalPosition);
      statesByChronologicalPositionId.put(chronologicalPosition.id(), currentState);
    }

    return statesByChronologicalPositionId;
  }

  public static LicencePositionState previousState(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<UUID, LicencePositionState> statesByChronologicalPositionId
  ) {
    var previousState = LicencePositionState.EMPTY;
    for (var chronologicalPosition : chronologicalPositions) {
      if (chronologicalPosition.id().equals(currentLicencePositionId)) {
        return previousState;
      }
      previousState = statesByChronologicalPositionId.getOrDefault(chronologicalPosition.id(), LicencePositionState.EMPTY);
    }
    return previousState;
  }

  private static LicencePositionState applyChanges(
      LicencePositionState licencePositionState,
      ChronologicalPosition chronologicalPosition
  ) {
    var currentState = licencePositionState;
    var changes = chronologicalPosition.changes();

    for (var change : changes) {
      currentState = applyChange(currentState, change);
    }

    return currentState;
  }

  private static LicencePositionState applyChange(LicencePositionState state, PositionChange change) {
    if (Objects.equals(change.changeType(), LicencePositionChangeType.REMOVE_CHANGE)) {
      return state;
    }

    var currentState = state;
    for (var operation : change.operations()) {
      currentState = applyOperation(currentState, operation);
    }
    return currentState;
  }

  private static LicencePositionState applyOperation(LicencePositionState state, LicenceOperation operation) {
    //TODO extend the switch statement as other operation types are added
    return switch (operation) {
      case AdministratorOperation administratorOperation ->
          state.withAdministratorId(administratorOperation.operatorId());
      case SetEquityOperation setEquityOperation -> state;
    };
  }
}

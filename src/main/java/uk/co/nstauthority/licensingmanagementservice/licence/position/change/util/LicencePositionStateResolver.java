package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionKey;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

public final class LicencePositionStateResolver {

  private LicencePositionStateResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static ResolvedStates resolve(List<ChronologicalPosition> chronologicalPositions) {
    return resolve(chronologicalPositions, Set.of());
  }

  public static ResolvedStates resolve(
      List<ChronologicalPosition> chronologicalPositions,
      Collection<UUID> positionIdsExcludedFromState
  ) {
    var statesByKey = new TreeMap<PositionKey, LicencePositionState>();
    var keyByPositionId = new LinkedHashMap<UUID, PositionKey>();

    var currentState = LicencePositionState.EMPTY;
    for (var chronologicalPosition : chronologicalPositions) {
      if (!positionIdsExcludedFromState.contains(chronologicalPosition.id())) {
        currentState = applyChanges(currentState, chronologicalPosition);
      }
      var key = PositionKey.from(chronologicalPosition);
      statesByKey.put(key, currentState);
      keyByPositionId.put(chronologicalPosition.id(), key);
    }

    return new ResolvedStates(statesByKey, keyByPositionId);
  }

  private static LicencePositionState applyChanges(
      LicencePositionState positionState,
      ChronologicalPosition chronologicalPosition
  ) {
    var currentState = positionState;
    var changes = chronologicalPosition.changes();

    for (var change : changes) {
      currentState = applyChange(currentState, change);
    }

    return currentState;
  }

  public static LicencePositionState applyChange(LicencePositionState state, PositionChange change) {
    if (Objects.equals(change.changeType(), LicencePositionChangeType.REMOVE_CHANGE)) {
      return state;
    }

    // TODO: refactor as part of https://fivium.atlassian.net/browse/LMS2-243
    var setEquityOperations = change.operations().stream()
        .filter(SetEquityOperation.class::isInstance)
        .map(SetEquityOperation.class::cast)
        .toList();

    var currentState = state;

    // Set equity operations defines the equity holdings for each organisation
    // which is then used to transfer equity between organisations
    if (!setEquityOperations.isEmpty()) {
      currentState = applySetEquityChange(currentState, setEquityOperations);
    }

    for (var operation : change.operations()) {
      // TODO: refactor as part of https://fivium.atlassian.net/browse/LMS2-243
      if (operation instanceof SetEquityOperation) {
        continue;
      }
      currentState = operation.applyState(currentState);
    }
    return currentState;
  }

  // TODO: refactor as part of https://fivium.atlassian.net/browse/LMS2-243
  private static LicencePositionState applySetEquityChange(
      LicencePositionState state,
      List<SetEquityOperation> setEquityOperations
  ) {
    var currentState = state;
    for (var setEquityOperation : setEquityOperations) {
      currentState = setEquityOperation.applyState(currentState);
    }
    return currentState;
  }

}
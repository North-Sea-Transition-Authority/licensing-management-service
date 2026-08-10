package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

public record ResolvedStates(
    NavigableMap<PositionKey, LicencePositionState> statesByKey,
    Map<UUID, PositionKey> keyByPositionId
) {

  public LicencePositionState currentState(UUID positionId) {
    var key = keyByPositionId.get(positionId);
    if (key == null) {
      return LicencePositionState.EMPTY;
    }
    return statesByKey.getOrDefault(key, LicencePositionState.EMPTY);
  }

  public LicencePositionState previousState(UUID positionId) {
    var key = keyByPositionId.get(positionId);
    if (key == null) {
      return LicencePositionState.EMPTY;
    }
    var lower = statesByKey.lowerEntry(key);
    return lower == null ? LicencePositionState.EMPTY : lower.getValue();
  }
}

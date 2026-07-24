package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

public final class LicencePositionChangeUtil {

  private LicencePositionChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<UUID, Integer> administratorIdChangeByPositionId(List<ChronologicalPosition> chronologicalPositions) {
    var administratorIdByPositionId = new HashMap<UUID, Integer>();
    chronologicalPositions.forEach(chronologicalPosition ->
        chronologicalPosition.changes().stream()
            .flatMap(change -> change.operations().stream())
            .filter(AdministratorOperation.class::isInstance)
            .map(operation -> ((AdministratorOperation) operation).operatorId())
            .forEach(operatorId -> administratorIdByPositionId.put(chronologicalPosition.id(), operatorId)));

    return administratorIdByPositionId;
  }
}

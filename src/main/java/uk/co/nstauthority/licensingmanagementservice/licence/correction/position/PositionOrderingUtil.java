package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class PositionOrderingUtil {

  private PositionOrderingUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static List<UUID> moveRelativeTo(
      List<UUID> orderedIds,
      UUID movedId,
      UUID targetId,
      PositionMoveDirection direction
  ) {
    var reordered = new ArrayList<>(orderedIds);
    reordered.remove(movedId);
    var targetIndex = reordered.indexOf(targetId);
    var insertIndex = direction == PositionMoveDirection.BEFORE ? targetIndex : targetIndex + 1;
    reordered.add(insertIndex, movedId);
    return reordered;
  }
}
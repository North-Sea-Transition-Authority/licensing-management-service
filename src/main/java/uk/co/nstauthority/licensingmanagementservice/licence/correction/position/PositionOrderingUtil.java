package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class PositionOrderingUtil {

  private PositionOrderingUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static <T> List<T> moveRelativeTo(
      List<T> orderedIds,
      T movedId,
      T targetId,
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
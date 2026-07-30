package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class PositionMoveOptionUtil {

  private PositionMoveOptionUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static LinkedHashMap<String, String> buildMoveOptions(
      List<OrderablePosition> orderedPositions,
      UUID positionBeingMovedId
  ) {
    var currentSequenceIds = orderedPositions.stream().map(OrderablePosition::id).toList();
    var otherPositions = orderedPositions.stream()
        .filter(position -> !position.id().equals(positionBeingMovedId))
        .toList();

    var moveOptions = new LinkedHashMap<String, String>();
    for (var otherPosition : otherPositions) {
      addMoveOptionIfItChangesOrder(moveOptions, currentSequenceIds, positionBeingMovedId,
          PositionMoveDirection.BEFORE, otherPosition, "Before " + otherPosition.reference());
    }
    if (!otherPositions.isEmpty()) {
      var lastPosition = otherPositions.getLast();
      addMoveOptionIfItChangesOrder(moveOptions, currentSequenceIds, positionBeingMovedId,
          PositionMoveDirection.AFTER, lastPosition, "After " + lastPosition.reference());
    }
    return moveOptions;
  }

  private static void addMoveOptionIfItChangesOrder(
      LinkedHashMap<String, String> moveOptions,
      List<UUID> currentSequenceIds,
      UUID movedPositionId,
      PositionMoveDirection direction,
      OrderablePosition targetPosition,
      String label
  ) {
    var resultingSequence = simulateMove(currentSequenceIds, movedPositionId, direction, targetPosition.id());
    if (!resultingSequence.equals(currentSequenceIds)) {
      moveOptions.put(new PositionMove(direction, targetPosition.id()).toFormValue(), label);
    }
  }

  private static List<UUID> simulateMove(
      List<UUID> sequenceIds,
      UUID movedPositionId,
      PositionMoveDirection direction,
      UUID targetPositionId
  ) {
    return PositionOrderingUtil.moveRelativeTo(sequenceIds, movedPositionId, targetPositionId, direction);
  }

  public static List<PositionOrderView> buildCurrentOrder(
      List<OrderablePosition> orderedPositions,
      UUID positionBeingMovedId
  ) {
    var currentOrder = new ArrayList<PositionOrderView>();
    for (var index = orderedPositions.size() - 1; index >= 0; index--) {
      var position = orderedPositions.get(index);
      currentOrder.add(new PositionOrderView(
          index + 1,
          position.reference(),
          position.id().equals(positionBeingMovedId)
      ));
    }
    return currentOrder;
  }
}
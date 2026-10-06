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
      List<? extends Orderable> orderedItems,
      UUID itemBeingMovedId
  ) {
    var currentSequenceIds = orderedItems.stream().map(Orderable::id).toList();
    var otherItems = orderedItems.stream()
        .filter(item -> !item.id().equals(itemBeingMovedId))
        .toList();

    var moveOptions = new LinkedHashMap<String, String>();
    for (var otherItem : otherItems) {
      addMoveOptionIfItChangesOrder(moveOptions, currentSequenceIds, itemBeingMovedId,
          PositionMoveDirection.BEFORE, otherItem, "Before " + otherItem.reference());
    }
    if (!otherItems.isEmpty()) {
      var lastItem = otherItems.getLast();
      addMoveOptionIfItChangesOrder(moveOptions, currentSequenceIds, itemBeingMovedId,
          PositionMoveDirection.AFTER, lastItem, "After " + lastItem.reference());
    }
    return moveOptions;
  }

  public static LinkedHashMap<String, String> buildInsertOptions(List<? extends Orderable> orderedItems) {
    var insertOptions = new LinkedHashMap<String, String>();
    for (var item : orderedItems) {
      insertOptions.put(
          new PositionMove(PositionMoveDirection.BEFORE, item.id()).toFormValue(),
          "Before " + item.reference()
      );
    }
    if (!orderedItems.isEmpty()) {
      var lastItem = orderedItems.getLast();
      insertOptions.put(
          new PositionMove(PositionMoveDirection.AFTER, lastItem.id()).toFormValue(),
          "After " + lastItem.reference()
      );
    }
    return insertOptions;
  }

  private static void addMoveOptionIfItChangesOrder(
      LinkedHashMap<String, String> moveOptions,
      List<UUID> currentSequenceIds,
      UUID movedItemId,
      PositionMoveDirection direction,
      Orderable targetItem,
      String label
  ) {
    var resultingSequence =
        PositionOrderingUtil.moveRelativeTo(currentSequenceIds, movedItemId, targetItem.id(), direction);
    if (!resultingSequence.equals(currentSequenceIds)) {
      moveOptions.put(new PositionMove(direction, targetItem.id()).toFormValue(), label);
    }
  }

  public static List<PositionOrderView> buildCurrentOrder(
      List<? extends Orderable> orderedItems,
      UUID itemBeingMovedId
  ) {
    var currentOrder = new ArrayList<PositionOrderView>();
    for (var index = orderedItems.size() - 1; index >= 0; index--) {
      var item = orderedItems.get(index);
      currentOrder.add(new PositionOrderView(
          index + 1,
          item.reference(),
          item.id().equals(itemBeingMovedId)
      ));
    }
    return currentOrder;
  }
}
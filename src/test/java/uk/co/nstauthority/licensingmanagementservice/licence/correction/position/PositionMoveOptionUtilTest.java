package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PositionMoveOptionUtilTest {

  private static final LocalDate DATE = LocalDate.of(2026, Month.JUNE, 1);

  @Test
  void buildMoveOptions_whenMovedPositionIsFirst_excludesNoOpMoveAndOffersBeforeAndAfterLast() {
    var moved = position("REF-MOVED", 1);
    var middle = position("REF-MIDDLE", 2);
    var last = position("REF-LAST", 3);

    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(List.of(moved, middle, last), moved.id());

    assertThat(moveOptions).containsExactly(
        entry(new PositionMove(PositionMoveDirection.BEFORE, last.id()).toFormValue(), "Before REF-LAST"),
        entry(new PositionMove(PositionMoveDirection.AFTER, last.id()).toFormValue(), "After REF-LAST")
    );
  }

  @Test
  void buildMoveOptions_whenMovedPositionInMiddle_offersBeforeEarlierPositionAndAfterLast() {
    var first = position("REF-FIRST", 1);
    var moved = position("REF-MOVED", 2);
    var last = position("REF-LAST", 3);

    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(List.of(first, moved, last), moved.id());

    assertThat(moveOptions).containsExactly(
        entry(new PositionMove(PositionMoveDirection.BEFORE, first.id()).toFormValue(), "Before REF-FIRST"),
        entry(new PositionMove(PositionMoveDirection.AFTER, last.id()).toFormValue(), "After REF-LAST")
    );
  }

  @Test
  void buildMoveOptions_whenNoOtherSameDatePositions_returnsEmpty() {
    var moved = position("REF-MOVED", 1);

    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(List.of(moved), moved.id());

    assertThat(moveOptions).isEmpty();
  }

  @Test
  void buildCurrentOrder_returnsRowsLatestFirst_flaggingTheMovedPosition() {
    var first = position("REF-FIRST", 1);
    var moved = position("REF-MOVED", 2);
    var last = position("REF-LAST", 3);

    var currentOrder = PositionMoveOptionUtil.buildCurrentOrder(List.of(first, moved, last), moved.id());

    assertThat(currentOrder).containsExactly(
        new PositionOrderView(3, "REF-LAST", false),
        new PositionOrderView(2, "REF-MOVED", true),
        new PositionOrderView(1, "REF-FIRST", false)
    );
  }

  @Test
  void buildCurrentOrder_whenSinglePosition_returnsThatPositionFlaggedAsMoved() {
    var moved = position("REF-MOVED", 1);

    assertThat(PositionMoveOptionUtil.buildCurrentOrder(List.of(moved), moved.id()))
        .containsExactly(new PositionOrderView(1, "REF-MOVED", true));
  }

  private static OrderablePosition position(String reference, int order) {
    return new OrderablePosition(UUID.randomUUID(), DATE, order, reference, false);
  }
}
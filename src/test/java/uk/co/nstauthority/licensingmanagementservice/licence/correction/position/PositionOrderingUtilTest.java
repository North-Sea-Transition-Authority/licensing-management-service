package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PositionOrderingUtilTest {

  private static final UUID A = UUID.randomUUID();
  private static final UUID B = UUID.randomUUID();
  private static final UUID C = UUID.randomUUID();
  private static final UUID D = UUID.randomUUID();

  @Test
  void moveRelativeTo_before_movesElementImmediatelyBeforeTarget() {
    assertThat(PositionOrderingUtil.moveRelativeTo(List.of(A, B, C, D), D, B, PositionMoveDirection.BEFORE))
        .containsExactly(A, D, B, C);
  }

  @Test
  void moveRelativeTo_after_movesElementImmediatelyAfterTarget() {
    assertThat(PositionOrderingUtil.moveRelativeTo(List.of(A, B, C, D), A, C, PositionMoveDirection.AFTER))
        .containsExactly(B, C, A, D);
  }

  @Test
  void moveRelativeTo_before_targetIsFirst_movesToFront() {
    assertThat(PositionOrderingUtil.moveRelativeTo(List.of(A, B, C), C, A, PositionMoveDirection.BEFORE))
        .containsExactly(C, A, B);
  }

  @Test
  void moveRelativeTo_after_targetIsLast_movesToEnd() {
    assertThat(PositionOrderingUtil.moveRelativeTo(List.of(A, B, C), A, C, PositionMoveDirection.AFTER))
        .containsExactly(B, C, A);
  }

  @Test
  void moveRelativeTo_whenResultingOrderIsUnchanged_returnsSameSequence() {
    assertThat(PositionOrderingUtil.moveRelativeTo(List.of(A, B, C), A, B, PositionMoveDirection.BEFORE))
        .containsExactly(A, B, C);
  }

  @Test
  void moveRelativeTo_doesNotMutateInputList() {
    var input = List.of(A, B, C);

    PositionOrderingUtil.moveRelativeTo(input, A, C, PositionMoveDirection.AFTER);

    assertThat(input).containsExactly(A, B, C);
  }
}
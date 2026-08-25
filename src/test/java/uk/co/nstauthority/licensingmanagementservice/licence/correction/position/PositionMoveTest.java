package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PositionMoveTest {

  @Test
  void toFormValue_encodesDirectionAndTargetPositionId() {
    var targetPositionId = UUID.randomUUID();
    var move = new PositionMove(PositionMoveDirection.BEFORE, targetPositionId);

    assertThat(move.toFormValue()).isEqualTo("BEFORE:" + targetPositionId);
  }

  @Test
  void fromFormValue_parsesDirectionAndTargetPositionId() {
    var targetPositionId = UUID.randomUUID();
    var move = PositionMove.fromFormValue("AFTER:" + targetPositionId);

    assertThat(move.direction()).isEqualTo(PositionMoveDirection.AFTER);
    assertThat(move.targetId()).isEqualTo(targetPositionId);
  }

  @ParameterizedTest
  @EnumSource(PositionMoveDirection.class)
  void toFormValue_thenFromFormValue_roundTrips(PositionMoveDirection direction) {
    var original = new PositionMove(direction, UUID.randomUUID());

    assertThat(PositionMove.fromFormValue(original.toFormValue())).isEqualTo(original);
  }
}

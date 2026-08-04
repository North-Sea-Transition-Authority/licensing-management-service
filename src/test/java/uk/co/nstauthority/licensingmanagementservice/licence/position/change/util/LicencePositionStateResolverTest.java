package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

class LicencePositionStateResolverTest {

  @Test
  void resolveStates_snapshotsAdministratorPerPosition() {
    var positionOne = LicencePositionTestUtil.newBuilder().build();
    var positionTwo = LicencePositionTestUtil.newBuilder().build();

    var chronologicalOne = ChronologicalPositionTestUtil.live(
        positionOne, LicenceOperation.newAdministratorChange().withOperator(1).build());
    var chronologicalTwo = ChronologicalPositionTestUtil.live(
        positionTwo, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(List.of(chronologicalOne, chronologicalTwo));

    assertThat(result.get(positionOne.getId()).administratorId()).isEqualTo(1);
    assertThat(result.get(positionTwo.getId()).administratorId()).isEqualTo(2);
  }

  @Test
  void resolveStates_carriesAdministratorForwardWhenPositionHasNoChange() {
    var oldest = LicencePositionTestUtil.newBuilder().build();
    var middle = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();
    var later = LicencePositionTestUtil.newBuilder().build();

    var oldestChronological = ChronologicalPositionTestUtil.live(
        oldest,
        LicenceOperation.newAdministratorChange().withOperator(1).build()
    );
    var middleChronological = ChronologicalPositionTestUtil.live(
        middle,
        LicenceOperation.newAdministratorChange().withOperator(2).build()
    );
    var currentChronological = ChronologicalPositionTestUtil.live(current);
    var laterChronological = ChronologicalPositionTestUtil.live(
        later,
        LicenceOperation.newAdministratorChange().withOperator(3).build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(oldestChronological, middleChronological, currentChronological, laterChronological)
    );

    // current has no change of its own, so it carries the administrator forward from the middle position
    assertThat(result.get(current.getId()).administratorId()).isEqualTo(2);
  }

  @Test
  void resolveStates_whenRemoveChange_skipsItAndCarriesPreviousAdministratorForward() {
    var earlier = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();
    var later = LicencePositionTestUtil.newBuilder().build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier, LicenceOperation.newAdministratorChange().withOperator(1).build());

    var removeChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        LicencePositionChangeType.REMOVE_CHANGE,
        List.of(LicenceOperation.newAdministratorChange().withOperator(2).build())
    );
    var currentChronological = ChronologicalPosition.fromLicencePosition(
        current, current.getPositionDate(), current.getPositionDateOrder(), List.of(removeChange));

    var laterChronological = ChronologicalPositionTestUtil.live(later);

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(earlierChronological, currentChronological, laterChronological));

    assertThat(result.get(current.getId()).administratorId()).isEqualTo(1);
    assertThat(result.get(later.getId()).administratorId()).isEqualTo(1);
  }

  @Test
  void resolveStates_whenNoAdministratorChange_isNull() {
    var current = LicencePositionTestUtil.newBuilder().build();
    var currentChronological = ChronologicalPositionTestUtil.live(current);

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(List.of(currentChronological));

    assertThat(result.get(current.getId()).administratorId()).isNull();
  }

  @Test
  void previousState_returnsStateCarriedInBeforeTheGivenPosition() {
    var earlier = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier,
        LicenceOperation.newAdministratorChange().withOperator(1).build()
    );
    var currentChronological = ChronologicalPositionTestUtil.live(
        current,
        LicenceOperation.newAdministratorChange().withOperator(2).build()
    );

    var chronologicalPositions = List.of(earlierChronological, currentChronological);
    var states = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions);

    var previousState = LicencePositionStateResolver.previousState(current.getId(), chronologicalPositions, states);

    assertThat(previousState.administratorId()).isEqualTo(1);
  }

  @Test
  void previousState_whenFirstPosition_isEmpty() {
    var current = LicencePositionTestUtil.newBuilder().build();
    var currentChronological = ChronologicalPositionTestUtil.live(
        current, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var chronologicalPositions = List.of(currentChronological);
    var states = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions);

    var previousState = LicencePositionStateResolver.previousState(current.getId(), chronologicalPositions, states);

    assertThat(previousState.administratorId()).isNull();
  }
}

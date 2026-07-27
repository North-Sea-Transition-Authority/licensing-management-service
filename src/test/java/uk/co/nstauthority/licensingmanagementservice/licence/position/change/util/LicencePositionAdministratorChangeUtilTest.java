package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

class LicencePositionAdministratorChangeUtilTest {

  @Test
  void administratorIdChangeByPositionId() {
    var positionOne = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var positionTwo = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var chronologicalPositionOne = ChronologicalPositionTestUtil.live(
        positionOne,
        LicenceOperation.newAdministratorChange().withOperator(1).build()
    );
    var chronologicalPositionTwo = ChronologicalPositionTestUtil.live(
        positionTwo,
        LicenceOperation.newAdministratorChange().withOperator(2).build()
    );

    var result = LicencePositionAdministratorChangeUtil.administratorIdChangeByPositionId(List.of(chronologicalPositionOne, chronologicalPositionTwo));

    assertThat(result)
        .containsOnly(
            entry(positionOne.getId(), 1),
            entry(positionTwo.getId(), 2));
  }

  @Test
  void resolveCurrentAdministratorId_appliesCurrentPositionsOwnChange() {
    var earlier = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var current = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier, LicenceOperation.newAdministratorChange().withOperator(1).build());
    var currentChronological = ChronologicalPositionTestUtil.live(
        current, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var result = LicencePositionAdministratorChangeUtil.resolveCurrentAdministratorId(
        current.getId(), List.of(earlierChronological, currentChronological));

    assertThat(result).isEqualTo(2);
  }

  @Test
  void resolvePreviousAdministratorId_returnsAdministratorBeforeCurrentPositionsChange() {
    var earlier = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var current = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier, LicenceOperation.newAdministratorChange().withOperator(1).build());
    var currentChronological = ChronologicalPositionTestUtil.live(
        current, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var result = LicencePositionAdministratorChangeUtil.resolvePreviousAdministratorId(
        current.getId(), List.of(earlierChronological, currentChronological));

    assertThat(result).isEqualTo(1);
  }

  @Test
  void resolveCurrentAdministratorId_ignoresChangesOnLaterPositions() {
    var oldest = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var middle = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var current = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var later = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var oldestChronological = ChronologicalPositionTestUtil.live(
        oldest, LicenceOperation.newAdministratorChange().withOperator(1).build());
    var middleChronological = ChronologicalPositionTestUtil.live(
        middle, LicenceOperation.newAdministratorChange().withOperator(2).build());
    var currentChronological = ChronologicalPositionTestUtil.live(current);
    var laterChronological = ChronologicalPositionTestUtil.live(
        later, LicenceOperation.newAdministratorChange().withOperator(3).build());

    var result = LicencePositionAdministratorChangeUtil.resolveCurrentAdministratorId(
        current.getId(), List.of(oldestChronological, middleChronological, currentChronological, laterChronological));

    assertThat(result).isEqualTo(2);
  }

  @Test
  void resolveCurrentAdministratorId_whenNoChangeInScope_returnsNull() {
    var current = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var currentChronological = ChronologicalPositionTestUtil.live(current);

    var result = LicencePositionAdministratorChangeUtil.resolveCurrentAdministratorId(
        current.getId(), List.of(currentChronological));

    assertThat(result).isNull();
  }

  @Test
  void resolvePreviousAdministratorId_whenNoEarlierAdminChange_returnsNull() {
    var current = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var currentChronological = ChronologicalPositionTestUtil.live(
        current, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var result = LicencePositionAdministratorChangeUtil.resolvePreviousAdministratorId(
        current.getId(), List.of(currentChronological));

    assertThat(result).isNull();
  }

}

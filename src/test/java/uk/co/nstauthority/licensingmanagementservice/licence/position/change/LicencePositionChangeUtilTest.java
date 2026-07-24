package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

class LicencePositionChangeUtilTest {

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

    var result = LicencePositionChangeUtil.administratorIdChangeByPositionId(List.of(chronologicalPositionOne, chronologicalPositionTwo));

    assertThat(result)
        .containsOnly(
            entry(positionOne.getId(), 1),
            entry(positionTwo.getId(), 2));
  }

}

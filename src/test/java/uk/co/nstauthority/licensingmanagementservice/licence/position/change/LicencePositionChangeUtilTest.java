package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionChangeOperation;

class LicencePositionChangeUtilTest {

  @Test
  void administratorIdChangeByPositionId() {
    var positionOne = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var positionTwo = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var changeOne = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(positionOne)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(1).build()))
        .build();
    var changeTwo = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(positionTwo)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(2).build()))
        .build();

    var result = LicencePositionChangeUtil.administratorIdChangeByPositionId(List.of(changeOne, changeTwo));

    assertThat(result)
        .containsOnly(
            entry(positionOne.getId(), 1),
            entry(positionTwo.getId(), 2));
  }

}

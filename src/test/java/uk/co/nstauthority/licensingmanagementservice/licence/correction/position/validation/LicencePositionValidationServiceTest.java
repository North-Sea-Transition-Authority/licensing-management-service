package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

class LicencePositionValidationServiceTest {

  private final LicencePositionValidationService licencePositionValidationService =
      new LicencePositionValidationService();

  @Test
  void validate_marksOnlyTheFirstPositionAsFirst() {
    // The first position has no admin change, so the flag is true and causes an error
    var first = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());
    var second = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());

    var chronologicalPositions = List.of(first, second);
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(licencePositionValidationService.validate(chronologicalPositions, states, false))
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), "The first licence position must have an administrator change"));
  }

  @Test
  void validate_aggregatesErrorsAcrossPositions() {
    var first = LicencePositionTestUtil.newBuilder().build();
    var second = LicencePositionTestUtil.newBuilder().build();
    var chronologicalPositions = List.of(
        ChronologicalPositionTestUtil.live(first),
        ChronologicalPositionTestUtil.live(
            second,
            LicenceOperation.newAdministratorChange().withOperator(2).build(),
            LicenceOperation.newAdministratorChange().withOperator(3).build())
    );
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(licencePositionValidationService.validate(chronologicalPositions, states, false))
        .extracting(PositionValidationError::message)
        .containsExactlyInAnyOrder(
            "The first licence position must have an administrator change",
            "A licence position can only have one administrator change"
        );
  }
}

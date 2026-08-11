package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class ChronologicalPositionTest {

  @Test
  void validate_whenFirstPositionAndNoAdministratorChange_returnsFirstPositionError() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
          .withPosition(position)
          .withIsFirstPosition(true)
          .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("The first licence position must have an administrator change");
  }

  @Test
  void validate_whenFirstPositionHasAdministratorChange_returnsNoErrors() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change))
        .build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .build()
    );

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenFirstPositionAdministratorChangeRemoved_returnsFirstPositionError() {
    var removedChange = PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(removedChange))
        .build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("The first licence position must have an administrator change");
  }

  @Test
  void validate_whenMoreThanOneAdministratorChange_returnsSingleAdministratorError() {
    var change1 = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    var change2 = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(2).build()))
        .build();

    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change1, change2))
        .build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("A licence position can only have one administrator change");
  }

  @Test
  void validate_whenCarbonStorageAndFirstPositionWithNoAdministratorChange_returnsNoErrors() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .withIsCarbonStorage(true)
            .build()
    );

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenNotFirstPositionAndNoChanges_returnsNoErrors() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
          .withPosition(position)
          .build()
    );

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenOperationInvalid_returnsOperationErrorWithChangeId() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change))
        .build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
          .withPosition(position)
          .withPreviousState(
              PositionStateTestUtil.newBuilder()
                  .withAdministratorId(5)
                  .build()
          ).build()
    );

    assertThat(errors).singleElement().satisfies(error -> {
      assertThat(error.message())
          .isEqualTo("The joining administrator cannot be the same as the withdrawing administrator");
      assertThat(error.changeId()).isEqualTo(change.changeId());
      assertThat(error.operationType()).isEqualTo(LicenceOperation.LICENCE_ADMINISTRATOR);
    });
  }
}

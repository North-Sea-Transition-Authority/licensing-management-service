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
  void validate_whenExtantLicence_anyPosition_mustHaveLicensees() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsExtantLicence(true)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .contains("A licence position must have licensees");
  }

  @Test
  void validate_whenNotExtantLicence_notLastPosition_mustHaveLicensees() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withIsExtantLicence(false)
            .withIsLastPosition(false)
            .withPosition(position)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .contains("A licence position must have licensees");
  }

  @Test
  void validate_whenNotExtantLicence_notLastPosition_mayNotHaveLicensees() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withIsExtantLicence(false)
            .withIsLastPosition(true)
            .withPosition(position)
            .build()
    );

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenFirstPositionAndNoLicenseeChange_returnsFirstPositionError() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .contains("The first licence position must have a licensee change");
  }

  @Test
  void validate_whenFirstPositionWithLicenseeChange_returnsNoErrors() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(
            List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build())).build();
    var position = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of(change)).build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withPreviousState(null)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .isEmpty();
  }

  @Test
  void validate_whenMultipleLicenseeChanges_returnsSingleChangeError() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build(),
            LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1)).build()))
        .build();
    var position = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of(change)).build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("A licence position can only have one licensee change");
  }

  @Test
  void validate_whenJoiningLicenseeOnPreviousState_returnsRepeatedJoinError() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1)).build()))
        .build();
    var position = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of(change)).build();
    var previousState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());
    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withResolvedState(currentState)
            .withPreviousState(previousState)
            .withPosition(position)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("A licensee cannot join a licence they have already joined");
  }

  @Test
  void validate_whenWithdrawingLicenseeNotOnPreviousState_returnsMissingLicenseeError() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newLicenseeOperation().withLicenseesToRemove(List.of(2)).build()))
        .build();
    var position = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of(change)).build();
    var previousState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());
    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(10), List.of());

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withResolvedState(currentState)
            .withPreviousState(previousState)
            .withPosition(position)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("A licensee cannot withdraw from a licence they are not on");
  }

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
        .contains("The first licence position must have an administrator change");
  }

  @Test
  void validate_whenFirstPositionHasNecessaryChanges_returnsNoErrors() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build(),
            LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1)).build()))
        .build();

    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change))
        .build();

    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .withResolvedState(currentState)
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
        .contains("The first licence position must have an administrator change");
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

    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withResolvedState(currentState)
            .build()
    );

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .containsExactly("A licence position can only have one administrator change");
  }

  @Test
  void validate_whenCarbonStorageAndFirstPositionWithNoAdministratorChange_returnsNoErrors() {
    var change = PositionChangeTestUtil.newBuilder().withLicenseeOperation(List.of(1), List.of()).build();
    var position = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of(change)).build();
    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position)
            .withIsFirstPosition(true)
            .withIsCarbonStorage(true)
            .withResolvedState(currentState)
            .build()
    );

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenNotFirstPositionAndNoChanges_returnsNoErrors() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());
    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
          .withPosition(position)
          .withResolvedState(currentState)
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
    var currentState = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());
    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
          .withPosition(position)
          .withResolvedState(currentState)
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

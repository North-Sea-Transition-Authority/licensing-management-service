package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaEquityPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;

class ChronologicalPositionTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails OTHER_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final LicencePositionState LICENSEE_HELD =
      LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());
  private static final LicencePositionState SUBAREAS_HELD =
      LICENSEE_HELD.withBlock(BLOCK_FEATURE_ID, List.of(SUBAREA, OTHER_SUBAREA));

  @Test
  void validate_whenExtantLicence_anyPosition_mustHaveLicensees() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var errors = position.validate(PositionValidationContextTestUtil.newBuilder().withPosition(position).build());

    assertThat(errors)
        .extracting(PositionValidationError::message)
        .contains("A licence position must have licensees");
  }

  @Test
  void validate_whenNotExtantLicence_notLastPosition_mustHaveLicensees() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();

    var errors = position.validate(
        PositionValidationContextTestUtil.newBuilder()
            .withIsLastPosition(false)
            .withPosition(position)
            .withLicenceStatusType(LicenceStatusType.REVOKED)
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
            .withIsLastPosition(true)
            .withPosition(position)
            .withLicenceStatusType(LicenceStatusType.EXPIRED)
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
            .withResolvedState(LicencePositionState.EMPTY.withLicenseeIds(List.of(2), List.of()))
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
            .withLicenceType(LicenceType.CARBON_STORAGE)
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

  @Test
  void validate_whenSubareaTotalNotOneHundred_returnsTotalError() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withPreviousState(SUBAREAS_HELD)
        .withResolvedState(SUBAREAS_HELD.withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 1, new BigDecimal("60")))
        .build();

    var errors = position.validate(context);

    var expected = PositionValidationError.forPosition(
        context,
        SubareaEquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED
    );
    assertThat(errors).containsExactly(expected);
  }

  @Test
  void validate_whenSubareaTotalIsOneHundred_returnsNoErrors() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withPreviousState(SUBAREAS_HELD)
        .withResolvedState(SUBAREAS_HELD
            .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 1, new BigDecimal("60"))
            .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 2, new BigDecimal("40")))
        .build();

    var errors = position.validate(context);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenSubareaHolderOutOfRange_returnsOutOfRangeError() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withPreviousState(SUBAREAS_HELD)
        .withResolvedState(SUBAREAS_HELD
            .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 1, new BigDecimal("150"))
            .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 2, new BigDecimal("-50")))
        .build();

    var errors = position.validate(context);

    var expected = PositionValidationError.forPosition(context, SubareaEquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE);
    assertThat(errors).containsExactly(expected);
  }

  @Test
  void validate_whenSameInvalidSubareaTotalCarriedForward_returnsNoErrors() {
    var invalidState = SUBAREAS_HELD.withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 1, new BigDecimal("60"));
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withPreviousState(invalidState)
        .withResolvedState(invalidState)
        .build();

    var errors = position.validate(context);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenTwoSubareasHaveInvalidTotals_returnsSingleTotalError() {
    var position = ChronologicalPositionTestUtil.newBuilder().build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withPreviousState(SUBAREAS_HELD)
        .withResolvedState(SUBAREAS_HELD
            .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 1, new BigDecimal("60"))
            .withSubareaEquity(BLOCK_FEATURE_ID, OTHER_SUBAREA, 1, new BigDecimal("70")))
        .build();

    var errors = position.validate(context);

    var expected = PositionValidationError.forPosition(
        context,
        SubareaEquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED
    );
    assertThat(errors).containsExactly(expected);
  }

  @Test
  void subareaEquityChangeKeys_whenChangeHasSeveralHolders_returnsOneKeyForTheChange() {
    var operation = subareaSetEquity(SUBAREA, 1, "60");
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change(operation, subareaSetEquity(SUBAREA, 2, "40"))))
        .build();

    var result = position.subareaEquityChangeKeys();

    assertThat(result).containsExactly(operation.subareaKey());
  }

  @Test
  void subareaEquityChangeKeys_whenTwoChangesToSameSubarea_returnsKeyForEachChange() {
    var operation = subareaSetEquity(SUBAREA, 1, "100");
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change(operation), change(subareaSetEquity(SUBAREA, 2, "100"))))
        .build();

    var result = position.subareaEquityChangeKeys();

    var key = operation.subareaKey();
    assertThat(result).containsExactly(key, key);
  }

  @Test
  void subareaEquityChangeKeys_whenChangesToDifferentSubareas_returnsKeyForEachSubarea() {
    var operation = subareaSetEquity(SUBAREA, 1, "100");
    var otherOperation = subareaSetEquity(OTHER_SUBAREA, 1, "100");
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(change(operation), change(otherOperation)))
        .build();

    var result = position.subareaEquityChangeKeys();

    assertThat(result).containsExactly(operation.subareaKey(), otherOperation.subareaKey());
  }

  @Test
  void subareaEquityChangeKeys_whenChangeRemovedOrNotSubareaEquity_returnsNoKeys() {
    var removedChange = PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(subareaSetEquity(SUBAREA, 1, "100")))
        .build();
    var administratorChange = PositionChangeTestUtil.newBuilder().build();
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(removedChange, administratorChange))
        .build();

    var result = position.subareaEquityChangeKeys();

    assertThat(result).isEmpty();
  }

  private static SubareaSetEquityOperation subareaSetEquity(SubareaDetails subarea, int organisationId, String equity) {
    return new SubareaSetEquityOperation(BLOCK_FEATURE_ID, subarea, organisationId, new BigDecimal(equity));
  }

  private static PositionChange change(LicenceOperation... operations) {
    return PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(operations))
        .build();
  }
}

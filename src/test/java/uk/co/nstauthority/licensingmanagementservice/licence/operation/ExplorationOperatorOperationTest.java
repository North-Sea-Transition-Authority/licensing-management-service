package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionStateTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;

class ExplorationOperatorOperationTest {
  @Test
  void builder() {
    var change = LicenceOperation.newExplorationOperatorOperation()
        .withOperator(116)
        .build();

    assertThat(change.id()).isEqualTo(ExplorationOperatorOperation.EXPLORATION_OPERATOR_OPERATION_ID);
    assertThat(change.operatorId()).isEqualTo(116);
    assertThat(change.type()).isEqualTo(LicenceOperation.EXPLORATION_OPERATOR);
  }

  @Test
  void displayName() {
    var operation = new ExplorationOperatorOperation(UUID.randomUUID(), 1);
    assertThat(operation.displayName()).isEqualTo("Exploration operator change");
  }

  @Test
  void nullOperatorId_assertThrows() {
    assertThatThrownBy(() -> new ExplorationOperatorOperation(ExplorationOperatorOperation.EXPLORATION_OPERATOR_OPERATION_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operatorId must not be null");
  }

  @Test
  void validate_whenJoiningExplorationOperatorSameAsWithdrawing_returnsError() {
    var operation = LicenceOperation.newExplorationOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withExplorationOperatorId(100).build())
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var error = operation.validate(context);

    assertThat(error).isNotNull();
    assertThat(error.message())
        .isEqualTo("The joining exploration operator cannot be the same as the withdrawing operator");
    assertThat(error.operationType()).isEqualTo(LicenceOperation.EXPLORATION_OPERATOR);
  }

  @Test
  void validate_whenJoiningExplorationOperatorDiffersFromWithdrawing_returnsNull() {
    var operation = LicenceOperation.newExplorationOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withExplorationOperatorId(200).build())
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void validate_whenNoPreviousExplorationOperator_returnsNull() {
    var operation = LicenceOperation.newExplorationOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withExplorationOperatorId(null).build())
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void organisationUnitIds() {
    var operation = LicenceOperation.newExplorationOperatorOperation().withOperator(100).build();

    var result = operation.organisationUnitIds();

    assertThat(result).containsExactly(100);
  }

  @Test
  void getChangeView() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("Exploration operator").build();
    var previousState = LicencePositionState.EMPTY.withExplorationOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(1, "Zeta", 2, "Beta"), Map.of());
    var urls = ChangeViewUrls.none();
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();

    assertThat(explorationOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isNull();
  }

  @Test
  void applyState() {
    var state = LicencePositionState.EMPTY;
    var expectedState = LicencePositionState.EMPTY.withExplorationOperatorId(2);
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();
    assertThat(explorationOperatorOperation.applyState(state)).isEqualTo(expectedState);
  }
}
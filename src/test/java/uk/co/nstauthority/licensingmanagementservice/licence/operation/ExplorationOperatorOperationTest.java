package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ExplorationOperatorChangeView;

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
  void validate_whenOperatorRemovedAndPreviousOperatorExists_returnsNull() {
    var operation = LicenceOperation.newExplorationOperatorOperation().build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withExplorationOperatorId(100).build())
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void validate_whenNoOperatorJoiningAndNoPreviousOperator_returnsNull() {
    var operation = LicenceOperation.newExplorationOperatorOperation().build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withExplorationOperatorId(null).build())
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
  void organisationUnitIds_operatorIdIsNull() {
    var operation = LicenceOperation.newExplorationOperatorOperation().build();

    var result = operation.organisationUnitIds();

    assertThat(result).isEmpty();
  }

  @Test
  void applyState() {
    var state = LicencePositionState.EMPTY;
    var expectedState = LicencePositionState.EMPTY.withExplorationOperatorId(2);
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();
    assertThat(explorationOperatorOperation.applyState(state)).isEqualTo(expectedState);
  }

  @Test
  void applyState_joiningIdIsNull_clearsExistingOperator() {
    var state = LicencePositionState.EMPTY.withExplorationOperatorId(2);
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(null).build();
    assertThat(explorationOperatorOperation.applyState(state)).usingRecursiveComparison()
        .isEqualTo(LicencePositionState.EMPTY);
  }

  @Test
  void getChangeView() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("exploration operator").build();
    var previousState = LicencePositionState.EMPTY.withExplorationOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(1, "Zeta", 2, "Beta"), Map.of());
    var urls = ChangeViewUrls.none();
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();
    var expectedChangeView = new ExplorationOperatorChangeView(
        "Zeta",
        "Beta",
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(explorationOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }

  @Test
  void getChangeView_nameNotAvailable() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("exploration operator").build();
    var previousState = LicencePositionState.EMPTY.withExplorationOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of());
    var urls = ChangeViewUrls.none();
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();
    var expectedChangeView = new ExplorationOperatorChangeView(
        NOT_AVAILABLE,
        NOT_AVAILABLE,
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(explorationOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }
  @Test
  void getChangeView_noWithdrawingId_withdrawingNameIsNull() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("exploration operator").build();
    var previousState = LicencePositionState.EMPTY.withExplorationOperatorId(null);
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of());
    var urls = ChangeViewUrls.none();
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation().withOperator(2).build();
    var expectedChangeView = new ExplorationOperatorChangeView(
        null,
        NOT_AVAILABLE,
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(explorationOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }

  @Test
  void getChangeView_noJoiningId_joiningNameIsNull() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("exploration operator").build();
    var previousState = LicencePositionState.EMPTY.withExplorationOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(1, "Zeta"), Map.of());
    var urls = ChangeViewUrls.none();
    var operation = LicenceOperation.newExplorationOperatorOperation().build();
    var expectedChangeView = new ExplorationOperatorChangeView(
        "Zeta",
        null,
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(operation.getChangeView(positionChange, previousState, null, context, urls))
        .isEqualTo(expectedChangeView);
  }
}
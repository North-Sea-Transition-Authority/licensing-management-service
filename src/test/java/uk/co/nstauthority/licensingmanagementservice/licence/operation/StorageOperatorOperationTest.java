package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.StorageOperatorChangeView;

class StorageOperatorOperationTest {

  @Test
  void builder() {
    var change = LicenceOperation.newStorageOperatorOperation()
        .withOperator(116)
        .build();

    assertThat(change.id()).isEqualTo(StorageOperatorOperation.STORAGE_OPERATOR_OPERATION_ID);
    assertThat(change.operatorId()).isEqualTo(116);
    assertThat(change.type()).isEqualTo(LicenceOperation.STORAGE_OPERATOR);
  }

  @Test
  void getOperationUrls() {
    var operation = new StorageOperatorOperation(UUID.randomUUID(), 1);
    assertThat(operation.getOperationUrls()).usingRecursiveComparison().isEqualTo(OperationRoutes.none());
  }

  @Test
  void displayName() {
    var operation = new StorageOperatorOperation(UUID.randomUUID(), 1);
    assertThat(operation.displayName()).isEqualTo("Storage operator change");
  }

  @Test
  void nullOperatorId_assertThrows() {
    assertThatThrownBy(() -> new StorageOperatorOperation(StorageOperatorOperation.STORAGE_OPERATOR_OPERATION_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operatorId must not be null");
  }

  @Test
  void validate_whenJoiningStorageOperatorSameAsWithdrawing_returnsError() {
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withStorageOperatorId(100).build())
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var error = operation.validate(context);

    assertThat(error).isNotNull();
    assertThat(error.message())
        .isEqualTo("The joining storage operator cannot be the same as the withdrawing operator");
    assertThat(error.operationType()).isEqualTo(LicenceOperation.STORAGE_OPERATOR);
  }

  @Test
  void validate_whenJoiningStorageOperatorDiffersFromWithdrawing_returnsNull() {
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withStorageOperatorId(200).build())
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void validate_whenNoPreviousStorageOperator_returnsNull() {
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withStorageOperatorId(null).build())
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void organisationUnitIds() {
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(100).build();

    var result = operation.organisationUnitIds();

    assertThat(result).containsExactly(100);
  }

  @Test
  void getChangeView() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("storage operator").build();
    var previousState = LicencePositionState.EMPTY.withStorageOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(1, "Zeta", 2, "Beta"), Map.of());
    var urls = ChangeViewUrls.none();
    var storageOperatorOperation = LicenceOperation.newStorageOperatorOperation().withOperator(2).build();
    var expectedChangeView = new StorageOperatorChangeView(
        "Zeta",
        "Beta",
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(storageOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }

  @Test
  void getChangeView_nameNotAvailable() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("storage operator").build();
    var previousState = LicencePositionState.EMPTY.withStorageOperatorId(1);
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of());
    var urls = ChangeViewUrls.none();
    var storageOperatorOperation = LicenceOperation.newStorageOperatorOperation().withOperator(2).build();
    var expectedChangeView = new StorageOperatorChangeView(
        NOT_AVAILABLE,
        NOT_AVAILABLE,
        positionChange.changeId(),
        positionChange.changeType(),
        urls
    );

    assertThat(storageOperatorOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }
}
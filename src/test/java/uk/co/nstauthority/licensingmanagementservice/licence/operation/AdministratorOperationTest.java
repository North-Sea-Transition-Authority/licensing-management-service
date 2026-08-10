package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionStateTestUtil;

class AdministratorOperationTest {

  @Test
  void builder() {
    var change = LicenceOperation.newAdministratorChange()
        .withOperator(116)
        .build();

    assertThat(change.id()).isEqualTo(AdministratorOperation.ADMINISTRATOR_OPERATION_ID);
    assertThat(change.operatorId()).isEqualTo(116);
    assertThat(change.type()).isEqualTo(LicenceOperation.LICENCE_ADMINISTRATOR);
  }

  @Test
  void nullOperatorId_assertThrows() {
    assertThatThrownBy(() -> new AdministratorOperation(AdministratorOperation.ADMINISTRATOR_OPERATION_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operatorId must not be null");
  }

  @Test
  void validate_whenJoiningAdministratorSameAsWithdrawing_returnsError() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(100).build())
        .build();

    var error = operation.validate(context);

    assertThat(error).isNotNull();
    assertThat(error.message())
        .isEqualTo("The joining administrator cannot be the same as the withdrawing administrator");
    assertThat(error.operationType()).isEqualTo(LicenceOperation.LICENCE_ADMINISTRATOR);
  }

  @Test
  void validate_whenJoiningAdministratorDiffersFromWithdrawing_returnsNull() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(200).build())
        .build();

    assertThat(operation.validate(context)).isNull();
  }

  @Test
  void validate_whenNoPreviousAdministrator_returnsNull() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();

    var context = PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(null).build())
        .build();

    assertThat(operation.validate(context)).isNull();
  }
}
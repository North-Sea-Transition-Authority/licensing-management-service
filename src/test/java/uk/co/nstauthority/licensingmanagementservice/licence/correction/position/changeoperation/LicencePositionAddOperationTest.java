package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class LicencePositionAddOperationTest {

  @Test
  void builder() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(116).build();

    var addOperation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();

    assertThat(addOperation.operationId()).isEqualTo(operation.id());
    assertThat(addOperation.operation()).isEqualTo(operation);
    assertThat(addOperation.type()).isEqualTo(LicencePositionChangeOperation.ADD_OPERATION);
  }

  @Test
  void nullOperationId_assertThrows() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(116).build();
    assertThatThrownBy(() -> new LicencePositionAddOperation(null, operation))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operationId must not be null");
  }

  @Test
  void nullOperation_assertThrows() {
    assertThatThrownBy(() -> new LicencePositionAddOperation(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operation must not be null");
  }

}
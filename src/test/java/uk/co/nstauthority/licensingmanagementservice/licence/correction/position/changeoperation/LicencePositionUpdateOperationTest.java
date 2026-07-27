package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class LicencePositionUpdateOperationTest {

  @Test
  void builder() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(116).build();

    var updateOperation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();

    assertThat(updateOperation.operationId()).isEqualTo(operation.id());
    assertThat(updateOperation.operation()).isEqualTo(operation);
    assertThat(updateOperation.type()).isEqualTo(LicencePositionChangeOperation.UPDATE_OPERATION);
  }

  @Test
  void nullOperationId_assertThrows() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(116).build();
    assertThatThrownBy(() -> new LicencePositionUpdateOperation(null, operation))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operationId must not be null");
  }

  @Test
  void nullOperation_assertThrows() {
    assertThatThrownBy(() -> new LicencePositionUpdateOperation(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operation must not be null");
  }

}

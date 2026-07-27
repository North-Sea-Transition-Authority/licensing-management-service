package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class UpdateChangeOperationsTest {

  @Test
  void builder() {
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(1)
        .build();

    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();

    List<LicencePositionChangeOperation> changes = List.of(operation);

    var updateChangeOperations = LicencePositionChangeType.updateChangeOperations()
        .withChangeId("123")
        .withOperations(changes)
        .build();

    assertThat(updateChangeOperations.changeId()).isEqualTo("123");
    assertThat(updateChangeOperations.operations()).containsExactly(operation);
    assertThat(updateChangeOperations.type()).isEqualTo(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS);
  }
}

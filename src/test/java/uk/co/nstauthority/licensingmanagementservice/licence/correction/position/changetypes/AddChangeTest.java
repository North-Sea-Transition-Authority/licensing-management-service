package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class AddChangeTest {

  @Test
  void builder() {
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(1)
        .build();

    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();

    List<LicencePositionChangeOperation> changes = List.of(operation);

    var addChanges = LicencePositionChangeType.addChange()
        .withChangeId("123")
        .withChangeOrder(123)
        .withOperations(changes)
        .build();

    assertThat(addChanges.changeId()).isEqualTo("123");
    assertThat(addChanges.changeOrder()).isEqualTo(123);
    assertThat(addChanges.operations()).containsExactly(operation);
    assertThat(addChanges.type()).isEqualTo(LicencePositionChangeType.ADD_CHANGE);
  }
}
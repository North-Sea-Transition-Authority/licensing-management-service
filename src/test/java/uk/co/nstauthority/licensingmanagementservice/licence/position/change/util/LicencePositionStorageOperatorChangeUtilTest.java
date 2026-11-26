package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;

class LicencePositionStorageOperatorChangeUtilTest {

  private static final Integer STORAGE_OPERATOR_ID = 116;

  @Test
  void storageOperatorOperation() {
    var result = LicencePositionStorageOperatorChangeUtil.storageOperatorOperation(STORAGE_OPERATOR_ID);
    assertThat(result).isEqualTo(LicenceOperation.newStorageOperatorOperation().withOperator(STORAGE_OPERATOR_ID).build());
  }

  private LicencePositionChangeType storageOperatorAddChange() {
    var storageOperatorOperation = LicenceOperation.newStorageOperatorOperation().withOperator(STORAGE_OPERATOR_ID).build();
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(storageOperatorOperation.id())
        .withOperation(storageOperatorOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
  }
}
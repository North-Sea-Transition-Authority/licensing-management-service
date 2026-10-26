package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;

class LicencePositionStorageOperatorChangeUtilTest {

  private static final Integer STORAGE_OPERATOR_ID = 116;
  private static final Integer UPDATED_STORAGE_OPERATOR_ID = 999;

  @Test
  void storageOperatorChangeExists() {
    assertThat(LicencePositionStorageOperatorChangeUtil.storageOperatorChangeExists(List.of(storageOperatorAddChange())))
        .isTrue();
  }

  @Test
  void upsertAddStorageOperatorChange_whenNoStorageOperatorChangeExists_appendsAddChange() {
    var result = LicencePositionStorageOperatorChangeUtil
        .upsertAddStorageOperatorChange(List.of(), STORAGE_OPERATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(STORAGE_OPERATOR_ID);
  }

  @Test
  void upsertAddStorageOperatorChange_whenStorageOperatorChangeExists_replacesOperatorInPlace() {
    var existing = storageOperatorAddChange();

    var result = LicencePositionStorageOperatorChangeUtil
        .upsertAddStorageOperatorChange(List.of(existing), UPDATED_STORAGE_OPERATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(UPDATED_STORAGE_OPERATOR_ID);
  }

  @Test
  void replaceStorageOperatorChange_replacesOperatorInMatchingChange() {
    var existing = storageOperatorAddChange();

    var result = LicencePositionStorageOperatorChangeUtil.replaceStorageOperatorChange(List.of(existing), UPDATED_STORAGE_OPERATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(UPDATED_STORAGE_OPERATOR_ID);
  }

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

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionAddOperation) change.operations().getFirst();
    return ((StorageOperatorOperation) op.operation()).operatorId();
  }
}
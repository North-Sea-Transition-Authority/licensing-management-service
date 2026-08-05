package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

class LicencePositionAdministratorChangeUtilTest {

  private static final Integer ADMINISTRATOR_ID = 116;
  private static final Integer UPDATED_ADMINISTRATOR_ID = 999;

  @Test
  void adminChangeExists_whenNoChanges_returnsFalse() {
    assertThat(LicencePositionAdministratorChangeUtil.adminChangeExists(List.of())).isFalse();
  }

  @Test
  void adminChangeExists_whenAddChangeContainsAdministratorOperation_returnsTrue() {
    assertThat(LicencePositionAdministratorChangeUtil.adminChangeExists(List.of(adminAddChange(ADMINISTRATOR_ID)))).isTrue();
  }

  @Test
  void adminChangeExists_whenUpdateChangeContainsAdministratorOperation_returnsTrue() {
    var updateChange = adminUpdateChange(UUID.randomUUID().toString(), ADMINISTRATOR_ID);

    assertThat(LicencePositionAdministratorChangeUtil.adminChangeExists(List.of(updateChange))).isTrue();
  }

  @Test
  void upsertAddAdminChange_whenNoAdminChangeExists_appendsAddChange() {
    var result = LicencePositionAdministratorChangeUtil.upsertAddAdminChange(List.of(), ADMINISTRATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void upsertAddAdminChange_whenAdminChangeExists_replacesOperatorInPlace() {
    var existing = adminAddChange(ADMINISTRATOR_ID);

    var result = LicencePositionAdministratorChangeUtil.upsertAddAdminChange(List.of(existing), UPDATED_ADMINISTRATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  @Test
  void replaceAdminChange_replacesOperatorInMatchingChange() {
    var existing = adminAddChange(ADMINISTRATOR_ID);

    var result = LicencePositionAdministratorChangeUtil.replaceAdminChange(List.of(existing), UPDATED_ADMINISTRATOR_ID);

    assertThat(result).hasSize(1);
    assertThat(operatorIdOf(result.getFirst())).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  @Test
  void removeAdminChange_dropsChangesContainingAdministratorOperation() {
    var adminChange = adminUpdateChange(UUID.randomUUID().toString(), ADMINISTRATOR_ID);

    var result = LicencePositionAdministratorChangeUtil.removeAdminChange(List.of(adminChange));

    assertThat(result).isEmpty();
  }

  @Test
  void removeAdminChange_keepsChangesWithoutAdministratorOperation() {
    var adminChange = adminAddChange(ADMINISTRATOR_ID);
    var setEquityChange = setEquityAddChange();

    var result = LicencePositionAdministratorChangeUtil.removeAdminChange(List.of(adminChange, setEquityChange));

    assertThat(result).containsExactly(setEquityChange);
  }

  @Test
  void containsAdminOperation_whenLiveChangeHasAdministratorOperation_returnsTrue() {
    assertThat(LicencePositionAdministratorChangeUtil.containsAdminOperation(liveAdminChange(ADMINISTRATOR_ID))).isTrue();
  }

  @Test
  void containsAdminOperation_whenLiveChangeHasOnlyNonAdminOperation_returnsFalse() {
    var change = new LicencePositionChange();
    change.setOperations(List.of(new SetEquityOperation(300, BigDecimal.valueOf(50))));

    assertThat(LicencePositionAdministratorChangeUtil.containsAdminOperation(change)).isFalse();
  }

  @Test
  void containsAdminOperation_whenLiveChangeHasNullOperations_returnsFalse() {
    assertThat(LicencePositionAdministratorChangeUtil.containsAdminOperation(new LicencePositionChange())).isFalse();
  }

  @Test
  void adminIdNotChanged_whenLiveAdminMatches_returnsTrue() {
    var change = liveAdminChange(ADMINISTRATOR_ID);

    assertThat(LicencePositionAdministratorChangeUtil.adminIdNotChanged(change, ADMINISTRATOR_ID)).isTrue();
  }

  @Test
  void adminIdNotChanged_whenLiveAdminDiffers_returnsFalse() {
    var change = liveAdminChange(ADMINISTRATOR_ID);

    assertThat(LicencePositionAdministratorChangeUtil.adminIdNotChanged(change, UPDATED_ADMINISTRATOR_ID)).isFalse();
  }

  @Test
  void removeChangeById_dropsMatchingChangeKeepsOthers() {
    var keptId = UUID.randomUUID().toString();
    var removedId = UUID.randomUUID().toString();
    var kept = adminUpdateChange(keptId, ADMINISTRATOR_ID);
    var removed = adminUpdateChange(removedId, UPDATED_ADMINISTRATOR_ID);

    var result = LicencePositionAdministratorChangeUtil.removeChangeById(List.of(kept, removed), removedId);

    assertThat(result).containsExactly(kept);
  }

  @Test
  void removeChangeById_whenNoMatch_returnsUnchanged() {
    var change = adminUpdateChange(UUID.randomUUID().toString(), ADMINISTRATOR_ID);

    var result = LicencePositionAdministratorChangeUtil.removeChangeById(List.of(change), UUID.randomUUID().toString());

    assertThat(result).containsExactly(change);
  }

  private LicencePositionChange liveAdminChange(Integer administratorId) {
    var change = new LicencePositionChange();
    change.setOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(administratorId).build()));
    return change;
  }

  private LicencePositionChangeType adminAddChange(Integer administratorId) {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(administratorId).build();
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
  }

  private LicencePositionChangeType setEquityAddChange() {
    var setEquityOperation = new SetEquityOperation(300, BigDecimal.valueOf(50));
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(setEquityOperation.id())
        .withOperation(setEquityOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(2)
        .withOperations(List.of(operation))
        .build();
  }

  private LicencePositionChangeType adminUpdateChange(String changeId, Integer administratorId) {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(administratorId).build();
    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();
    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId)
        .withOperations(List.of(operation))
        .build();
  }

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var addOperation = (LicencePositionAddOperation) ((AddChange) change).operations().getFirst();
    return ((AdministratorOperation) addOperation.operation()).operatorId();
  }
}

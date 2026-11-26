package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;

class LicencePositionAdministratorChangeUtilTest {

  private static final Integer ADMINISTRATOR_ID = 116;
  private static final Integer UPDATED_ADMINISTRATOR_ID = 999;

  @Test
  void adminChangeExists() {
    assertThat(LicencePositionAdministratorChangeUtil.adminChangeExists(List.of(adminAddChange(ADMINISTRATOR_ID))))
        .isTrue();
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
}
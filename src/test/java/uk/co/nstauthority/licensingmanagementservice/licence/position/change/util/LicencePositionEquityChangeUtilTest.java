package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;

class LicencePositionEquityChangeUtilTest {

  private static final int TRANSFER_TO_ID = 300;
  private static final int TRANSFER_FROM_ID = 301;

  @Test
  void containsEquityOperation_whenAddChangeHasSetEquityOperation_returnsTrue() {
    var change = addChange(new SetEquityOperation(TRANSFER_TO_ID, BigDecimal.TEN));

    assertThat(LicencePositionChangeType.containsEquityOperation(change)).isTrue();
  }

  @Test
  void containsEquityOperation_whenAddChangeHasTransferEquityOperation_returnsTrue() {
    var change = addChange(new TransferEquityOperation(TRANSFER_FROM_ID, TRANSFER_TO_ID, BigDecimal.TEN, null));

    assertThat(LicencePositionChangeType.containsEquityOperation(change)).isTrue();
  }

  @Test
  void containsEquityOperation_whenAddChangeHasOnlyNonEquityOperation_returnsFalse() {
    var change = addChange(LicenceOperation.newAdministratorChange().withOperator(TRANSFER_TO_ID).build());

    assertThat(LicencePositionChangeType.containsEquityOperation(change)).isFalse();
  }

  @Test
  void containsEquityOperation_whenRemoveChange_returnsFalse() {
    var change = LicencePositionChangeType.removeChange().withChangeId(UUID.randomUUID().toString()).build();

    assertThat(LicencePositionChangeType.containsEquityOperation(change)).isFalse();
  }

  private LicencePositionChangeType addChange(LicenceOperation operation) {
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();
  }
}
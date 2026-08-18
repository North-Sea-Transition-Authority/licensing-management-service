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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

class LicencePositionEquityChangeUtilTest {

  private static final int TRANSFER_TO_ID = 300;
  private static final int TRANSFER_FROM_ID = 301;

  @Test
  void containsEquityOperation_whenLiveChangeHasSetEquityOperation_returnsTrue() {
    var change = liveChange(new SetEquityOperation(TRANSFER_TO_ID, BigDecimal.TEN));

    assertThat(LicencePositionChange.containsEquityOperation(change)).isTrue();
  }

  @Test
  void containsEquityOperation_whenLiveChangeHasTransferEquityOperation_returnsTrue() {
    var change = liveChange(new TransferEquityOperation(TRANSFER_FROM_ID, TRANSFER_TO_ID, BigDecimal.TEN, null));

    assertThat(LicencePositionChange.containsEquityOperation(change)).isTrue();
  }

  @Test
  void containsEquityOperation_whenLiveChangeHasOnlyNonEquityOperation_returnsFalse() {
    var change = liveChange(LicenceOperation.newAdministratorChange().withOperator(TRANSFER_TO_ID).build());

    assertThat(LicencePositionChange.containsEquityOperation(change)).isFalse();
  }

  @Test
  void containsEquityOperation_whenLiveChangeHasNullOperations_returnsFalse() {
    assertThat(LicencePositionChange.containsEquityOperation(new LicencePositionChange())).isFalse();
  }

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

  private LicencePositionChange liveChange(LicenceOperation operation) {
    var change = new LicencePositionChange();
    change.setOperations(List.of(operation));
    return change;
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
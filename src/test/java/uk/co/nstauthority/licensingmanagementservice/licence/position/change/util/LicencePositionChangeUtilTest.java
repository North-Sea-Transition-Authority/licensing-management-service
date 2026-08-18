package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

class LicencePositionChangeUtilTest {

  private static final int TRANSFER_TO_ID = 300;
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JANUARY, 1);
  private static final int POSITION_DATE_ORDER = 1;

  @Test
  void removeChangeById_dropsMatchingChangeKeepsOthers() {
    var removedId = UUID.randomUUID().toString();
    var kept = setEquityAddChange(UUID.randomUUID().toString());
    var removed = setEquityAddChange(removedId);

    var result = LicencePositionChangeUtil.removeChangeById(List.of(kept, removed), removedId);

    assertThat(result).containsExactly(kept);
  }

  @Test
  void removeChangeById_whenNoMatch_returnsUnchanged() {
    var change = setEquityAddChange(UUID.randomUUID().toString());

    var result = LicencePositionChangeUtil.removeChangeById(List.of(change), UUID.randomUUID().toString());

    assertThat(result).containsExactly(change);
  }

  @Test
  void positionDateAndOrderUnchanged_whenUpdatePositionWithNullDateAndOrder_returnsTrue() {
    var correction = updatePositionCorrection(null, null);

    assertThat(LicencePositionChangeUtil.positionDateAndOrderUnchanged(correction)).isTrue();
  }

  @Test
  void positionDateAndOrderUnchanged_whenDateMatchesLivePosition_returnsTrue() {
    var correction = updatePositionCorrection(POSITION_DATE, POSITION_DATE_ORDER);

    assertThat(LicencePositionChangeUtil.positionDateAndOrderUnchanged(correction)).isTrue();
  }

  @Test
  void positionDateAndOrderUnchanged_whenDateChanged_returnsFalse() {
    var correction = updatePositionCorrection(LocalDate.of(2026, Month.MARCH, 1), null);

    assertThat(LicencePositionChangeUtil.positionDateAndOrderUnchanged(correction)).isFalse();
  }

  @Test
  void positionDateAndOrderUnchanged_whenOrderChanged_returnsFalse() {
    var correction = updatePositionCorrection(null, 2);

    assertThat(LicencePositionChangeUtil.positionDateAndOrderUnchanged(correction)).isFalse();
  }

  @Test
  void operationsOf_change_returnsAddChangeOperations() {
    var operation = LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN)
        .build();
    var change = setEquityAddChange(UUID.randomUUID().toString(), operation);

    assertThat(LicencePositionChangeType.operationsOf(change)).containsExactly(operation);
  }

  @Test
  void operationsOf_change_whenUpdateChangeOperations_returnsUpdateOperations() {
    var operation = LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN)
        .build();
    var change = LicencePositionChangeType.updateChangeOperations()
        .withChangeId(UUID.randomUUID().toString())
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionUpdateOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();

    assertThat(LicencePositionChangeType.operationsOf(change)).containsExactly(operation);
  }

  @Test
  void operationsOf_change_whenRemoveChange_returnsEmpty() {
    var change = LicencePositionChangeType.removeChange().withChangeId(UUID.randomUUID().toString()).build();

    assertThat(LicencePositionChangeType.operationsOf(change)).isEmpty();
  }

  @Test
  void operationsOf_liveChange_returnsOperations() {
    var operation = LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN)
        .build();
    var liveChange = new LicencePositionChange();
    liveChange.setOperations(List.of(operation));

    assertThat(LicencePositionChange.operationsOf(liveChange)).containsExactly(operation);
  }

  @Test
  void operationsOf_liveChange_whenNullOperations_returnsEmpty() {
    assertThat(LicencePositionChange.operationsOf(new LicencePositionChange())).isEmpty();
  }

  private LicencePositionCorrection updatePositionCorrection(LocalDate effectiveDate, Integer effectiveDateOrder) {
    var position = LicencePositionTestUtil.newBuilder()
        .withPositionDate(POSITION_DATE)
        .withPositionOrder(POSITION_DATE_ORDER)
        .build();
    var payload = new UpdateLicencePositionPayload(effectiveDate, effectiveDateOrder, "TEST-REF", List.of());
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(payload)
        .build();
  }

  private LicencePositionChangeType setEquityAddChange(String changeId) {
    return setEquityAddChange(changeId,
        LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN).build());
  }

  private LicencePositionChangeType setEquityAddChange(String changeId, SetEquityOperation operation) {
    return LicencePositionChangeType.addChange()
        .withChangeId(changeId)
        .withChangeOrder(1)
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();
  }
}
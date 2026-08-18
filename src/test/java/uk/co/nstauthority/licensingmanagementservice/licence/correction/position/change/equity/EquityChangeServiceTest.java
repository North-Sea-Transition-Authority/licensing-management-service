package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;

@ExtendWith(MockitoExtension.class)
class EquityChangeServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final int TRANSFER_TO_ID = 116;
  private static final int TRANSFER_FROM_ID = 117;
  private static final String TRANSFER_TO_NAME = "Transfer To Org Ltd";
  private static final String TRANSFER_FROM_NAME = "Transfer From Org Ltd";

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private TransferEquityCorrectionService transferEquityCorrectionService;

  @Mock
  private SetEquityCorrectionService setEquityCorrectionService;

  @InjectMocks
  private EquityChangeService equityChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  @Test
  void undoEquityChange_whenUpdatePositionEmptyAndDateOrderUnchanged_deletesCorrection() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(setEquityAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService).delete(correction);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void undoEquityChange_whenTransferEquityChange_undoesChange() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(transferEquityAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService).delete(correction);
  }

  @Test
  void undoEquityChange_whenOtherChangesRemain_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var remaining = setEquityAddChange(UUID.randomUUID().toString());
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(setEquityAddChange(changeId), remaining));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(correction);
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).containsExactly(remaining);
  }

  @Test
  void undoEquityChange_whenUpdatePositionEmptyButDateChanged_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .build();
    var payload = new UpdateLicencePositionPayload(
        LocalDate.of(2026, Month.MARCH, 1), null, CORRECTION_REFERENCE, List.of(setEquityAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(correction);
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).isEmpty();
  }

  @Test
  void undoEquityChange_whenAddPositionEmpty_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(UUID.randomUUID().toString())
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withChanges(List.of(setEquityAddChange(changeId)))
        .build();
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(correction);
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload()).isInstanceOf(CreateLicencePositionPayload.class);
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).isEmpty();
  }

  @Test
  void undoEquityChange_whenChangeNotFound_throwsAndDoesNotModify() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(setEquityAddChange(UUID.randomUUID().toString())));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    assertThatThrownBy(() -> equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).delete(correction);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void undoEquityChange_whenTargetChangeIsNonEquityChange_throwsAndDoesNotModify() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(administratorAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);

    assertThatThrownBy(() -> equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).delete(correction);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void getEquityChangeUndoView_whenSetEquityChange_returnsSetEquityRows() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(setEquityAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    var setEquityRow = new SetEquityRow(TRANSFER_TO_NAME, BigDecimal.TEN);

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(setEquityCorrectionService.getSetEquityViews(
        List.of(LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN).build())))
        .thenReturn(List.of(setEquityRow));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of()))
        .thenReturn(List.of());

    var undoView = equityChangeService.getEquityChangeUndoView(LICENCE_CORRECTION, changeId);

    assertThat(undoView.setEquityRows()).containsExactly(setEquityRow);
    assertThat(undoView.transferEquityRows()).isEmpty();
  }

  @Test
  void getEquityChangeUndoView_whenTransferEquityChange_returnsTransferRows() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(transferEquityAddChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    var transferEquityRow = new TransferEquityHoldingView(TRANSFER_FROM_NAME, TRANSFER_TO_NAME, BigDecimal.TEN, null);

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(setEquityCorrectionService.getSetEquityViews(List.of()))
        .thenReturn(List.of());
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(TRANSFER_FROM_ID)
            .withTransferTo(TRANSFER_TO_ID)
            .withEquity(BigDecimal.TEN)
            .build())))
        .thenReturn(List.of(transferEquityRow));

    var undoView = equityChangeService.getEquityChangeUndoView(LICENCE_CORRECTION, changeId);

    assertThat(undoView.setEquityRows()).isEmpty();
    assertThat(undoView.transferEquityRows()).containsExactly(transferEquityRow);
  }

  private LicencePositionChangeType setEquityAddChange(String changeId) {
    var operation = LicenceOperation.newSetEquityOperation()
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(changeId)
        .withChangeOrder(1)
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();
  }

  private LicencePositionChangeType transferEquityAddChange(String changeId) {
    var operation = LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(TRANSFER_FROM_ID)
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(changeId)
        .withChangeOrder(1)
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();
  }

  private LicencePositionChangeType administratorAddChange(String changeId) {
    var operation = LicenceOperation.newAdministratorChange()
        .withOperator(TRANSFER_TO_ID)
        .build();
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
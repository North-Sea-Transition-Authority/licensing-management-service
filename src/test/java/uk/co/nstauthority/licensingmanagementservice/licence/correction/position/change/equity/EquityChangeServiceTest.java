package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
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
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private TransferEquityCorrectionService transferEquityCorrectionService;

  @Mock
  private SetEquityCorrectionService setEquityCorrectionService;

  @InjectMocks
  private EquityChangeService equityChangeService;

  @Test
  void removeExistingEquityChange() {
    var changeId = UUID.randomUUID().toString();

    equityChangeService.removeExistingEquityChange(LICENCE_POSITION, LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, changeId);
  }

  @Test
  void undoEquityChange_whenTheChangeStagesASetEquityOperation_thenDropsIt() {
    var changeId = UUID.randomUUID().toString();
    var correction = givenStagedChange(setEquityAddChange(changeId), setEquityOperation());

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService).dropStagedChange(correction, changeId);
  }

  @Test
  void undoEquityChange_whenTheChangeStagesATransferEquityOperation_thenDropsIt() {
    var changeId = UUID.randomUUID().toString();
    var correction = givenStagedChange(transferEquityAddChange(changeId), transferEquityOperation());

    equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService).dropStagedChange(correction, changeId);
  }

  @Test
  void undoEquityChange_whenTheChangeIsNotAnEquityChange_thenThrowsAndDropsNothing() {
    var changeId = UUID.randomUUID().toString();
    givenStagedChange(administratorAddChange(changeId),
        LicenceOperation.newAdministratorChange().withOperator(1).build());

    assertThatThrownBy(() -> equityChangeService.undoEquityChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).dropStagedChange(any(), any());
  }

  @Test
  void getEquityChangeContext_whenSetEquityChange_returnsSetEquityRows() {
    var changeId = UUID.randomUUID().toString();
    var setEquityRow = new SetEquityRow(TRANSFER_TO_NAME, BigDecimal.TEN);
    givenStagedChange(setEquityAddChange(changeId), setEquityOperation());

    when(setEquityCorrectionService.getSetEquityViews(List.of(setEquityOperation())))
        .thenReturn(List.of(setEquityRow));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of())).thenReturn(List.of());

    var context = equityChangeService.getEquityChangeContext(LICENCE_CORRECTION, changeId);

    assertThat(context).isEqualTo(new EquityChangeContext(List.of(setEquityRow), List.of()));
  }

  @Test
  void getEquityChangeContext_whenTransferEquityChange_returnsTransferRows() {
    var changeId = UUID.randomUUID().toString();
    var transferEquityRow = new TransferEquityHoldingView(TRANSFER_FROM_NAME, TRANSFER_TO_NAME, BigDecimal.TEN, null);
    givenStagedChange(transferEquityAddChange(changeId), transferEquityOperation());

    when(setEquityCorrectionService.getSetEquityViews(List.of())).thenReturn(List.of());
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(transferEquityOperation())))
        .thenReturn(List.of(transferEquityRow));

    var context = equityChangeService.getEquityChangeContext(LICENCE_CORRECTION, changeId);

    assertThat(context).isEqualTo(new EquityChangeContext(List.of(), List.of(transferEquityRow)));
  }

  @Test
  void getExecutedEquityChangeContext_whenSetEquityChange_returnsSetEquityRows() {
    var changeId = UUID.randomUUID();
    var setEquityOperation = LicenceOperation.newSetEquityOperation()
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(setEquityOperation))
        .build();
    var setEquityRow = new SetEquityRow(TRANSFER_TO_NAME, BigDecimal.TEN);

    when(licencePositionChangeService.getByIdOrThrow(changeId)).thenReturn(liveChange);
    when(setEquityCorrectionService.getSetEquityViews(List.of(setEquityOperation)))
        .thenReturn(List.of(setEquityRow));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of())).thenReturn(List.of());

    var context = equityChangeService.getExecutedEquityChangeContext(changeId.toString());

    assertThat(context.setEquityRows()).containsExactly(setEquityRow);
    assertThat(context.transferEquityRows()).isEmpty();
  }

  @Test
  void getExecutedEquityChangeContext_whenTransferEquityChange_returnsTransferRows() {
    var changeId = UUID.randomUUID();
    var transferEquityOperation = LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(TRANSFER_FROM_ID)
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(transferEquityOperation))
        .build();

    var transferEquityRow = new TransferEquityHoldingView(TRANSFER_FROM_NAME, TRANSFER_TO_NAME, BigDecimal.TEN, null);

    when(licencePositionChangeService.getByIdOrThrow(changeId)).thenReturn(liveChange);
    when(setEquityCorrectionService.getSetEquityViews(List.of())).thenReturn(List.of());
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(transferEquityOperation)))
        .thenReturn(List.of(transferEquityRow));

    var context = equityChangeService.getExecutedEquityChangeContext(changeId.toString());

    assertThat(context.setEquityRows()).isEmpty();
    assertThat(context.transferEquityRows()).containsExactly(transferEquityRow);
  }

  private LicencePositionCorrection givenStagedChange(
      LicencePositionChangeType change,
      LicenceOperation... resolvedOperations
  ) {
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE, List.of(change)))
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, change.changeId()))
        .thenReturn(correction);
    when(licencePositionCorrectionService.getStagedChangeOrThrow(correction, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(change))
        .thenReturn(List.of(resolvedOperations));

    return correction;
  }

  private static SetEquityOperation setEquityOperation() {
    return LicenceOperation.newSetEquityOperation().withTransferTo(TRANSFER_TO_ID).withEquity(BigDecimal.TEN).build();
  }

  private static TransferEquityOperation transferEquityOperation() {
    return LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(TRANSFER_FROM_ID)
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
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
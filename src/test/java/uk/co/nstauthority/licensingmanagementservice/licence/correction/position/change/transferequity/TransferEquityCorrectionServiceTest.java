package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;

@ExtendWith(MockitoExtension.class)
class TransferEquityCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionCorrectionRepository licencePositionCorrectionRepository;

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  private TransferEquityCorrectionService transferEquityCorrectionService;

  @BeforeEach
  void setUp() {
    var licencePositionCorrectionService = new LicencePositionCorrectionService(
        licencePositionCorrectionRepository,
        licencePositionRepository,
        licencePositionChangeService
    );
    transferEquityCorrectionService = new TransferEquityCorrectionService(
        licencePositionCorrectionRepository,
        licencePositionCorrectionService,
        organisationUnitQueryService,
        licencePositionViewService
    );
  }

  @Test
  void getEquityHoldingsForCorrection_appliesSetAndTransferOperationsInOrder() {
    var earlierPosition = mock(ChronologicalPosition.class);
    when(earlierPosition.id()).thenReturn(UUID.randomUUID());
    var setChange = mock(PositionChange.class);
    when(setChange.operations()).thenReturn(List.of(
        new SetEquityOperation(1, BigDecimal.valueOf(60)),
        new SetEquityOperation(2, BigDecimal.valueOf(40))
    ));
    when(earlierPosition.changes()).thenReturn(List.of(setChange));

    var currentPosition = mock(ChronologicalPosition.class);
    when(currentPosition.id()).thenReturn(POSITION_ID);
    var transferChange = mock(PositionChange.class);
    when(transferChange.operations()).thenReturn(List.of(
        new TransferEquityOperation(1, 2, BigDecimal.valueOf(15), true)
    ));
    when(currentPosition.changes()).thenReturn(List.of(transferChange));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, POSITION_ID))
        .thenReturn(List.of(earlierPosition, currentPosition));

    assertThat(transferEquityCorrectionService.getEquityHoldingsForCorrection(LICENCE_CORRECTION, POSITION_ID))
        .hasSize(2)
        .containsEntry(1, BigDecimal.valueOf(45))
        .containsEntry(2, BigDecimal.valueOf(55));
  }

  @Test
  void getEquityHoldingsForCorrection_stopsProcessingAfterCurrentPositionIsReached() {
    var earlierPosition = mock(ChronologicalPosition.class);
    when(earlierPosition.id()).thenReturn(UUID.randomUUID());
    var setChange = mock(PositionChange.class);
    when(setChange.operations()).thenReturn(List.of(new SetEquityOperation(1, BigDecimal.valueOf(100))));
    when(earlierPosition.changes()).thenReturn(List.of(setChange));

    var currentPosition = mock(ChronologicalPosition.class);
    when(currentPosition.id()).thenReturn(POSITION_ID);
    var transferChange = mock(PositionChange.class);
    when(transferChange.operations()).thenReturn(List.of(new TransferEquityOperation(1, 2, BigDecimal.valueOf(20), true)));
    when(currentPosition.changes()).thenReturn(List.of(transferChange));

    var laterPosition = mock(ChronologicalPosition.class);

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, POSITION_ID))
        .thenReturn(List.of(earlierPosition, currentPosition, laterPosition));

    assertThat(transferEquityCorrectionService.getEquityHoldingsForCorrection(LICENCE_CORRECTION, POSITION_ID))
        .containsEntry(1, BigDecimal.valueOf(80))
        .containsEntry(2, BigDecimal.valueOf(20));
  }

  @Test
  void getEquityHoldingsForAddedPosition_resolvesPositionIdFromPayloadThenDelegates() {
    var payload = mock(CreateLicencePositionPayload.class);
    when(payload.licencePositionId()).thenReturn(POSITION_ID.toString());
    var positionCorrection = mock(LicencePositionCorrection.class);
    when(positionCorrection.getPayload()).thenReturn(payload);

    var currentPosition = mock(ChronologicalPosition.class);
    when(currentPosition.id()).thenReturn(POSITION_ID);
    var setChange = mock(PositionChange.class);
    when(setChange.operations()).thenReturn(List.of(new SetEquityOperation(1, BigDecimal.valueOf(100))));
    when(currentPosition.changes()).thenReturn(List.of(setChange));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, POSITION_ID))
        .thenReturn(List.of(currentPosition));

    assertThat(transferEquityCorrectionService.getEquityHoldingsForAddedPosition(LICENCE_CORRECTION, positionCorrection))
        .containsEntry(1, BigDecimal.valueOf(100));
  }

  @Test
  void getCommittedTransferEquityOperations_whenNoTransferEquityChange_returnsEmpty() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    assertThat(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection)).isEmpty();
  }

  @Test
  void getCommittedTransferEquityOperations_returnsOperations() {
    var positionCorrection = positionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), true)));

    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection);

    assertThat(operations)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(transferEquityOp(1, 2, BigDecimal.valueOf(100), true));
  }

  @Test
  void addTransferEquity_appendsOperationToChanges() {
    var positionCorrection = positionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(50), true)));

    transferEquityCorrectionService.addTransferEquity(positionCorrection, transferForm("2", "3", "50"));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    var change = (AddChange) payload.changes().getFirst();

    assertThat(change.operations()).hasSize(2);
    var lastOp = (TransferEquityOperation) ((LicencePositionAddOperation) change.operations().get(1)).operation();
    assertThat(lastOp)
        .usingRecursiveComparison()
        .isEqualTo(transferEquityOp(2, 3, BigDecimal.valueOf(50), null));
  }

  @Test
  void setTransferEquityRetention_whenValidIndex_updatesRetention() {
    var positionCorrection = positionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), null)));

    transferEquityCorrectionService.setTransferEquityRetention(positionCorrection, 0, false);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    var change = (AddChange) payload.changes().getFirst();
    var op = (TransferEquityOperation) ((LicencePositionAddOperation) change.operations().getFirst()).operation();

    assertThat(op.retainBeneficialInterest()).isFalse();
  }

  @Test
  void setTransferEquityRetention_whenIndexOutOfRange_doesNothing() {
    var positionCorrection = positionCorrectionWithTransferEquity(List.of());

    transferEquityCorrectionService.setTransferEquityRetention(positionCorrection, 0, false);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void removeTransferEquity_whenValidIndex_removesOperation() {
    var positionCorrection = positionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), null)));

    transferEquityCorrectionService.removeTransferEquity(positionCorrection, 0);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.changes()).isEmpty();
  }

  @Test
  void removeTransferEquity_whenIndexOutOfRange_doesNothing() {
    var positionCorrection = positionCorrectionWithTransferEquity(List.of());

    transferEquityCorrectionService.removeTransferEquity(positionCorrection, 0);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void getTransferEquityViews_mapsOperationsToViewsWithResolvedNames() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, "Org One", 2, "Org Two"));

    var views = transferEquityCorrectionService.getTransferEquityViews(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), true)));

    assertThat(views)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(new TransferEquityHoldingView("Org One", "Org Two", BigDecimal.valueOf(100), true));
  }

  @Test
  void getTransferEquityViews_whenNamesUnknown_usesEmptyStrings() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1, 2))).thenReturn(Map.of());

    var views = transferEquityCorrectionService.getTransferEquityViews(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), true)));

    assertThat(views)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(new TransferEquityHoldingView("", "", BigDecimal.valueOf(100), true));
  }

  @Test
  void getCommittedTransferEquityOperationsForExecutedPosition_whenCorrectionExists_returnsOperations() {
    var positionCorrection = updatePositionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), true)));

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(positionCorrection));

    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(operations)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(transferEquityOp(1, 2, BigDecimal.valueOf(100), true));
  }

  @Test
  void getCommittedTransferEquityOperationsForExecutedPosition_whenNoCorrection_returnsEmptyWithoutSaving() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(operations).isEmpty();
    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void addTransferEquityForExecutedPosition_whenNoExistingCorrection_createsUpdatePositionCorrectionAndAppendsOperation() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    transferEquityCorrectionService.addTransferEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, transferForm("1", "2", "100"));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);
    var op = (TransferEquityOperation) ((LicencePositionAddOperation)
        ((AddChange) payload.changes().getFirst()).operations().getFirst()).operation();

    assertThat(op)
        .usingRecursiveComparison()
        .isEqualTo(transferEquityOp(1, 2, BigDecimal.valueOf(100), null));
  }

  @Test
  void setTransferEquityRetentionForExecutedPosition_updatesRetention() {
    var existing = updatePositionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), null)));

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    transferEquityCorrectionService.setTransferEquityRetentionForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, 0, true);

    verify(licencePositionCorrectionRepository).save(existing);
    var payload = (UpdateLicencePositionPayload) existing.getPayload();
    var op = (TransferEquityOperation) ((LicencePositionAddOperation)
        ((AddChange) payload.changes().getFirst()).operations().getFirst()).operation();

    assertThat(op.retainBeneficialInterest()).isTrue();
  }

  @Test
  void setTransferEquityRetentionForExecutedPosition_whenIndexOutOfRange_doesNothing() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    transferEquityCorrectionService.setTransferEquityRetentionForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, 0, true);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void removeTransferEquityForExecutedPosition_removesOperation() {
    var existing = updatePositionCorrectionWithTransferEquity(
        List.of(transferEquityOp(1, 2, BigDecimal.valueOf(100), null)));

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    transferEquityCorrectionService.removeTransferEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, 0);

    verify(licencePositionCorrectionRepository).save(existing);
    var payload = (UpdateLicencePositionPayload) existing.getPayload();
    assertThat(payload.changes()).isEmpty();
  }

  @Test
  void removeTransferEquityForExecutedPosition_whenIndexOutOfRange_doesNothing() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    transferEquityCorrectionService.removeTransferEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, 0);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  private LicencePositionTransferEquityForm transferForm(String from, String to, String equity) {
    var form = new LicencePositionTransferEquityForm();
    form.setTransferFrom(from);
    form.setTransferTo(to);
    form.getEquity().setInputValue(equity);
    return form;
  }

  private static TransferEquityOperation transferEquityOp(int from, int to, BigDecimal equity, Boolean retain) {
    return new TransferEquityOperation(from, to, equity, retain);
  }

  private static LicencePositionCorrection positionCorrectionWithTransferEquity(List<TransferEquityOperation> operations) {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(operations.isEmpty() ? List.of() : List.of(transferEquityChange(operations)))
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  private static LicencePositionCorrection updatePositionCorrectionWithTransferEquity(List<TransferEquityOperation> operations) {
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(operations.isEmpty() ? List.of() : List.of(transferEquityChange(operations)))
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
  }

  private static LicencePositionChangeType transferEquityChange(List<TransferEquityOperation> operations) {
    var changeOperations = operations.stream()
        .map(operation -> (LicencePositionChangeOperation) LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build())
        .toList();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(changeOperations)
        .build();
  }
}
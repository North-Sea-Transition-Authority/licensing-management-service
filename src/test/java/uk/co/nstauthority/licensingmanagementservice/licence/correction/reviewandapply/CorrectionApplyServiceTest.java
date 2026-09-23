package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@ExtendWith(MockitoExtension.class)
class CorrectionApplyServiceTest {

  private static final String CORRECTION_REFERENCE = "CORRECTION_REFERENCE";
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, Month.MARCH, 1);
  private static final LicenceOperation OPERATION =
      LicenceOperation.newAdministratorChange().withOperator(1).build();

  @Mock
  private CorrectedTimelineService correctedTimelineService;

  @Mock
  private LicencePositionValidationService licencePositionValidationService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicencePositionChangeRepository licencePositionChangeRepository;

  @Mock
  private LicenceTransactionRepository licenceTransactionRepository;

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private CorrectionApplyService correctionApplyService;

  @Captor
  private ArgumentCaptor<Object> persistedCaptor;

  private final LicenceCorrection licenceCorrection = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .withCorrectionReference(CORRECTION_REFERENCE)
      .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
      .build();

  @Test
  void applyCorrection_whenCorrectionIsNotInProgress_thenNothingIsApplied() {
    var completedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    stubLockedCorrection(completedCorrection);

    assertThatThrownBy(() -> correctionApplyService.applyCorrection(completedCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(completedCorrection.getId().toString());

    verifyNoInteractions(correctedTimelineService, licenceCorrectionService);
  }

  @Test
  void applyCorrection_whenCorrectionWasAppliedConcurrently_thenTheStatusIsReReadUnderALock() {
    var appliedConcurrently = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    when(entityManager.find(LicenceCorrection.class, licenceCorrection.getId(), LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(appliedConcurrently);

    assertThatThrownBy(() -> correctionApplyService.applyCorrection(licenceCorrection))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(correctedTimelineService, licencePositionRepository, licenceCorrectionService);
  }

  @Test
  void applyCorrection_whenTimelineHasBlockingErrors_thenNothingIsApplied() {
    var positionCorrection = removePositionCorrection(LicencePositionTestUtil.newBuilder().build());
    var blockingError = new PositionValidationError(
        UUID.randomUUID(), "1 March 2026", null, null, "Beneficial interests must total 100%");

    stubTimeline(List.of(positionCorrection), List.of(blockingError));

    var result = correctionApplyService.applyCorrection(licenceCorrection);

    assertThat(result).isEqualTo(List.of(blockingError));

    verify(entityManager, never()).persist(any());
    verifyNoInteractions(licencePositionRepository, licenceCorrectionService);
  }

  @Test
  void applyCorrection_whenPositionIsAdded_thenTheTransactionAndPositionAreInsertedWithThePayloadIds() {
    var positionId = UUID.randomUUID();
    var transactionId = UUID.randomUUID();
    var createdTransaction = LicenceTransactionTestUtil.newBuilder()
        .withId(transactionId)
        .withRegulatorReference(CORRECTION_REFERENCE)
        .build();

    stubTimeline(List.of(addPositionCorrection(positionId, transactionId, List.of())), List.of());
    when(licenceTransactionRepository.findById(transactionId)).thenReturn(Optional.empty());
    when(licenceTransactionService.createLicenceTransaction(transactionId, CORRECTION_REFERENCE))
        .thenReturn(createdTransaction);

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(entityManager).persist(persistedCaptor.capture());
    assertThat((LicencePosition) persistedCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(LicencePositionTestUtil.newBuilder()
            .withId(positionId)
            .withLicence(licenceCorrection.getLicence())
            .withLicenceTransaction(createdTransaction)
            .withPositionDate(EFFECTIVE_DATE)
            .withPositionOrder(2)
            .withStatus(LicencePositionStatus.EXECUTED)
            .build());
  }

  @Test
  void applyCorrection_whenAddedPositionReusesALiveTransaction_thenNoTransactionIsInserted() {
    var positionId = UUID.randomUUID();
    var liveTransaction = LicenceTransactionTestUtil.newBuilder().build();

    stubTimeline(List.of(addPositionCorrection(positionId, liveTransaction.getId(), List.of())), List.of());
    when(licenceTransactionRepository.findById(liveTransaction.getId())).thenReturn(Optional.of(liveTransaction));

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(entityManager).persist(persistedCaptor.capture());
    assertThat(persistedCaptor.getValue())
        .isInstanceOf(LicencePosition.class)
        .extracting("licenceTransaction")
        .isEqualTo(liveTransaction);

    verifyNoInteractions(licenceTransactionService);
  }

  @Test
  void applyCorrection_whenPositionIsUpdated_thenDateOrderAndReferenceAreWrittenBack() {
    var livePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2025, Month.JANUARY, 1))
        .withPositionOrder(1)
        .build();

    var createdTransaction = LicenceTransactionTestUtil.newBuilder()
        .withRegulatorReference(CORRECTION_REFERENCE)
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, CORRECTION_REFERENCE, List.of())), List.of());
    when(licenceTransactionService.createLicenceTransaction(CORRECTION_REFERENCE)).thenReturn(createdTransaction);

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionRepository).save(livePosition);
    assertThat(livePosition)
        .extracting(
            LicencePosition::getPositionDate,
            LicencePosition::getPositionDateOrder,
            LicencePosition::getLicenceTransaction)
        .containsExactly(EFFECTIVE_DATE, 2, createdTransaction);
  }

  @Test
  void applyCorrection_whenUpdatedPositionAlreadyCarriesTheCorrectionReference_thenTheTransactionIsKept() {
    var liveTransaction = LicenceTransactionTestUtil.newBuilder()
        .withRegulatorReference(CORRECTION_REFERENCE)
        .build();
    var livePosition = LicencePositionTestUtil.newBuilder()
        .withLicenceTransaction(liveTransaction)
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, CORRECTION_REFERENCE, List.of())), List.of());

    correctionApplyService.applyCorrection(licenceCorrection);

    verifyNoInteractions(licenceTransactionService);
    assertThat(livePosition.getLicenceTransaction()).isEqualTo(liveTransaction);
  }

  @Test
  void applyCorrection_whenPositionIsRemoved_thenItIsFlaggedRemovedAndItsChangesAreKept() {
    var livePosition = LicencePositionTestUtil.newBuilder()
        .withStatus(LicencePositionStatus.EXECUTED)
        .build();

    stubTimeline(List.of(removePositionCorrection(livePosition)), List.of());

    correctionApplyService.applyCorrection(licenceCorrection);

    assertThat(livePosition.getStatus()).isEqualTo(LicencePositionStatus.REMOVED);

    verify(licencePositionRepository).save(livePosition);
    verify(licencePositionRepository, never()).delete(livePosition);
    verifyNoInteractions(licencePositionChangeRepository);
  }

  @Test
  void applyCorrection_whenChangeIsAdded_thenItIsInsertedWithThePayloadChangeId() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID();
    var addChange = LicencePositionChangeType.addChange()
        .withChangeId(changeId.toString())
        .withChangeOrder(3)
        .withOperations(List.of(addOperation(OPERATION)))
        .build();

    stubTimeline(
        List.of(updatePositionCorrection(livePosition, null, List.of(addChange))), List.of());

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionChangeService).createLicencePositionChange(
        changeId, livePosition, List.of(OPERATION), 3, LicencePositionChangeStatus.CONSENTED);
  }

  @Test
  void applyCorrection_whenChangeOperationsAreUpdated_thenTheLiveOperationsAreOverwritten() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(livePosition)
        .withChangeOrder(4)
        .build();
    var updateChange = LicencePositionChangeType.updateChangeOperations()
        .withChangeId(liveChange.getId().toString())
        .withOperations(List.of(addOperation(OPERATION)))
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, null, List.of(updateChange))), List.of());
    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionChangeRepository).save(liveChange);
    verify(licencePositionChangeService, never())
        .createLicencePositionChange(any(), any(), any(), anyInt(), any());
    assertThat(liveChange)
        .extracting(LicencePositionChange::getOperations, LicencePositionChange::getChangeOrder)
        .containsExactly(List.of(OPERATION), 4);
  }

  @Test
  void applyCorrection_whenChangeOperationsAreUpdatedAndTheLiveChangeIsGone_thenTheChangeIsInserted() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID();
    var updateChange = LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId.toString())
        .withOperations(List.of(addOperation(OPERATION)))
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, null, List.of(updateChange))), List.of());
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.empty());
    when(licencePositionChangeService.findByLicencePositionId(livePosition.getId()))
        .thenReturn(List.of(LicencePositionChangeTestUtil.newBuilder().withChangeOrder(7).build()));

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionChangeService).createLicencePositionChange(
        changeId, livePosition, List.of(OPERATION), 8, LicencePositionChangeStatus.CONSENTED);
  }

  @Test
  void applyCorrection_whenChangeOrderIsUpdated_thenTheLiveChangeIsReordered() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder().withChangeOrder(1).build();
    var updateChangeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId(liveChange.getId().toString())
        .withChangeOrder(5)
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, null, List.of(updateChangeOrder))), List.of());
    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionChangeRepository).save(liveChange);
    assertThat(liveChange.getChangeOrder()).isEqualTo(5);
  }

  @Test
  void applyCorrection_whenChangeIsRemoved_thenTheLiveChangeIsDeleted() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(livePosition).build();
    var removeChange = LicencePositionChangeType.removeChange()
        .withChangeId(liveChange.getId().toString())
        .build();

    stubTimeline(List.of(updatePositionCorrection(livePosition, null, List.of(removeChange))), List.of());
    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));

    correctionApplyService.applyCorrection(licenceCorrection);

    verify(licencePositionChangeRepository).delete(liveChange);
  }

  @Test
  void applyCorrection_whenAPositionStagesEveryChangeType_thenRemovalsAreAppliedBeforeAdditionsThenUpdates() {
    var livePosition = LicencePositionTestUtil.newBuilder().build();
    var removedChange = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(livePosition).build();
    var reorderedChange = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(livePosition).build();
    var updatedChange = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(livePosition).build();
    var addedChangeId = UUID.randomUUID();

    List<LicencePositionChangeType> changes = List.of(
        LicencePositionChangeType.updateChangeOrder()
            .withChangeId(reorderedChange.getId().toString())
            .withChangeOrder(5)
            .build(),
        LicencePositionChangeType.updateChangeOperations()
            .withChangeId(updatedChange.getId().toString())
            .withOperations(List.of(addOperation(OPERATION)))
            .build(),
        LicencePositionChangeType.addChange()
            .withChangeId(addedChangeId.toString())
            .withChangeOrder(3)
            .withOperations(List.of(addOperation(OPERATION)))
            .build(),
        LicencePositionChangeType.removeChange()
            .withChangeId(removedChange.getId().toString())
            .build()
    );

    stubTimeline(List.of(updatePositionCorrection(livePosition, null, changes)), List.of());
    when(licencePositionChangeService.findById(removedChange.getId())).thenReturn(Optional.of(removedChange));
    when(licencePositionChangeService.findById(updatedChange.getId())).thenReturn(Optional.of(updatedChange));
    when(licencePositionChangeService.findById(reorderedChange.getId())).thenReturn(Optional.of(reorderedChange));

    correctionApplyService.applyCorrection(licenceCorrection);

    var inOrder = inOrder(licencePositionChangeRepository, licencePositionChangeService);
    inOrder.verify(licencePositionChangeRepository).delete(removedChange);
    inOrder.verify(licencePositionChangeService).createLicencePositionChange(
        addedChangeId, livePosition, List.of(OPERATION), 3, LicencePositionChangeStatus.CONSENTED);
    inOrder.verify(licencePositionChangeRepository).save(updatedChange);
    inOrder.verify(licencePositionChangeRepository).save(reorderedChange);
  }

  @Test
  void applyCorrection_thenTheCorrectionIsCompletedWithNoBlockingErrors() {
    stubTimeline(List.of(), List.of());

    var result = correctionApplyService.applyCorrection(licenceCorrection);

    assertThat(result).isEmpty();

    verify(licenceCorrectionService).completeCorrection(licenceCorrection);
  }

  private static LicencePositionChangeOperation addOperation(LicenceOperation operation) {
    return LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();
  }

  private void stubTimeline(
      List<LicencePositionCorrection> positionCorrections,
      List<PositionValidationError> blockingErrors
  ) {
    var correctedTimeline = new CorrectedTimeline(
        licenceCorrection,
        positionCorrections,
        List.of(),
        LicencePositionStateResolver.resolve(List.of(), Set.of())
    );

    stubLockedCorrection(licenceCorrection);
    when(correctedTimelineService.getCorrectedTimeline(licenceCorrection)).thenReturn(correctedTimeline);
    when(licencePositionValidationService.validate(
        correctedTimeline.positionsToApply(),
        correctedTimeline.resolvedStates(),
        correctedTimeline.isCarbonStorage()))
        .thenReturn(blockingErrors);
  }

  private void stubLockedCorrection(LicenceCorrection correction) {
    when(entityManager.find(LicenceCorrection.class, correction.getId(), LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(correction);
  }

  private LicencePositionCorrection addPositionCorrection(
      UUID positionId,
      UUID transactionId,
      List<LicencePositionChangeType> changes
  ) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(licenceCorrection)
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(positionId.toString())
            .withLicenceTransactionId(transactionId.toString())
            .withEffectiveDate(EFFECTIVE_DATE)
            .withEffectiveDateOrder(2)
            .withCorrectionReference(CORRECTION_REFERENCE)
            .withChanges(changes)
            .build())
        .build();
  }

  private LicencePositionCorrection updatePositionCorrection(
      LicencePosition targetPosition,
      String correctionReference,
      List<LicencePositionChangeType> changes
  ) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(licenceCorrection)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withEffectiveDate(EFFECTIVE_DATE)
            .withEffectiveDateOrder(2)
            .withCorrectionReference(correctionReference)
            .withChanges(changes)
            .build())
        .build();
  }

  private LicencePositionCorrection removePositionCorrection(LicencePosition targetPosition) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(licenceCorrection)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(null)
        .build();
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @InjectMocks
  private StorageOperatorChangeService storageOperatorChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  private static final Integer OPERATOR_ID = 116;
  private static final Integer UPDATED_OPERATOR_ID = 999;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .withId(POSITION_ID)
      .build();

  @Test
  void addStorageOperatorChangeForAddedLicencePosition_whenNoOperatorChangeYet_appendsOperatorChange() {
    var correction = addPositionCorrectionFor(List.of());

    storageOperatorChangeService.addStorageOperatorChangeForAddedLicencePosition(correction, OPERATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.licencePositionId()).isEqualTo(POSITION_ID.toString());
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(OPERATOR_ID);
  }

  @Test
  void addStorageOperatorChangeForAddedLicencePosition_whenOperatorChangeAlreadyExists_rewritesAddChange() {
    var correction = addPositionCorrectionFor(List.of(storageOperatorAddChange()));

    storageOperatorChangeService.addStorageOperatorChangeForAddedLicencePosition(correction, UPDATED_OPERATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(operatorIdOf(change)).isEqualTo(UPDATED_OPERATOR_ID);
  }

  @Test
  void addStorageOperatorChangeForExistingLicencePosition_whenNoExistingUpdateCorrection_createsNewCorrection() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    storageOperatorChangeService
        .addStorageOperatorChangeForExistingLicencePosition(LICENCE_POSITION, LICENCE_CORRECTION, OPERATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(OPERATOR_ID);
  }

  @Test
  void addStorageOperatorChangeForExistingLicencePosition_whenExistingUpdateCorrection_appendsOperatorChange() {
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(existing));

    storageOperatorChangeService
        .addStorageOperatorChangeForExistingLicencePosition(LICENCE_POSITION, LICENCE_CORRECTION, OPERATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(OPERATOR_ID);
  }

  @Test
  void addStorageOperatorChangeForExistingLicencePosition_whenOperatorChangeAlreadyExists_rewritesAddChange() {
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(
            LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of(storageOperatorAddChange())).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(existing));

    storageOperatorChangeService
        .addStorageOperatorChangeForExistingLicencePosition(LICENCE_POSITION, LICENCE_CORRECTION, UPDATED_OPERATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(operatorIdOf(change)).isEqualTo(UPDATED_OPERATOR_ID);
  }

  private LicencePositionCorrection addPositionCorrectionFor(List<LicencePositionChangeType> changes) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(POSITION_ID.toString())
        .withChanges(changes)
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  @Test
  void correctExistingStorageOperatorChange_whenCorrectionHasNoOperatorChange_appendsUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(correction);

    storageOperatorChangeService
        .correctExistingStorageOperatorChange(licencePosition, LICENCE_CORRECTION, OPERATOR_ID, originalChangeId);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);

    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(change)).isEqualTo(OPERATOR_ID);
  }

  @Test
  void correctExistingStorageOperatorChange_whenOperatorChangeAlreadyExists_rewritesExistingUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(storageOperatorUpdateChange(originalChangeId)))
            .build())
        .build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(correction);

    storageOperatorChangeService
        .correctExistingStorageOperatorChange(licencePosition, LICENCE_CORRECTION, UPDATED_OPERATOR_ID, originalChangeId);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);

    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(change)).isEqualTo(UPDATED_OPERATOR_ID);
  }

  @Test
  void removeExistingStorageOperatorChange_whenNoExistingUpdateCorrection_thenOnlyStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());

    storageOperatorChangeService
        .removeExistingStorageOperatorChange(licencePosition, LICENCE_CORRECTION, originalChangeId);

    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, licencePosition, originalChangeId);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void removeExistingStorageOperatorChange_whenExistingUpdateCorrectionWithOperatorChange_thenDropsChangeAndStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(storageOperatorUpdateChange(originalChangeId)))
            .build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    storageOperatorChangeService
        .removeExistingStorageOperatorChange(licencePosition, LICENCE_CORRECTION, originalChangeId);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.changes()).isEmpty();
    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, licencePosition, originalChangeId);
  }

  @Test
  void undoStorageOperatorChange_whenTheChangeStagesAnOperatorOperation_thenDropsIt() {
    var change = storageOperatorAddChange();
    var correction = givenStagedChange(change,
        LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build());

    storageOperatorChangeService.undoStorageOperatorChange(LICENCE_CORRECTION, change.changeId());

    verify(licencePositionCorrectionService).dropStagedChange(correction, change.changeId());
  }

  @Test
  void undoStorageOperatorChange_whenTheChangeRemovesALiveOperatorChange_thenDropsIt() {
    var change = removeChange(UUID.randomUUID().toString());
    var correction = givenStagedChange(change,
        LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build());

    storageOperatorChangeService.undoStorageOperatorChange(LICENCE_CORRECTION, change.changeId());

    verify(licencePositionCorrectionService).dropStagedChange(correction, change.changeId());
  }

  @Test
  void undoStorageOperatorChange_whenTheChangeIsNotAnOperatorChange_thenThrowsAndDropsNothing() {
    var changeId = UUID.randomUUID().toString();
    var change = setEquityAddChange(changeId);
    givenStagedChange(change, LicenceOperation.newSetEquityOperation()
        .withTransferTo(1).withEquity(BigDecimal.TEN).build());

    assertThatThrownBy(() -> storageOperatorChangeService.undoStorageOperatorChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).dropStagedChange(any(), any());
  }

  @Test
  void getJoiningOperatorForReinstating() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var payloadToUndo = CreateLicencePositionPayloadTestUtil
        .newBuilder()
        .withLicencePositionId(POSITION_ID.toString())
        .build();
    var otherPayload = CreateLicencePositionPayloadTestUtil
        .newBuilder()
        .withLicencePositionId(POSITION_ID.toString())
        .build();
    var positionCorrectionToUndo = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPayload(payloadToUndo)
        .build();
    var otherPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPayload(otherPayload)
        .build();

    var targetPosition1 = ChronologicalPositionTestUtil.newBuilder().build();
    var targetPosition2 = ChronologicalPositionTestUtil.newBuilder().build();
    var targetPositions = List.of(targetPosition1, targetPosition2);

    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(positionCorrectionToUndo, otherPositionCorrection));
    when(licencePositionViewService.getCorrectedChronologicalPositions(correction, List.of(otherPositionCorrection), Set.of(POSITION_ID)))
        .thenReturn(targetPositions);
    when(licencePositionViewService.getStorageOperatorChangeContext(correction, POSITION_ID, targetPositions))
        .thenReturn(new StorageOperatorChangeContext(2, 1, "Uncorrected Current Operator Name", "Uncorrected Previous Operator Name"));

    assertThat(storageOperatorChangeService.getJoiningOperatorForReinstating(correction, positionCorrectionToUndo))
        .isEqualTo("Uncorrected Current Operator Name");
  }

  @Test
  void populateAddedStorageOperatorForm_storageOperatorChangeExists_returnsPopulatedForm() {
    var context = new StorageOperatorChangeContext(OPERATOR_ID, null, "org1", "org2");
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build();
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(operation), 1)
    );
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    var expectedForm = new StorageOperatorChangeForm();
    expectedForm.getOperatorId().setInputValue(OPERATOR_ID.toString());
    assertThat(storageOperatorChangeService.populateAddedStorageOperatorForm(context, positionCorrection))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void populateAddedStorageOperatorForm_storageOperatorChangeDoesNotExists_returnsEmptyForm() {
    var context = new StorageOperatorChangeContext(null, null, "org1", "org2");
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    var expectedForm = new StorageOperatorChangeForm();
    assertThat(storageOperatorChangeService.populateAddedStorageOperatorForm(context, positionCorrection))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void populateExistingStorageOperatorForm_hasPendingStorageOperatorChange_returnsPopulatedForm() {
    var context = new StorageOperatorChangeContext(OPERATOR_ID, null, "org1", "org2");
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build();
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(operation), 1)
    );
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    var expectedForm = new StorageOperatorChangeForm();
    expectedForm.getOperatorId().setInputValue(OPERATOR_ID.toString());
    assertThat(storageOperatorChangeService.populateExistingStorageOperatorForm(context, LICENCE_CORRECTION, LICENCE_POSITION))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void populateExistingStorageOperatorForm_hasNoPendingStorageOperatorChange_returnsEmptyForm() {
    var context = new StorageOperatorChangeContext(OPERATOR_ID, null, "org1", "org2");

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    var expectedForm = new StorageOperatorChangeForm();
    assertThat(storageOperatorChangeService.populateExistingStorageOperatorForm(context, LICENCE_CORRECTION, LICENCE_POSITION))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void populateCorrectingStorageOperatorForm_returnsPopulatedForm() {
    var context = new StorageOperatorChangeContext(OPERATOR_ID, null, "org1", "org2");
    var expectedForm = new StorageOperatorChangeForm();
    expectedForm.getOperatorId().setInputValue(OPERATOR_ID.toString());
    assertThat(storageOperatorChangeService.populateCorrectingStorageOperatorForm(context))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void populateCorrectingStorageOperatorForm_noIdInContext_returnsEmptyForm() {
    var context = new StorageOperatorChangeContext(null, null, "org1", "org2");
    var expectedForm = new StorageOperatorChangeForm();
    expectedForm.getOperatorId().setInputValue(null);
    assertThat(storageOperatorChangeService.populateCorrectingStorageOperatorForm(context))
        .usingRecursiveComparison().isEqualTo(expectedForm);
  }

  private LicencePositionChangeType storageOperatorAddChange() {
    var storageOperatorOperation = LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build();
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

  private LicencePositionChangeType setEquityAddChange(String changeId) {
    var setEquityOperation = LicenceOperation.newSetEquityOperation()
        .withTransferTo(1)
        .withEquity(BigDecimal.TEN)
        .build();
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(setEquityOperation.id())
        .withOperation(setEquityOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(changeId)
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
  }

  private LicencePositionChangeType storageOperatorUpdateChange(String changeId) {
    var storageOperatorOperation = LicenceOperation.newStorageOperatorOperation().withOperator(OPERATOR_ID).build();
    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(storageOperatorOperation.id())
        .withOperation(storageOperatorOperation)
        .build();
    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId)
        .withOperations(List.of(operation))
        .build();
  }

  private static LicencePositionChangeType removeChange(String changeId) {
    return LicencePositionChangeType.removeChange().withChangeId(changeId).build();
  }

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionAddOperation) change.operations().getFirst();
    return ((StorageOperatorOperation ) op.operation()).operatorId();
  }

  private Integer updateOperatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionUpdateOperation) ((UpdateChangeOperations) change).operations().getFirst();
    return ((StorageOperatorOperation) op.operation()).operatorId();
  }
}
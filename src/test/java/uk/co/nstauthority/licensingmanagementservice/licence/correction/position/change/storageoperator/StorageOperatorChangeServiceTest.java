package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

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

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionAddOperation) change.operations().getFirst();
    return ((StorageOperatorOperation ) op.operation()).operatorId();
  }
}
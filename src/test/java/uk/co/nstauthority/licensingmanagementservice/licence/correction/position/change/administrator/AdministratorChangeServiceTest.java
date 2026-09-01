package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@ExtendWith(MockitoExtension.class)
class AdministratorChangeServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final Integer ADMINISTRATOR_ID = 116;
  private static final Integer UPDATED_ADMINISTRATOR_ID = 999;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private AdministratorChangeService administratorChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  @Test
  void addAdministratorChangeForAddedLicencePosition_whenNoAdminChangeYet_appendsAdminChange() {
    var licencePositionId = UUID.randomUUID();
    var correction = addPositionCorrectionFor(licencePositionId, List.of());

    administratorChangeService.addAdministratorChangeForAddedLicencePosition(correction, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.licencePositionId()).isEqualTo(licencePositionId.toString());
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void addAdministratorChangeForAddedLicencePosition_whenAdminChangeAlreadyExists_rewritesAddChange() {
    var licencePositionId = UUID.randomUUID();
    var correction = addPositionCorrectionFor(licencePositionId, List.of(adminAddChange()));

    administratorChangeService.addAdministratorChangeForAddedLicencePosition(correction, UPDATED_ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(operatorIdOf(change)).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  @Test
  void addAdministratorChangeForExistingLicencePosition_whenNoExistingUpdateCorrection_createsNewCorrection() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());

    administratorChangeService
        .addAdministratorChangeForExistingLicencePosition(licencePosition, LICENCE_CORRECTION, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(licencePosition);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void addAdministratorChangeForExistingLicencePosition_whenExistingUpdateCorrection_appendsAdminChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .addAdministratorChangeForExistingLicencePosition(licencePosition, LICENCE_CORRECTION, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void addAdministratorChangeForExistingLicencePosition_whenAdminChangeAlreadyExists_rewritesAddChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of(adminAddChange())).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .addAdministratorChangeForExistingLicencePosition(licencePosition, LICENCE_CORRECTION, UPDATED_ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(operatorIdOf(change)).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenNoExistingUpdateCorrection_createsNewCorrectionWithUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(originalChangeId)).thenReturn(liveAdminChange(UPDATED_ADMINISTRATOR_ID));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId.toString(), ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(licencePosition);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);

    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId.toString());
    assertThat(updateOperatorIdOf(change)).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenNoExistingCorrectionAndLiveAdminUnchanged_doesNotSave() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(originalChangeId)).thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId.toString(), ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void correctExistingAdministratorChange_whenExistingUpdateCorrectionWithoutAdminChange_appendsUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(payload.changes().getFirst().changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenAdminChangeAlreadyExists_rewritesExistingUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(adminUpdateChange(originalChangeId, ADMINISTRATOR_ID)))
            .build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, UPDATED_ADMINISTRATOR_ID);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(change)).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  @Test
  void removeExistingAdministratorChange_whenNoExistingUpdateCorrection_thenOnlyStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());

    administratorChangeService
        .removeExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId);

    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, licencePosition, originalChangeId);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void removeExistingAdministratorChange_whenExistingUpdateCorrectionWithAdminChange_thenDropsAdminChangeAndStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(adminUpdateChange(originalChangeId, ADMINISTRATOR_ID)))
            .build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .removeExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.changes()).isEmpty();
    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, licencePosition, originalChangeId);
  }

  @Test
  void undoAdministratorChange_whenUpdatePositionEmptyAndDateOrderUnchanged_deletesCorrection() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE, List.of(removeChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId)))
        .thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService).delete(correction);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void undoAdministratorChange_whenOtherChangesRemain_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var remaining = removeChange("change-2");
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE,
        List.of(removeChange(changeId), remaining));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId)))
        .thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).containsExactly(remaining);
  }

  @Test
  void undoAdministratorChange_whenUpdatePositionEmptyButDateChanged_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .build();
    var payload = new UpdateLicencePositionPayload(
        LocalDate.of(2026, Month.MARCH, 1), null, CORRECTION_REFERENCE, List.of(removeChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId)))
        .thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).isEmpty();
  }

  @Test
  void undoAdministratorChange_whenAddPositionEmpty_savesWithoutDelete() {
    var changeId = UUID.randomUUID().toString();
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(UUID.randomUUID().toString())
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withChanges(List.of(removeChange(changeId)))
        .build();
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId)))
        .thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload()).isInstanceOf(CreateLicencePositionPayload.class);
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).isEmpty();
  }

  @Test
  void undoAdministratorChange_whenRemoveChangeReferencesNonAdminLiveChange_throwsAndDoesNotModify() {
    var changeId = UUID.randomUUID().toString();
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE, List.of(removeChange(changeId)));
    var correction = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .thenReturn(correction);
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId)))
        .thenReturn(liveSetEquityChange());

    assertThatThrownBy(() -> administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void undoAdministratorChange_whenTargetChangeIsNonAdminAddChange_throwsAndDoesNotModify() {
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

    assertThatThrownBy(() -> administratorChangeService.undoAdministratorChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionChangeService, never()).getByIdOrThrow(any());
    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void hasPendingAdministratorChange_whenUpdateCorrectionContainsAdminChange_returnsTrue() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of(adminAddChange())).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    assertThat(administratorChangeService.hasPendingAdministratorChange(licencePosition, LICENCE_CORRECTION)).isTrue();
  }

  @Test
  void hasPendingAdministratorChange_whenNoUpdateCorrection_returnsFalse() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());

    assertThat(administratorChangeService.hasPendingAdministratorChange(licencePosition, LICENCE_CORRECTION)).isFalse();
  }

  private LicencePositionChange liveAdminChange(Integer administratorId) {
    var change = new LicencePositionChange();
    change.setOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(administratorId).build()));
    return change;
  }

  private LicencePositionChange liveSetEquityChange() {
    var change = new LicencePositionChange();
    change.setOperations(List.of(
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(BigDecimal.TEN).build()));
    return change;
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

  private LicencePositionChangeType adminAddChange() {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build();
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

  private LicencePositionChangeType adminUpdateChange(String changeId, Integer administratorId) {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(administratorId).build();
    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();
    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId)
        .withOperations(List.of(operation))
        .build();
  }

  private LicencePositionCorrection addPositionCorrectionFor(UUID positionId, List<LicencePositionChangeType> changes) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(positionId.toString())
        .withChanges(changes)
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  private static LicencePositionChangeType removeChange(String changeId) {
    return LicencePositionChangeType.removeChange().withChangeId(changeId).build();
  }

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionAddOperation) ((AddChange) change).operations().getFirst();
    return ((AdministratorOperation) op.operation()).operatorId();
  }

  private Integer updateOperatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionUpdateOperation) ((UpdateChangeOperations) change).operations().getFirst();
    return ((AdministratorOperation) op.operation()).operatorId();
  }
}

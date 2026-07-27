package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder().build();
  private static final Integer ADMINISTRATOR_ID = 116;

  @Mock
  private LicencePositionCorrectionRepository licencePositionCorrectionRepository;

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @InjectMocks
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;


  @Test
  void addNewPosition_whenNoExistingPositions_savesAddPositionCorrectionWithOrderOne() {
    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.ADD_POSITION);
    assertThat(saved.getTargetLicencePosition()).isNull();

    assertThat(saved.getPayload()).isInstanceOf(CreateLicencePositionPayload.class);
    var payload = (CreateLicencePositionPayload) saved.getPayload();
    assertThat(payload.effectiveDate()).isEqualTo(POSITION_DATE);
    assertThat(payload.effectiveDateOrder()).isEqualTo(1);
    assertThat(payload.correctionReference()).isEqualTo(CORRECTION_REFERENCE);
    assertThat(payload.changes()).isEmpty();
    assertThat(payload.licencePositionId()).isNotNull();
    assertThat(payload.licenceTransactionId()).isNotNull();
    assertThat(payload.licencePositionId()).isNotEqualTo(payload.licenceTransactionId());
  }

  @Test
  void addNewPosition_whenLivePositionsExist_setsOrderFromLiveMax() {
    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, POSITION_DATE)).thenReturn(3);

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(4);
  }

  @Test
  void addNewPosition_whenDraftPositionsExistForSameDate_setsOrderFromDraftMax() {
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(
            draftCorrectionWith(POSITION_DATE, 1),
            draftCorrectionWith(POSITION_DATE, 2)
        ));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(3);
  }

  @Test
  void addNewPosition_whenDraftPositionForDifferentDate_isExcludedFromOrder() {
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(draftCorrectionWith(POSITION_DATE.plusDays(1), 9)));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(1);
  }

  @Test
  void addNewPosition_whenLiveAndDraftExist_usesGreaterOfTheTwo() {
    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, POSITION_DATE)).thenReturn(2);
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(draftCorrectionWith(POSITION_DATE, 5)));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(6);
  }

  @Test
  void getPositionCorrectionForCorrection_whenFound_returnsCorrection() {
    var positionCorrectionId = UUID.randomUUID();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(positionCorrectionId)
        .build();

    when(licencePositionCorrectionRepository.findByIdAndLicenceCorrection(positionCorrectionId, LICENCE_CORRECTION))
        .thenReturn(Optional.of(positionCorrection));

    assertThat(licencePositionCorrectionService
        .getPositionCorrectionForCorrection(positionCorrectionId, LICENCE_CORRECTION))
        .isEqualTo(positionCorrection);
  }

  @Test
  void getPositionCorrectionForCorrection_whenNotFound_throws() {
    var positionCorrectionId = UUID.randomUUID();

    when(licencePositionCorrectionRepository.findByIdAndLicenceCorrection(positionCorrectionId, LICENCE_CORRECTION))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> licencePositionCorrectionService
        .getPositionCorrectionForCorrection(positionCorrectionId, LICENCE_CORRECTION))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void undoPositionCorrection_deletesCorrection() {
    licencePositionCorrectionService.undoPositionCorrection(POSITION_CORRECTION);
    verify(licencePositionCorrectionRepository).delete(POSITION_CORRECTION);
  }

  @Test
  void getPositionCorrections_returnsAllCorrectionsForLicenceCorrectionInSingleQuery() {
    var addedCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION).build();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION).build();
    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION).build();

    when(licencePositionCorrectionRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(addedCorrection, updateCorrection, removeCorrection));

    assertThat(licencePositionCorrectionService.getPositionCorrections(LICENCE_CORRECTION))
        .containsExactly(addedCorrection, updateCorrection, removeCorrection);
  }

  @Test
  void getAddedLicencePositionCorrections_returnsAddPositionCorrectionsFromRepository() {
    var addedCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(addedCorrection));

    assertThat(licencePositionCorrectionService.getAddedLicencePositionCorrections(LICENCE_CORRECTION))
        .containsExactly(addedCorrection);
  }

  @ParameterizedTest
  @ValueSource(strings = {"REF-1", "ref-1"})
  void isCorrectionReferenceInUse_whenReferenceMatchesExistingIgnoringCase_returnsTrue(String correctionReference) {
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(addedCorrectionWithReference("REF-1")));

    assertThat(licencePositionCorrectionService.isCorrectionReferenceInUse(LICENCE_CORRECTION, correctionReference))
        .isTrue();
  }

  @Test
  void isCorrectionReferenceInUse_whenReferenceDoesNotMatchAnyExisting_returnsFalse() {
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(addedCorrectionWithReference("REF-1")));

    assertThat(licencePositionCorrectionService.isCorrectionReferenceInUse(LICENCE_CORRECTION, "REF-2")).isFalse();
  }

  @Test
  void removeExecutedPosition_savesRemovePositionCorrectionWithNoPayload() {
    licencePositionCorrectionService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.REMOVE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(saved.getPayload()).isNull();
  }

  @Test
  void removeExecutedPosition_whenUpdateCorrectionExists_deletesUpdateCorrectionFirst() {
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(updateCorrection));

    licencePositionCorrectionService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionRepository).delete(updateCorrection);
    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getChangeType())
        .isEqualTo(LicencePositionCorrectionChangeType.REMOVE_POSITION);
  }

  @Test
  void removeExecutedPosition_whenNoUpdateCorrectionExists_doesNotDelete() {
    licencePositionCorrectionService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionRepository, never()).delete(any());
  }

  @Test
  void removeExecutedPosition_whenPositionAlreadyTargetedByRemoveCorrection_throwsAndDoesNotSave() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThatThrownBy(() ->
        licencePositionCorrectionService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isInstanceOf(IllegalStateException.class);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void canRemovePosition_whenPositionNotAlreadyTargetedByRemoveCorrection_returnsTrue() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThat(licencePositionCorrectionService.canRemovePosition(LICENCE_CORRECTION, LICENCE_POSITION)).isTrue();
  }

  @Test
  void canRemovePosition_whenPositionAlreadyTargetedByRemoveCorrection_returnsFalse() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThat(licencePositionCorrectionService.canRemovePosition(LICENCE_CORRECTION, LICENCE_POSITION)).isFalse();
  }

  @Test
  void canRemovePosition_whenPositionNotExecuted_returnsFalse() {
    var nonExecutedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withIsExecuted(false)
        .build();

    assertThat(licencePositionCorrectionService.canRemovePosition(LICENCE_CORRECTION, nonExecutedPosition)).isFalse();
  }

  @Test
  void reinstateDeletedPositionCorrection_whenMarkedForRemoval_deletesRemoveCorrections() {
    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(true);
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(Optional.of(removeCorrection));

    licencePositionCorrectionService.reinstateDeletedPositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionRepository).delete(removeCorrection);
  }

  @Test
  void reinstateDeletedPositionCorrection_whenNotMarkedForRemoval_throwsAndDoesNotDelete() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThatThrownBy(() ->
        licencePositionCorrectionService.reinstateDeletedPositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .isInstanceOf(IllegalStateException.class);

    verify(licencePositionCorrectionRepository, never()).delete(any());
  }

  @Test
  void canReinstateDeletedPositionCorrection_whenPositionTargetedByRemoveCorrection_returnsTrue() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThat(licencePositionCorrectionService
        .canReinstateDeletedPositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION)).isTrue();
  }

  @Test
  void canReinstateDeletedPositionCorrection_whenPositionNotTargetedByRemoveCorrection_returnsFalse() {
    when(licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThat(licencePositionCorrectionService
        .canReinstateDeletedPositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION)).isFalse();
  }

  @Test
  void correctPositionDate_whenNoExistingUpdateCorrection_savesNewUpdateCorrection() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionCorrectionService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);

    assertThat(saved.getPayload()).isInstanceOf(UpdateLicencePositionPayload.class);
    var payload = (UpdateLicencePositionPayload) saved.getPayload();

    var expectedPayload = new UpdateLicencePositionPayload(
        newDate, 1, LICENCE_CORRECTION.getCorrectionReference(), List.of()
    );
    assertThat(payload).usingRecursiveComparison().isEqualTo(expectedPayload);
  }

  @Test
  void correctPositionDate_whenExistingUpdateCorrection_updatesItInPlace() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(2026, Month.JANUARY, 1))
            .withEffectiveDateOrder(9)
            .build())
        .build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved).isSameAs(existing);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.effectiveDate()).isEqualTo(newDate);
  }

  @Test
  void correctPositionDate_whenExistingUpdateCorrectionHasChanges_preservesChangesAndReference() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    var existingChange = LicencePositionChangeType.addChange()
        .withChangeId("change-id")
        .build();
    var existingPayload = new UpdateLicencePositionPayload(
        LocalDate.of(2026, Month.JANUARY, 1), 9, "REF-9", List.of(existingChange));

    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(existingPayload)
        .build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.effectiveDate()).isEqualTo(newDate);
    assertThat(payload.correctionReference()).isEqualTo("REF-9");
    assertThat(payload.changes()).containsExactly(existingChange);
  }

  @Test
  void correctPositionDate_setsEffectiveDateOrderFromLiveMaxOnTargetDate() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, newDate)).thenReturn(4);

    licencePositionCorrectionService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.effectiveDateOrder()).isEqualTo(5);
  }

  @Test
  void addAdministratorChangeForAddedLicencePosition_whenNoAdminChangeYet_appendsAdminChange() {
    var licencePositionId = UUID.randomUUID();
    var correction = addPositionCorrectionFor(licencePositionId, List.of());

    licencePositionCorrectionService
        .addAdministratorChangeForAddedLicencePosition(correction, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.licencePositionId()).isEqualTo(licencePositionId.toString());
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void addAdministratorChangeForExistingLicencePosition_whenNoExistingUpdateCorrection_createsNewCorrection() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionCorrectionService
        .addAdministratorChangeForExistingLicencePosition(licencePosition, LICENCE_CORRECTION, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
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

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService
        .addAdministratorChangeForExistingLicencePosition(licencePosition, LICENCE_CORRECTION, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(operatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenNoExistingUpdateCorrection_createsNewCorrectionWithUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionCorrectionService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(licencePosition);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);

    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(((UpdateChangeOperations) change).changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(change)).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenExistingUpdateCorrectionWithoutAdminChange_appendsUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(((UpdateChangeOperations) payload.changes().getFirst()).changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenAdminChangeAlreadyExists_doesNotSave() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(adminAddChange()))
            .build())
        .build();

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, UUID.randomUUID().toString(), ADMINISTRATOR_ID);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void adminChangeExists_whenNoChanges_returnsFalse() {
    assertThat(licencePositionCorrectionService.adminChangeExists(List.of())).isFalse();
  }

  @Test
  void adminChangeExists_whenAddChangeContainsAdministratorOperation_returnsTrue() {
    assertThat(licencePositionCorrectionService.adminChangeExists(List.of(adminAddChange()))).isTrue();
  }

  @Test
  void adminChangeExists_whenUpdateChangeContainsAdministratorOperation_returnsTrue() {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build();
    var updateOperation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();
    var updateChange = LicencePositionChangeType.updateChangeOperations()
        .withChangeId(UUID.randomUUID().toString())
        .withOperations(List.of(updateOperation))
        .build();

    assertThat(licencePositionCorrectionService.adminChangeExists(List.of(updateChange))).isTrue();
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

  private Integer updateOperatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionUpdateOperation) ((UpdateChangeOperations) change).operations().getFirst();
    return ((AdministratorOperation) op.operation()).operatorId();
  }

  private LicencePositionCorrection addPositionCorrectionFor(UUID positionId, List<LicencePositionChangeType> changes) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(positionId.toString())
        .withChanges(changes)
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
  }

  private Integer operatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionAddOperation) ((AddChange) change).operations().getFirst();
    return ((AdministratorOperation) op.operation()).operatorId();
  }

  private CreateLicencePositionPayload captureSavedPayload() {
    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(CreateLicencePositionPayload.class);
    return (CreateLicencePositionPayload) payload;
  }

  private LicencePositionCorrection addedCorrectionWithReference(String correctionReference) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withCorrectionReference(correctionReference)
        .build();

    return LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(payload)
        .build();
  }

  private LicencePositionCorrection draftCorrectionWith(LocalDate effectiveDate, int effectiveDateOrder) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .build();

    return LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(payload)
        .build();
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

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
  private static final String MOVED_CHANGE_ID = UUID.randomUUID().toString();

  @Mock
  private LicencePositionCorrectionRepository licencePositionCorrectionRepository;

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;


  @Test
  void addNewPosition_whenNoExistingPositions_savesAddPositionCorrectionWithOrderOne() {
    var newPositionId =
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
    assertThat(newPositionId).hasToString(payload.licencePositionId());
  }

  @Test
  void addNewPosition_whenGivenATransactionId_thenThePayloadUsesIt() {
    var transactionId = UUID.randomUUID();

    licencePositionCorrectionService.addNewPosition(
        LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE, transactionId);

    assertThat(captureSavedPayload().licenceTransactionId()).isEqualTo(transactionId.toString());
  }

  @Test
  void addNewPosition_whenLivePositionsExist_setsOrderFromLiveMax() {
    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 3, "REF-LIVE"));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(4);
  }

  @Test
  void addNewPosition_whenDraftPositionsExistForSameDate_setsOrderFromDraftMax() {
    givenPositionCorrections(
        draftCorrectionWith(POSITION_DATE, 1),
        draftCorrectionWith(POSITION_DATE, 2)
    );

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(3);
  }

  @Test
  void addNewPosition_whenDraftPositionForDifferentDate_isExcludedFromOrder() {
    givenPositionCorrections(draftCorrectionWith(POSITION_DATE.plusDays(1), 9));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(1);
  }

  @Test
  void addNewPosition_whenLiveAndDraftExist_usesGreaterOfTheTwo() {
    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 2, "REF-LIVE"));
    givenPositionCorrections(draftCorrectionWith(POSITION_DATE, 5));

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(6);
  }

  @Test
  void addNewPosition_whenExistingPositionRelocatedOntoDateByUpdate_countsItSoOrderDoesNotCollide() {
    var relocated = executedPosition(UUID.randomUUID(), POSITION_DATE.minusMonths(1), 1, "REF-RELOCATED");
    var relocateOntoDate = updateCorrectionFor(
        relocated,
        UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(POSITION_DATE)
            .withEffectiveDateOrder(1)
            .build()
    );

    givenExecutedPositions(relocated);
    givenPositionCorrections(relocateOntoDate);

    licencePositionCorrectionService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(2);
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
  void getPositionCorrectionContainingChange_whenFound_returnsCorrection() {
    var changeId = UUID.randomUUID().toString();
    var match = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId).build()))
            .build())
        .build();
    var other = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(other, match));

    assertThat(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .isEqualTo(match);
  }

  @Test
  void getPositionCorrectionContainingChange_whenACorrectionHasNoPayload_thenThatCorrectionIsSkipped() {
    var changeId = UUID.randomUUID().toString();
    var removedPosition = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withPayload(null)
        .build();
    var match = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId).build()))
            .build())
        .build();

    when(licencePositionCorrectionRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(removedPosition, match));

    assertThat(licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, changeId))
        .isEqualTo(match);
  }

  @Test
  void getPositionCorrectionContainingChange_whenNotFound_throws() {
    when(licencePositionCorrectionRepository.findByLicenceCorrection(LICENCE_CORRECTION)).thenReturn(List.of());

    assertThatThrownBy(() ->
        licencePositionCorrectionService.getPositionCorrectionContainingChange(LICENCE_CORRECTION, "missing"))
        .isInstanceOf(LmsEntityNotFoundException.class);
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
  void getRemovedPositionIds_returnsOnlyTheTargetsOfRemovePositionCorrections() {
    var removedPosition = LicencePositionTestUtil.newBuilder().build();
    var updatedPosition = LicencePositionTestUtil.newBuilder().build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removedPosition)
        .withPayload(null)
        .build();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .build();
    var addCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .build();

    var result = LicencePositionCorrectionService.getRemovedPositionIds(
        List.of(removeCorrection, updateCorrection, addCorrection));

    assertThat(result).containsExactly(removedPosition.getId());
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

  @Test
  void findFirstAddedPositionCorrection_whenPositionWasAddedInCorrection_returnsThatCorrection() {
    var licencePositionId = UUID.randomUUID();
    var addedCorrection = addedCorrectionForPosition(licencePositionId);

    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(addedCorrectionForPosition(UUID.randomUUID()), addedCorrection));

    assertThat(licencePositionCorrectionService.findFirstAddedPositionCorrection(LICENCE_CORRECTION, licencePositionId))
        .contains(addedCorrection);
  }

  @Test
  void findFirstAddedPositionCorrection_whenPositionNotAddedInCorrection_isEmpty() {
    when(licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionCorrectionChangeType.ADD_POSITION))
        .thenReturn(List.of(addedCorrectionForPosition(UUID.randomUUID())));

    assertThat(licencePositionCorrectionService.findFirstAddedPositionCorrection(LICENCE_CORRECTION, UUID.randomUUID()))
        .isEmpty();
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
  void removeExecutedPosition_whenPositionNotExecuted_throwsAndDoesNotSave() {
    var nonExecutedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicencePositionStatus.SUBMITTED)
        .build();

    assertThatThrownBy(() ->
        licencePositionCorrectionService.removeExecutedPosition(LICENCE_CORRECTION, nonExecutedPosition))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("is not executed");

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
        .withStatus(LicencePositionStatus.SUBMITTED)
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
    givenExecutedPositions(executedPosition(UUID.randomUUID(), newDate, 4, "REF-EXISTING"));

    licencePositionCorrectionService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.effectiveDateOrder()).isEqualTo(5);
  }


  @Test
  void getOrderableSameDatePositions_whenPositionNotFound_returnsEmpty() {
    assertThat(licencePositionCorrectionService
        .getOrderableSameDatePositions(LICENCE_CORRECTION, UUID.randomUUID()))
        .isEmpty();
  }

  @Test
  void getOrderableSameDatePositions_returnsOnlySameDatePositionsSortedByOrder() {
    var movedId = UUID.randomUUID();
    var sameDateEarlierId = UUID.randomUUID();
    var otherDateId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(movedId, POSITION_DATE, 2, "REF-MOVED"),
        executedPosition(sameDateEarlierId, POSITION_DATE, 1, "REF-EARLIER"),
        executedPosition(otherDateId, POSITION_DATE.plusDays(1), 1, "REF-OTHER-DATE"));

    assertThat(licencePositionCorrectionService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
        .containsExactly(
            new OrderablePosition(sameDateEarlierId, POSITION_DATE, 1, "REF-EARLIER", false),
            new OrderablePosition(movedId, POSITION_DATE, 2, "REF-MOVED", false));
  }

  @Test
  void getOrderableSameDatePositions_appliesUpdateCorrectionOverrides() {
    var movedId = UUID.randomUUID();
    var otherId = UUID.randomUUID();

    var moved = executedPosition(movedId, POSITION_DATE, 1, "LIVE-REF");
    var other = executedPosition(otherId, POSITION_DATE, 2, "REF-OTHER");

    var updatePayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE)
        .withEffectiveDateOrder(5)
        .withCorrectionReference("CORRECTED-REF")
        .build();

    givenExecutedPositions(moved, other);
    givenPositionCorrections(updateCorrectionFor(moved, updatePayload));

    assertThat(licencePositionCorrectionService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
        .containsExactly(
            new OrderablePosition(otherId, POSITION_DATE, 2, "REF-OTHER", false),
            new OrderablePosition(movedId, POSITION_DATE, 5, "CORRECTED-REF", false));
  }

  @Test
  void getOrderableSameDatePositions_excludesPositionsMarkedForRemoval() {
    var movedId = UUID.randomUUID();
    var removedId = UUID.randomUUID();

    var moved = executedPosition(movedId, POSITION_DATE, 1, "REF-MOVED");
    var removed = executedPosition(removedId, POSITION_DATE, 2, "REF-REMOVED");

    givenExecutedPositions(moved, removed);
    givenPositionCorrections(removeCorrectionFor(removed));

    assertThat(licencePositionCorrectionService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
        .containsExactly(new OrderablePosition(movedId, POSITION_DATE, 1, "REF-MOVED", false));
  }

  @Test
  void getOrderableSameDatePositions_includesAddedPositions() {
    var addedId = UUID.randomUUID();
    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(addedId.toString())
        .withEffectiveDate(POSITION_DATE)
        .withEffectiveDateOrder(1)
        .withCorrectionReference("ADD-REF")
        .build();

    givenPositionCorrections(addCorrectionFor(addPayload));

    assertThat(licencePositionCorrectionService.getOrderableSameDatePositions(LICENCE_CORRECTION, addedId))
        .containsExactly(new OrderablePosition(addedId, POSITION_DATE, 1, "ADD-REF", true));
  }

  @Test
  void getOrderablePositionsOnDate_returnsOnlyPositionsOnThatDateInOrderLeavingOutRemovedOnes() {
    var secondId = UUID.randomUUID();
    var firstId = UUID.randomUUID();
    var removed = executedPosition(UUID.randomUUID(), POSITION_DATE, 3, "REF-REMOVED");
    var addedId = UUID.randomUUID();

    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(addedId.toString())
        .withEffectiveDate(POSITION_DATE)
        .withEffectiveDateOrder(4)
        .withCorrectionReference("ADD-REF")
        .build();

    givenExecutedPositions(
        executedPosition(secondId, POSITION_DATE, 2, "REF-SECOND"),
        executedPosition(UUID.randomUUID(), POSITION_DATE.plusDays(1), 1, "REF-OTHER-DATE"),
        executedPosition(firstId, POSITION_DATE, 1, "REF-FIRST"),
        removed);
    givenPositionCorrections(removeCorrectionFor(removed), addCorrectionFor(addPayload));

    assertThat(licencePositionCorrectionService.getOrderablePositionsOnDate(LICENCE_CORRECTION, POSITION_DATE))
        .containsExactly(
            new OrderablePosition(firstId, POSITION_DATE, 1, "REF-FIRST", false),
            new OrderablePosition(secondId, POSITION_DATE, 2, "REF-SECOND", false),
            new OrderablePosition(addedId, POSITION_DATE, 4, "ADD-REF", true));
  }

  @Test
  void findSameTransactionPositionOnDate_whenAnExecutedSourceHasASameTransactionPositionOnTheDate_thenFindsIt() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var sourceId = UUID.randomUUID();
    var matchId = UUID.randomUUID();
    var targetDate = POSITION_DATE.plusDays(10);

    givenExecutedPositions(
        executedPosition(sourceId, POSITION_DATE, 1, transaction),
        executedPosition(matchId, targetDate, 2, transaction));

    var lookup = licencePositionCorrectionService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, targetDate);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transaction.getId(), matchId));
  }

  @Test
  void findSameTransactionPositionOnDate_whenTheDateIsTheSourcesOwnDate_thenDoesNotMatchTheSource() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var sourceId = UUID.randomUUID();

    givenExecutedPositions(executedPosition(sourceId, POSITION_DATE, 1, transaction));

    var lookup = licencePositionCorrectionService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, POSITION_DATE);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transaction.getId(), null));
  }

  @Test
  void findSameTransactionPositionOnDate_whenAnotherSameTransactionPositionIsOnTheSourcesDate_thenFindsIt() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var sourceId = UUID.randomUUID();
    var matchId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(sourceId, POSITION_DATE, 1, transaction),
        executedPosition(matchId, POSITION_DATE, 2, transaction));

    var lookup = licencePositionCorrectionService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, POSITION_DATE);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transaction.getId(), matchId));
  }

  @Test
  void findSameTransactionPositionOnDate_whenSameTransactionIsOnAnotherDateAndAnotherTransactionIsOnTheDate_thenFindsNothing() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var sourceId = UUID.randomUUID();
    var targetDate = POSITION_DATE.plusDays(10);

    givenExecutedPositions(
        executedPosition(sourceId, POSITION_DATE, 1, transaction),
        executedPosition(UUID.randomUUID(), targetDate.plusDays(1), 1, transaction),
        executedPosition(UUID.randomUUID(), targetDate, 1, LicenceTransactionTestUtil.newBuilder().build()));

    var lookup = licencePositionCorrectionService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, targetDate);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transaction.getId(), null));
  }

  @Test
  void findSameTransactionPositionOnDate_whenTheSourceWasAddedInTheCorrection_thenUsesItsPayloadTransactionId() {
    var transactionId = UUID.randomUUID();
    var sourceId = UUID.randomUUID();
    var matchId = UUID.randomUUID();
    var targetDate = POSITION_DATE.plusDays(10);

    var sourcePayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(sourceId.toString())
        .withLicenceTransactionId(transactionId.toString())
        .withEffectiveDate(POSITION_DATE)
        .build();
    var matchPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(matchId.toString())
        .withLicenceTransactionId(transactionId.toString())
        .withEffectiveDate(targetDate)
        .build();

    givenPositionCorrections(addCorrectionFor(sourcePayload), addCorrectionFor(matchPayload));

    var lookup = licencePositionCorrectionService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, targetDate);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transactionId, matchId));
  }

  @Test
  void getOrderableDatePositions_returnsPositionsOnEveryDate() {
    var firstDateId = UUID.randomUUID();
    var secondDateId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(firstDateId, POSITION_DATE, 1, "REF-FIRST"),
        executedPosition(secondDateId, POSITION_DATE.plusDays(1), 1, "REF-SECOND"));

    assertThat(licencePositionCorrectionService.getOrderableDatePositions(LICENCE_CORRECTION))
        .containsExactly(
            new OrderablePosition(firstDateId, POSITION_DATE, 1, "REF-FIRST", false),
            new OrderablePosition(secondDateId, POSITION_DATE.plusDays(1), 1, "REF-SECOND", false));
  }

  @Test
  void getOrderableDatePositions_excludesRemovedPositionsAndIncludesAddedPositions() {
    var keptId = UUID.randomUUID();
    var removedId = UUID.randomUUID();
    var addedId = UUID.randomUUID();

    var kept = executedPosition(keptId, POSITION_DATE, 1, "REF-KEPT");
    var removed = executedPosition(removedId, POSITION_DATE.plusDays(1), 1, "REF-REMOVED");

    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(addedId.toString())
        .withEffectiveDate(POSITION_DATE.plusDays(2))
        .withEffectiveDateOrder(1)
        .withCorrectionReference("ADD-REF")
        .build();

    givenExecutedPositions(kept, removed);
    givenPositionCorrections(removeCorrectionFor(removed), addCorrectionFor(addPayload));

    assertThat(licencePositionCorrectionService.getOrderableDatePositions(LICENCE_CORRECTION))
        .containsExactly(
            new OrderablePosition(keptId, POSITION_DATE, 1, "REF-KEPT", false),
            new OrderablePosition(addedId, POSITION_DATE.plusDays(2), 1, "ADD-REF", true));
  }

  @Test
  void getOrderableDatePositions_sortsByDateThenOrder() {
    var laterDateId = UUID.randomUUID();
    var secondOnDateId = UUID.randomUUID();
    var firstOnDateId = UUID.randomUUID();
    var addedId = UUID.randomUUID();

    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(addedId.toString())
        .withEffectiveDate(POSITION_DATE.plusDays(1))
        .withEffectiveDateOrder(1)
        .withCorrectionReference("ADD-REF")
        .build();

    givenExecutedPositions(
        executedPosition(laterDateId, POSITION_DATE.plusDays(2), 1, "REF-LATER"),
        executedPosition(secondOnDateId, POSITION_DATE, 2, "REF-SECOND"),
        executedPosition(firstOnDateId, POSITION_DATE, 1, "REF-FIRST")
    );
    givenPositionCorrections(addCorrectionFor(addPayload));

    assertThat(licencePositionCorrectionService.getOrderableDatePositions(LICENCE_CORRECTION))
        .containsExactly(
            new OrderablePosition(firstOnDateId, POSITION_DATE, 1, "REF-FIRST", false),
            new OrderablePosition(secondOnDateId, POSITION_DATE, 2, "REF-SECOND", false),
            new OrderablePosition(addedId, POSITION_DATE.plusDays(1), 1, "ADD-REF", true),
            new OrderablePosition(laterDateId, POSITION_DATE.plusDays(2), 1, "REF-LATER", false)
        );
  }

  @Test
  void getOrderableDatePosition_whenPositionFound_thenReturnsPosition() {
    var positionId = UUID.randomUUID();
    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(positionId.toString())
        .withEffectiveDate(POSITION_DATE)
        .withEffectiveDateOrder(1)
        .withCorrectionReference("ADD-REF")
        .build();

    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 2, "REF-OTHER"));
    givenPositionCorrections(addCorrectionFor(addPayload));

    assertThat(licencePositionCorrectionService.getOrderableDatePosition(LICENCE_CORRECTION, positionId))
        .isEqualTo(new OrderablePosition(positionId, POSITION_DATE, 1, "ADD-REF", true));
  }

  @Test
  void getOrderableDatePosition_whenPositionNotFound_thenThrows() {
    var positionId = UUID.randomUUID();

    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 1, "REF-OTHER"));

    assertThatThrownBy(() -> licencePositionCorrectionService
        .getOrderableDatePosition(LICENCE_CORRECTION, positionId))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getOrderableDatePositionsExcluding() {
    var excludedId = UUID.randomUUID();
    var laterDateId = UUID.randomUUID();
    var secondOnDateId = UUID.randomUUID();
    var firstOnDateId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(laterDateId, POSITION_DATE.plusDays(1), 1, "REF-LATER"),
        executedPosition(excludedId, POSITION_DATE, 3, "REF-EXCLUDED"),
        executedPosition(secondOnDateId, POSITION_DATE, 2, "REF-SECOND"),
        executedPosition(firstOnDateId, POSITION_DATE, 1, "REF-FIRST")
    );

    assertThat(licencePositionCorrectionService.getOrderableDatePositionsExcluding(LICENCE_CORRECTION, excludedId))
        .containsExactly(
            new OrderablePosition(firstOnDateId, POSITION_DATE, 1, "REF-FIRST", false),
            new OrderablePosition(secondOnDateId, POSITION_DATE, 2, "REF-SECOND", false),
            new OrderablePosition(laterDateId, POSITION_DATE.plusDays(1), 1, "REF-LATER", false)
        );
  }

  @Test
  void correctPositionOrder_whenMovedPositionNotOnSameDate_throwsAndWritesNothing() {
    var movedId = UUID.randomUUID();
    var targetId = UUID.randomUUID();

    assertThatThrownBy(() -> licencePositionCorrectionService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, targetId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not orderable on the same date");

    verify(licencePositionCorrectionRepository, never()).save(any());
    verify(licencePositionCorrectionRepository, never()).delete(any());
  }

  @Test
  void correctPositionOrder_whenTargetIsSameAsMovedPosition_throws() {
    var movedId = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    givenExecutedPositions(
        executedPosition(movedId, POSITION_DATE, 1, "REF-A"),
        executedPosition(otherId, POSITION_DATE, 2, "REF-B"));

    assertThatThrownBy(() -> licencePositionCorrectionService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, movedId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void correctPositionOrder_whenTargetPositionNotOnSameDate_throws() {
    var movedId = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    var unknownTargetId = UUID.randomUUID();
    givenExecutedPositions(
        executedPosition(movedId, POSITION_DATE, 1, "REF-A"),
        executedPosition(otherId, POSITION_DATE, 2, "REF-B"));

    assertThatThrownBy(() -> licencePositionCorrectionService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, unknownTargetId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void correctPositionOrder_movingExecutedPositionToEnd_writesNewOrderForAffectedPositions() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();
    var cId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(aId, POSITION_DATE, 1, "REF-A"),
        executedPosition(bId, POSITION_DATE, 2, "REF-B"),
        executedPosition(cId, POSITION_DATE, 3, "REF-C"));

    licencePositionCorrectionService.correctPositionOrder(LICENCE_CORRECTION, aId, cId, PositionMoveDirection.AFTER);

    verify(licencePositionCorrectionRepository, times(3)).save(licencePositionCorrectionCaptor.capture());

    var savedPayloadsByTargetId = licencePositionCorrectionCaptor.getAllValues().stream()
        .collect(Collectors.toMap(
            correction -> correction.getTargetLicencePosition().getId(),
            correction -> (UpdateLicencePositionPayload) correction.getPayload()));

    assertThat(savedPayloadsByTargetId)
        .usingRecursiveComparison()
        .isEqualTo(Map.of(
            bId, new UpdateLicencePositionPayload(POSITION_DATE, 1, null, List.of()),
            cId, new UpdateLicencePositionPayload(POSITION_DATE, 2, null, List.of()),
            aId, new UpdateLicencePositionPayload(POSITION_DATE, 3, null, List.of())));
    assertThat(licencePositionCorrectionCaptor.getAllValues())
        .allSatisfy(correction -> assertThat(correction.getChangeType())
            .isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION));
  }

  @Test
  void correctPositionOrder_whenPositionKeepsLiveOrderAndHasNoExistingCorrection_isNotSaved() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();
    var cId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(aId, POSITION_DATE, 1, "REF-A"),
        executedPosition(bId, POSITION_DATE, 2, "REF-B"),
        executedPosition(cId, POSITION_DATE, 3, "REF-C"));

    licencePositionCorrectionService.correctPositionOrder(LICENCE_CORRECTION, bId, cId, PositionMoveDirection.AFTER);

    verify(licencePositionCorrectionRepository, times(2)).save(licencePositionCorrectionCaptor.capture());

    assertThat(licencePositionCorrectionCaptor.getAllValues())
        .extracting(correction -> correction.getTargetLicencePosition().getId())
        .containsExactlyInAnyOrder(cId, bId)
        .doesNotContain(aId);
  }

  @Test
  void correctPositionOrder_whenMoveRestoresLiveOrder_deletesRedundantUpdateCorrections() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();

    var positionA = executedPosition(aId, POSITION_DATE, 1, "REF-A");
    var positionB = executedPosition(bId, POSITION_DATE, 2, "REF-B");

    var updateA = updateCorrectionFor(positionA, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(2).withCorrectionReference(null).build());
    var updateB = updateCorrectionFor(positionB, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(1).withCorrectionReference(null).build());

    givenExecutedPositions(positionA, positionB);
    givenPositionCorrections(updateA, updateB);

    licencePositionCorrectionService.correctPositionOrder(LICENCE_CORRECTION, aId, bId, PositionMoveDirection.BEFORE);

    verify(licencePositionCorrectionRepository).delete(updateA);
    verify(licencePositionCorrectionRepository).delete(updateB);
    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void correctPositionOrder_whenExistingCorrectionCarriesReference_updatesOrderAndPreservesReference() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();
    var cId = UUID.randomUUID();

    var positionA = executedPosition(aId, POSITION_DATE, 1, "REF-A");
    var positionB = executedPosition(bId, POSITION_DATE, 2, "REF-B");
    var positionC = executedPosition(cId, POSITION_DATE, 3, "REF-C");

    var updateA = updateCorrectionFor(positionA, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(1).withCorrectionReference("KEEP-REF").build());

    givenExecutedPositions(positionA, positionB, positionC);
    givenPositionCorrections(updateA);

    licencePositionCorrectionService.correctPositionOrder(LICENCE_CORRECTION, cId, aId, PositionMoveDirection.BEFORE);

    verify(licencePositionCorrectionRepository, never()).delete(any());
    verify(licencePositionCorrectionRepository, times(3)).save(licencePositionCorrectionCaptor.capture());

    var savedForA = licencePositionCorrectionCaptor.getAllValues().stream()
        .filter(correction -> aId.equals(correction.getTargetLicencePosition().getId()))
        .findFirst()
        .orElseThrow();
    assertThat(savedForA.getPayload())
        .usingRecursiveComparison()
        .isEqualTo(new UpdateLicencePositionPayload(POSITION_DATE, 2, "KEEP-REF", List.of()));
  }

  @Test
  void correctPositionOrder_movingAddedPosition_updatesAddedPayloadOrder() {
    var executedId = UUID.randomUUID();
    var addedId = UUID.randomUUID();

    var executed = executedPosition(executedId, POSITION_DATE, 1, "REF-EXECUTED");

    var addPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(addedId.toString())
        .withEffectiveDate(POSITION_DATE)
        .withEffectiveDateOrder(2)
        .withCorrectionReference("ADD-REF")
        .build();

    givenExecutedPositions(executed);
    givenPositionCorrections(addCorrectionFor(addPayload));

    licencePositionCorrectionService
        .correctPositionOrder(LICENCE_CORRECTION, addedId, executedId, PositionMoveDirection.BEFORE);

    verify(licencePositionCorrectionRepository, times(2)).save(licencePositionCorrectionCaptor.capture());

    var savedAdded = licencePositionCorrectionCaptor.getAllValues().stream()
        .filter(correction -> correction.getPayload() instanceof CreateLicencePositionPayload)
        .findFirst()
        .orElseThrow();
    var expectedAddedPayload = new CreateLicencePositionPayload(
        addPayload.licencePositionId(),
        addPayload.licenceTransactionId(),
        addPayload.effectiveDate(),
        1,
        addPayload.correctionReference(),
        addPayload.changes());
    assertThat(savedAdded.getPayload())
        .usingRecursiveComparison()
        .isEqualTo(expectedAddedPayload);
  }

  @Test
  void correctPositionOrder_loadsPositionsAndCorrectionsWithoutDuplicateQueries() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(aId, POSITION_DATE, 1, "REF-A"),
        executedPosition(bId, POSITION_DATE, 2, "REF-B"));

    licencePositionCorrectionService.correctPositionOrder(LICENCE_CORRECTION, aId, bId, PositionMoveDirection.AFTER);

    verify(licencePositionRepository, times(1)).findByLicence(LICENCE);
    verify(licencePositionCorrectionRepository, times(1)).findByLicenceCorrection(LICENCE_CORRECTION);
    verify(licencePositionCorrectionRepository, never())
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(any(), any(), any());
  }

  @Test
  void getAddOperationsOfType_whenNoMatchingChange_returnsEmpty() {
    assertThat(licencePositionCorrectionService.getAddOperationsOfType(List.of(), SetEquityOperation.class))
        .isEmpty();
  }

  @Test
  void getAddOperationsOfType_returnsTheOperationsFromMatchingAddChanges() {
    var changes = List.of(setEquityChange(List.of(setEquityOp(1, 40), setEquityOp(2, 60))));

    assertThat(licencePositionCorrectionService.getAddOperationsOfType(changes, SetEquityOperation.class))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1, 2);
  }

  @Test
  void replaceAddChangeFor_whenNoExistingChange_createsOneWithTheOperations() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
  }

  @Test
  void replaceAddChangeFor_whenExistingChange_replacesItsOperations() {
    var positionCorrection = positionCorrectionWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, SetEquityOperation.class, List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    var change = (AddChange) payload.changes().getFirst();
    assertThat(payload.changes()).hasSize(1);
    assertThat(change.operations()).hasSize(2);
  }

  @Test
  void replaceAddChangeFor_whenNoOperations_dropsTheChange() {
    var positionCorrection = positionCorrectionWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionCorrectionService.replaceAddChangeFor(positionCorrection, SetEquityOperation.class, List.of());

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).isEmpty();
  }

  @Test
  void replaceAddChangeFor_retainsOtherChangeTypes() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    // seed an unrelated change type (administrator change) on the same position
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(ADMINISTRATOR_ID)
        .build();
    positionCorrection.setPayload(LicencePositionPayload.withChanges(
        positionCorrection.getPayload(),
        List.of(AddChange.buildOperationsChange(List.of(administratorOperation), 1))));

    licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    assertThat(payload.changes()).hasSize(2);
    assertThat(licencePositionCorrectionService.getAddOperationsOfType(payload.changes(), SetEquityOperation.class))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1);
  }

  private CreateLicencePositionPayload captureSavedPayload() {
    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(CreateLicencePositionPayload.class);
    return (CreateLicencePositionPayload) payload;
  }

  private LicencePositionCorrection addedCorrectionForPosition(UUID licencePositionId) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(licencePositionId.toString())
            .build())
        .build();
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
        .withLicencePositionId(UUID.randomUUID().toString())
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .build();

    return LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(payload)
        .build();
  }

  private static SetEquityOperation setEquityOp(int transferTo, int equity) {
    return new SetEquityOperation(transferTo, BigDecimal.valueOf(equity));
  }

  private static LicencePositionCorrection positionCorrectionWithSetEquity(
      List<SetEquityOperation> operations) {
    var changeOperations = operations.stream()
        .map(operation -> (LicencePositionChangeOperation) LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build())
        .toList();
    var change = LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(changeOperations)
        .build();
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(List.of(change))
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  @Test
  void getOrBuildUpdatePositionCorrection_whenExistingUpdateCorrection_returnsExistingWithoutSaving() {
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    assertThat(licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .isSameAs(existing);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void getOrBuildUpdatePositionCorrection_whenNoExistingCorrection_buildsNewUpdateCorrectionWithoutSaving() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    var result = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(result.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(result.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(result.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(result.getPayload()).isInstanceOf(UpdateLicencePositionPayload.class);

    var payload = (UpdateLicencePositionPayload) result.getPayload();
    assertThat(payload.correctionReference()).isEqualTo(LICENCE_CORRECTION.getCorrectionReference());
    assertThat(payload.changes()).isEmpty();

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void stageRemovalOfExecutedChange_whenNothingIsStagedAgainstTheChange_thenStagesARemoveChange() {
    var changeId = UUID.randomUUID().toString();
    var otherChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    givenUpdatePositionCorrection(List.of(otherChange));

    licencePositionCorrectionService.stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, changeId);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).containsExactly(
        otherChange,
        LicencePositionChangeType.removeChange().withChangeId(changeId).build());
  }

  @Test
  void stageRemovalOfExecutedChange_whenTheChangeIsAlreadyCorrected_thenReplacesTheCorrectionWithARemoveChange() {
    var changeId = UUID.randomUUID().toString();
    var otherChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    givenUpdatePositionCorrection(List.of(
        UpdateChangeOperations.buildUpdateChange(changeId,
            LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()),
        otherChange));

    licencePositionCorrectionService.stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, changeId);

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue().getPayload().changes()).containsExactly(
        otherChange,
        LicencePositionChangeType.removeChange().withChangeId(changeId).build());
  }

  private void givenUpdatePositionCorrection(List<LicencePositionChangeType> changes) {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
  }

  @Test
  void replaceAddChangeFor_whenUpdatePayload_rebuildsAsUpdatePayloadPreservingType() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(UpdateLicencePositionPayload.class);
    assertThat(payload.changes()).hasSize(1);
  }

  private static LicencePositionChangeType setEquityChange(List<SetEquityOperation> operations) {
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

  private LicencePosition executedPosition(UUID id, LocalDate positionDate, int order, String reference) {
    return executedPosition(
        id, positionDate, order, LicenceTransactionTestUtil.newBuilder().withRegulatorReference(reference).build());
  }

  private LicencePosition executedPosition(
      UUID id,
      LocalDate positionDate,
      int order,
      LicenceTransaction licenceTransaction
  ) {
    return LicencePositionTestUtil.newBuilder()
        .withId(id)
        .withLicence(LICENCE)
        .withPositionDate(positionDate)
        .withPositionOrder(order)
        .withLicenceTransaction(licenceTransaction)
        .build();
  }

  private void givenExecutedPositions(LicencePosition... positions) {
    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of(positions));
  }

  private void givenPositionCorrections(LicencePositionCorrection... corrections) {
    when(licencePositionCorrectionRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(corrections));
  }

  private LicencePositionCorrection updateCorrectionFor(LicencePosition target, UpdateLicencePositionPayload payload) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(target)
        .withPayload(payload)
        .build();
  }

  private LicencePositionCorrection removeCorrectionFor(LicencePosition target) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(target)
        .build();
  }

  private LicencePositionCorrection addCorrectionFor(CreateLicencePositionPayload payload) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(payload)
        .build();
  }

  @Test
  void resolveEffectiveDate_whenAddedPosition_returnsThePayloadEffectiveDate() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(POSITION_DATE)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).isEqualTo(POSITION_DATE);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithCorrectedDate_returnsTheCorrectedDate() {
    var correctedDate = POSITION_DATE.plusMonths(1);
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withPositionDate(POSITION_DATE).build())
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(correctedDate)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).isEqualTo(correctedDate);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithNoCorrectedDate_returnsThePositionDate() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withPositionDate(POSITION_DATE).build())
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(null)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).isEqualTo(POSITION_DATE);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithNoCorrectedDateAndNoTarget_thenThrows() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(null)
            .withChanges(List.of())
            .build())
        .build();

    assertThatThrownBy(() -> licencePositionCorrectionService.resolveEffectiveDate(positionCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(("Cannot resolve the effective date of licence position correction %s as it updates a position "
            + "but has no target").formatted(positionCorrection.getId()));
  }

  @Test
  void getEffectivePositionDate_whenNoUpdatePositionCorrection_returnsThePositionDate() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    assertThat(licencePositionCorrectionService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(POSITION_DATE);
  }

  @Test
  void getEffectivePositionDate_whenDateCorrectedInThisCorrection_returnsTheCorrectedDate() {
    var correctedDate = POSITION_DATE.plusMonths(1);
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionCorrectionTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(correctedDate)
                .withChanges(List.of())
                .build())
            .build()));

    assertThat(licencePositionCorrectionService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(correctedDate);
  }

  @Test
  void getEffectivePositionDate_whenUpdatePositionCorrectionHasNoDate_returnsThePositionDate() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionCorrectionTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(null)
                .withChanges(List.of())
                .build())
            .build()));

    assertThat(licencePositionCorrectionService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(POSITION_DATE);
  }

  @Test
  void getCommittedChangeOfType_whenNoPositionCorrection_returnsEmpty() {
    assertThat(licencePositionCorrectionService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .isEmpty();
  }

  @Test
  void getCommittedChangeOfType_whenChangeOfTypeStaged_returnsTheOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(operation), 1)))
            .build())
        .build();

    assertThat(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, PartialSurrenderOperation.class))
        .contains(operation);
  }

  @Test
  void getCommittedChangeOfType_whenNoChangeOfType_returnsEmpty() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    assertThat(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, PartialSurrenderOperation.class))
        .isEmpty();
  }

  @Test
  void getAddOperationsOfType_ignoresOperationsStagedAsCorrections() {
    var added = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(added), 1),
        UpdateChangeOperations.buildUpdateChange(
            UUID.randomUUID().toString(),
            LicenceOperation.newPartialSurrenderOperation()
                .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
                .build()));

    assertThat(licencePositionCorrectionService.getAddOperationsOfType(changes, PartialSurrenderOperation.class))
        .containsExactly(added);
  }

  @Test
  void getChangesForExecutedPosition_whenNoUpdateCorrection_foldsLiveChangesOnly() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionCorrectionService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null
    );

    assertThat(result).containsExactly(
        new PositionChange(liveChange.getId().toString(), 1, null, List.of(operation)));
  }

  @Test
  void getChangesForExecutedPosition_whenUpdateCorrectionPresent_foldsLiveAndStagedChanges() {
    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(liveOperation))
        .build();

    var stagedOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedChange = AddChange.buildOperationsChange(List.of(stagedOperation), 2);
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(stagedChange))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionCorrectionService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateCorrection
    );

    assertThat(result).containsExactly(
        new PositionChange(liveChange.getId().toString(), 1, null, List.of(liveOperation)),
        new PositionChange(stagedChange.changeId(), 2, LicencePositionChangeType.ADD_CHANGE, List.of(stagedOperation)));
  }

  @Test
  void getChangesForAddedPosition_foldsAddedCorrectionChanges() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var addChange = AddChange.buildOperationsChange(List.of(operation), 1);
    var addedCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(addChange))
            .build())
        .build();

    var result = licencePositionCorrectionService.getChangesForAddedPosition(addedCorrection);

    assertThat(result).containsExactly(
        new PositionChange(addChange.changeId(), 1, LicencePositionChangeType.ADD_CHANGE, List.of(operation)));
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForExecutedPosition_collectsFeatureIdsFromLiveAndStagedChanges() {
    var liveFeatureId = UUID.randomUUID();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(liveFeatureId, List.of(), List.of())))
        .build();

    var stagedFeatureId = UUID.randomUUID();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(new SubareaOperation(stagedFeatureId, List.of(), List.of())), 2)))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionCorrectionService
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION, updateCorrection);

    assertThat(result).containsExactlyInAnyOrder(liveFeatureId, stagedFeatureId);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForExecutedPosition_whenChangeStagedForRemoval_excludesItsFeatureIds() {
    var liveFeatureId = UUID.randomUUID();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(liveFeatureId, List.of(), List.of())))
        .build();

    var removalCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(LicencePositionChangeType.removeChange()
                .withChangeId(liveChange.getId().toString())
                .build()))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionCorrectionService
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION, removalCorrection);

    assertThat(result).isEmpty();
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForExecutedPosition_whenChangeIdExcluded_omitsThatChangesFeatureIds() {
    var excludedFeatureId = UUID.randomUUID();
    var excludedChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(excludedFeatureId, List.of(), List.of())))
        .build();

    var retainedFeatureId = UUID.randomUUID();
    var retainedChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(2)
        .withOperations(List.of(new SubareaOperation(retainedFeatureId, List.of(), List.of())))
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(excludedChange, retainedChange));

    var result = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null,
        excludedChange.getId().toString()
    );

    assertThat(result).containsExactly(retainedFeatureId);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForAddedPosition_collectsFeatureIdsAcrossOperationTypes() {
    var subareaFeatureId = UUID.randomUUID();
    var surrenderFeatureIdOne = UUID.randomUUID();
    var surrenderFeatureIdTwo = UUID.randomUUID();
    var addedCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(
                AddChange.buildOperationsChange(List.of(new SubareaOperation(subareaFeatureId, List.of(), List.of())), 1),
                AddChange.buildOperationsChange(List.of(
                    new PartialSurrenderOperation(
                        null, List.of(surrenderFeatureIdOne, surrenderFeatureIdTwo), null)), 2)))
            .build())
        .build();

    var result = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(addedCorrection);

    assertThat(result).containsExactlyInAnyOrder(subareaFeatureId, surrenderFeatureIdOne, surrenderFeatureIdTwo);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForAddedPosition_whenChangeIdExcluded_omitsThatChangesFeatureIds() {
    var excludedFeatureId = UUID.randomUUID();
    var excludedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(excludedFeatureId, List.of(), List.of())), 1);
    var retainedFeatureId = UUID.randomUUID();
    var retainedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(retainedFeatureId, List.of(), List.of())), 2);
    var addedCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(excludedChange, retainedChange))
            .build())
        .build();

    var result = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        addedCorrection, excludedChange.changeId());

    assertThat(result).containsExactly(retainedFeatureId);
  }

  @Test
  void getStagedChangeOrThrow_whenTheChangeIsStaged_thenReturnsIt() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionCorrection = updatePositionCorrectionWith(List.of(change));

    var result = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId());

    assertThat(result).isEqualTo(change);
  }

  @Test
  void getStagedChangeOrThrow_whenOnlyAChangeOrderIsStagedForThatId_thenThrows() {
    var changeId = UUID.randomUUID().toString();
    var positionCorrection = updatePositionCorrectionWith(List.of(
        LicencePositionChangeType.updateChangeOrder().withChangeId(changeId).withChangeOrder(2).build()));

    assertThatThrownBy(() -> licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);
  }

  @Test
  void getStagedChangeOrThrow_whenTheChangeIsNotStaged_thenThrows() {
    var changeId = UUID.randomUUID().toString();
    var positionCorrection = updatePositionCorrectionWith(List.of());

    assertThatThrownBy(() -> licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);
  }

  @Test
  void resolveStagedChangeOperations_whenTheChangeStagesItsOwnOperations_thenReturnsThem() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var change = AddChange.buildOperationsChange(List.of(operation), 1);

    var result = licencePositionCorrectionService.resolveStagedChangeOperations(change);

    assertThat(result).containsExactly(operation);
  }

  @Test
  void resolveStagedChangeOperations_whenTheChangeRemovesALiveChange_thenReturnsTheLiveOperations() {
    var liveChangeId = UUID.randomUUID();
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    when(licencePositionChangeService.getByIdOrThrow(liveChangeId))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(liveChangeId)
            .withOperations(List.of(operation))
            .build());

    var result = licencePositionCorrectionService.resolveStagedChangeOperations(
        LicencePositionChangeType.removeChange().withChangeId(liveChangeId.toString()).build());

    assertThat(result).containsExactly(operation);
  }

  @Test
  void dropStagedChange_whenTheCorrectionIsLeftEmptyAndTheDateAndOrderAreUnchanged_thenDeletesTheCorrection() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionCorrection = updatePositionCorrectionWith(List.of(change));

    licencePositionCorrectionService.dropStagedChange(positionCorrection, change.changeId());

    verify(licencePositionCorrectionRepository).delete(positionCorrection);
    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void dropStagedChange_whenOtherChangesRemain_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var retainedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 2);
    var positionCorrection = updatePositionCorrectionWith(List.of(change, retainedChange));

    licencePositionCorrectionService.dropStagedChange(positionCorrection, change.changeId());

    verify(licencePositionCorrectionRepository, never()).delete(any());
    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChange_whenAChangeOrderIsStagedForTheSameChange_thenThatIsKeptAndTheCorrectionSaved() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var changeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId(change.changeId()).withChangeOrder(2).build();
    var positionCorrection = updatePositionCorrectionWith(List.of(change, changeOrder));

    licencePositionCorrectionService.dropStagedChange(positionCorrection, change.changeId());

    verify(licencePositionCorrectionRepository, never()).delete(any());
    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(changeOrder);
  }

  @Test
  void dropStagedChange_whenThePositionDateWasAlsoCorrected_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LICENCE_POSITION.getPositionDate().plusMonths(1))
            .withEffectiveDateOrder(LICENCE_POSITION.getPositionDateOrder())
            .withChanges(List.of(change))
            .build())
        .build();

    licencePositionCorrectionService.dropStagedChange(positionCorrection, change.changeId());

    verify(licencePositionCorrectionRepository, never()).delete(any());
    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).isEmpty();
  }

  @Test
  void dropStagedChange_whenTheCorrectionAddsThePosition_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of(change)).build())
        .build();

    licencePositionCorrectionService.dropStagedChange(positionCorrection, change.changeId());

    verify(licencePositionCorrectionRepository, never()).delete(any());
    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).isEmpty();
  }

  @Test
  void dropStagedChange_whenALiveChangeWasMovedOntoThisPosition_thenItsChangeOrderIsDroppedToo() {
    var movedLiveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LicencePositionTestUtil.newBuilder().build())
        .build();
    var movedChangeId = movedLiveChange.getId().toString();
    var retainedChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        1
    );
    var positionCorrection = updatePositionCorrectionWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            movedChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        LicencePositionChangeType.updateChangeOrder().withChangeId(movedChangeId).withChangeOrder(2).build(),
        retainedChange
    ));

    when(licencePositionChangeService.findById(movedLiveChange.getId())).thenReturn(Optional.of(movedLiveChange));

    licencePositionCorrectionService.dropStagedChange(positionCorrection, movedChangeId);

    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChange_whenALiveChangeOnThisPositionWasCorrected_thenItsChangeOrderIsKept() {
    var liveChange = liveChangeWithOrder(1);
    var liveChangeId = liveChange.getId().toString();
    var changeOrder = LicencePositionChangeType.updateChangeOrder().withChangeId(liveChangeId).withChangeOrder(2).build();
    var positionCorrection = updatePositionCorrectionWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            liveChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        changeOrder
    ));

    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));

    licencePositionCorrectionService.dropStagedChange(positionCorrection, liveChangeId);

    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(changeOrder);
  }

  @Test
  void dropStagedChangeAndOrder_whenOtherChangesRemain_thenDropsTheChangeAndItsOrder() {
    var liveChange = liveChangeWithOrder(1);
    var liveChangeId = liveChange.getId().toString();
    var retainedChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        2
    );
    var positionCorrection = updatePositionCorrectionWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            liveChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        LicencePositionChangeType.updateChangeOrder().withChangeId(liveChangeId).withChangeOrder(3).build(),
        retainedChange
    ));

    licencePositionCorrectionService.dropStagedChangeAndOrder(positionCorrection, liveChangeId);

    verify(licencePositionCorrectionRepository).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChangeAndOrder_whenTheCorrectionWasOnlyBuilt_thenNeitherSavesNorDeletesIt() {
    var builtCorrection =
        licencePositionCorrectionService.newUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    licencePositionCorrectionService.dropStagedChangeAndOrder(builtCorrection, UUID.randomUUID().toString());

    verify(licencePositionCorrectionRepository, never()).save(any());
    verify(licencePositionCorrectionRepository, never()).delete(any());
  }

  @Test
  void stageChangesOnPosition_whenTargetWasAddedInTheCorrection_thenAppendsToItsChanges() {
    var targetPositionId = UUID.randomUUID();
    var existingChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        1
    );
    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId(targetPositionId.toString())
        .withChanges(List.of(existingChange))
        .build();
    var addedCorrection = addCorrectionFor(addedPayload);

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionCorrectionChangeType.ADD_POSITION
    )).thenReturn(List.of(addedCorrection));

    licencePositionCorrectionService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        targetPositionId,
        List.of(movedChange(2))
    );

    verify(licencePositionCorrectionRepository).save(addedCorrection);
    assertThat(addedCorrection.getPayload())
        .isEqualTo(LicencePositionPayload.withChanges(addedPayload, List.of(existingChange, movedChange(2))));
  }

  @Test
  void stageChangesOnPosition_whenExecutedTargetHasNoUpdateCorrection_thenBuildsOne() {
    givenExecutedTargetPosition(Optional.empty());

    licencePositionCorrectionService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION.getId(),
        List.of(movedChange(3))
    );

    var expected = new LicencePositionCorrection();
    expected.setLicenceCorrection(LICENCE_CORRECTION);
    expected.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    expected.setTargetLicencePosition(LICENCE_POSITION);
    expected.setPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
        .withCorrectionReference(LICENCE_CORRECTION.getCorrectionReference())
        .withChanges(List.of(movedChange(3)))
        .build());

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    assertThat(licencePositionCorrectionCaptor.getValue()).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void stageChangesOnPosition_whenTargetPositionIsNotOnTheLicence_thenThrowsAndSavesNothing() {
    var targetPositionId = UUID.randomUUID();

    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionCorrectionChangeType.ADD_POSITION
    )).thenReturn(List.of());
    when(licencePositionRepository.findByIdAndLicence(targetPositionId, LICENCE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licencePositionCorrectionService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        targetPositionId,
        List.of(movedChange(1))
    )).isInstanceOf(LmsEntityNotFoundException.class);

    verify(licencePositionCorrectionRepository, never()).save(any());
  }

  @Test
  void getUpdatedChangePositionIds() {
    var addedPositionId = UUID.randomUUID();
    var updatedOnExecutedPosition = UpdateChangeOperations.buildUpdateChange(
        UUID.randomUUID().toString(),
        new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
    );
    var updatedOnAddedPosition = UpdateChangeOperations.buildUpdateChange(
        UUID.randomUUID().toString(),
        new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
    );
    var addChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        1
    );

    var result = LicencePositionCorrectionService.getUpdatedChangePositionIds(List.of(
        updateCorrectionFor(
            LICENCE_POSITION,
            UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withChanges(List.of(updatedOnExecutedPosition))
                .build()
        ),
        addCorrectionFor(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(addedPositionId.toString())
            .withChanges(List.of(updatedOnAddedPosition, addChange))
            .build()),
        removeCorrectionFor(LicencePositionTestUtil.newBuilder().build())
    ));

    assertThat(result).isEqualTo(Map.of(
        updatedOnExecutedPosition.changeId(), LICENCE_POSITION.getId(),
        updatedOnAddedPosition.changeId(), addedPositionId
    ));
  }

  @Test
  void withoutMovedAwayChanges() {
    var unstagedChange = liveChangeWithOrder(1);
    var changeUpdatedInPlace = liveChangeWithOrder(2);
    var movedAwayChange = liveChangeWithOrder(3);

    var result = LicencePositionCorrectionService.withoutMovedAwayChanges(
        List.of(unstagedChange, changeUpdatedInPlace, movedAwayChange),
        LICENCE_POSITION.getId(),
        Map.of(
            changeUpdatedInPlace.getId().toString(), LICENCE_POSITION.getId(),
            movedAwayChange.getId().toString(), UUID.randomUUID()
        )
    );

    assertThat(result).containsExactly(unstagedChange, changeUpdatedInPlace);
  }

  @Test
  void getChangesForExecutedPosition_whenALiveChangeIsStagedOnAnotherPosition_thenLeavesItOut() {
    var remainingOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var remainingChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(remainingOperation))
        .build();
    var movedAwayChange = liveChangeWithOrder(2);
    var otherPositionCorrection = addCorrectionFor(CreateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(List.of(UpdateChangeOperations.buildUpdateChange(
            movedAwayChange.getId().toString(),
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        )))
        .build());

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(remainingChange, movedAwayChange));
    givenPositionCorrections(otherPositionCorrection);

    var result = licencePositionCorrectionService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null
    );

    assertThat(result).containsExactly(
        new PositionChange(remainingChange.getId().toString(), 1, null, List.of(remainingOperation))
    );
  }

  private void givenExecutedTargetPosition(Optional<LicencePositionCorrection> updateCorrection) {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionCorrectionChangeType.ADD_POSITION
    )).thenReturn(List.of());
    when(licencePositionRepository.findByIdAndLicence(LICENCE_POSITION.getId(), LICENCE))
        .thenReturn(Optional.of(LICENCE_POSITION));
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        LicencePositionCorrectionChangeType.UPDATE_POSITION
    )).thenReturn(updateCorrection);
  }

  private static LicencePositionChange liveChangeWithOrder(int changeOrder) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(changeOrder)
        .withOperations(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())))
        .build();
  }

  private static AddChange movedChange(int changeOrder) {
    return LicencePositionChangeType.addChange()
        .withChangeId(MOVED_CHANGE_ID)
        .withChangeOrder(changeOrder)
        .withOperations(List.of())
        .build();
  }

  private static LicencePositionCorrection updatePositionCorrectionWith(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }
}
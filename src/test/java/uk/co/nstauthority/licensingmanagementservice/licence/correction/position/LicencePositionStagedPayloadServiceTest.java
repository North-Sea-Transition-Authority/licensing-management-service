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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

@ExtendWith(MockitoExtension.class)
class LicencePositionStagedPayloadServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder().build();
  private static final Integer ADMINISTRATOR_ID = 116;
  private static final String MOVED_CHANGE_ID = UUID.randomUUID().toString();

  @Mock
  private LicencePositionStagedPayloadRepository licencePositionStagedPayloadRepository;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionService licencePositionService;

  @InjectMocks
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Captor
  private ArgumentCaptor<LicencePositionStagedPayload> licencePositionStagedPayloadCaptor;


  @Test
  void addNewPosition_whenNoExistingPositions_savesAddPositionStagedPayloadWithOrderOne() {
    var newPositionId =
        licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var saved = licencePositionStagedPayloadCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.ADD_POSITION);
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

    licencePositionStagedPayloadService.addNewPosition(
        LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE, transactionId);

    assertThat(captureSavedPayload().licenceTransactionId()).isEqualTo(transactionId.toString());
  }

  @Test
  void addNewPosition_whenLivePositionsExist_setsOrderFromLiveMax() {
    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 3, "REF-LIVE"));

    licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(4);
  }

  @Test
  void addNewPosition_whenDraftPositionsExistForSameDate_setsOrderFromDraftMax() {
    givenPositionStagedPayloads(
        draftStagedPayloadWith(POSITION_DATE, 1),
        draftStagedPayloadWith(POSITION_DATE, 2)
    );

    licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(3);
  }

  @Test
  void addNewPosition_whenDraftPositionForDifferentDate_isExcludedFromOrder() {
    givenPositionStagedPayloads(draftStagedPayloadWith(POSITION_DATE.plusDays(1), 9));

    licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(1);
  }

  @Test
  void addNewPosition_whenLiveAndDraftExist_usesGreaterOfTheTwo() {
    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 2, "REF-LIVE"));
    givenPositionStagedPayloads(draftStagedPayloadWith(POSITION_DATE, 5));

    licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(6);
  }

  @Test
  void addNewPosition_whenExistingPositionRelocatedOntoDateByUpdate_countsItSoOrderDoesNotCollide() {
    var relocated = executedPosition(UUID.randomUUID(), POSITION_DATE.minusMonths(1), 1, "REF-RELOCATED");
    var relocateOntoDate = updateStagedPayloadFor(
        relocated,
        UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(POSITION_DATE)
            .withEffectiveDateOrder(1)
            .build()
    );

    givenExecutedPositions(relocated);
    givenPositionStagedPayloads(relocateOntoDate);

    licencePositionStagedPayloadService.addNewPosition(LICENCE_CORRECTION, POSITION_DATE, CORRECTION_REFERENCE);

    assertThat(captureSavedPayload().effectiveDateOrder()).isEqualTo(2);
  }

  @Test
  void undoPositionStagedPayload_deletesPositionStagedPayload() {
    licencePositionStagedPayloadService.undoPositionStagedPayload(POSITION_STAGED_PAYLOAD);
    verify(licencePositionStagedPayloadRepository).delete(POSITION_STAGED_PAYLOAD);
  }

  @Test
  void getPositionStagedPayloadContainingChange_whenFound_returnsPositionStagedPayload() {
    var changeId = UUID.randomUUID().toString();
    var match = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId).build()))
            .build())
        .build();
    var other = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(other, match));

    assertThat(licencePositionStagedPayloadService.getPositionStagedPayloadContainingChange(LICENCE_CORRECTION, changeId))
        .isEqualTo(match);
  }

  @Test
  void getPositionStagedPayloadContainingChange_whenAPositionStagedPayloadHasNoPayload_thenItIsSkipped() {
    var changeId = UUID.randomUUID().toString();
    var removedPosition = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withPayload(null)
        .build();
    var match = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId).build()))
            .build())
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(removedPosition, match));

    assertThat(licencePositionStagedPayloadService.getPositionStagedPayloadContainingChange(LICENCE_CORRECTION, changeId))
        .isEqualTo(match);
  }

  @Test
  void getPositionStagedPayloadContainingChange_whenNotFound_throws() {
    when(licencePositionStagedPayloadRepository.findByLicenceCorrection(LICENCE_CORRECTION)).thenReturn(List.of());

    assertThatThrownBy(() ->
        licencePositionStagedPayloadService.getPositionStagedPayloadContainingChange(LICENCE_CORRECTION, "missing"))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getPositionStagedPayloads_returnsAllPositionStagedPayloadsForLicenceCorrectionInSingleQuery() {
    var addedStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION).build();
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION).build();
    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION).build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(addedStagedPayload, updateStagedPayload, removeStagedPayload));

    assertThat(licencePositionStagedPayloadService.getPositionStagedPayloads(LICENCE_CORRECTION))
        .containsExactly(addedStagedPayload, updateStagedPayload, removeStagedPayload);
  }

  @Test
  void getRemovedPositionIds_returnsOnlyTheTargetsOfRemovePositionStagedPayloads() {
    var removedPosition = LicencePositionTestUtil.newBuilder().build();
    var updatedPosition = LicencePositionTestUtil.newBuilder().build();

    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removedPosition)
        .withPayload(null)
        .build();
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .build();
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .build();

    var result = LicencePositionStagedPayloadService.getRemovedPositionIds(
        List.of(removeStagedPayload, updateStagedPayload, addStagedPayload));

    assertThat(result).containsExactly(removedPosition.getId());
  }

  @Test
  void getAddedLicencePositionStagedPayloads_returnsAddPositionStagedPayloadsFromRepository() {
    var addedStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().build();

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionStagedPayloadChangeType.ADD_POSITION))
        .thenReturn(List.of(addedStagedPayload));

    assertThat(licencePositionStagedPayloadService.getAddedLicencePositionStagedPayloads(LICENCE_CORRECTION))
        .containsExactly(addedStagedPayload);
  }

  @Test
  void findFirstAddedPositionStagedPayload_whenPositionWasAddedInCorrection_returnsThatPositionStagedPayload() {
    var licencePositionId = UUID.randomUUID();
    var addedStagedPayload = addedStagedPayloadForPosition(licencePositionId);

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionStagedPayloadChangeType.ADD_POSITION))
        .thenReturn(List.of(addedStagedPayloadForPosition(UUID.randomUUID()), addedStagedPayload));

    assertThat(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(LICENCE_CORRECTION, licencePositionId))
        .contains(addedStagedPayload);
  }

  @Test
  void findFirstAddedPositionStagedPayload_whenPositionNotAddedInCorrection_isEmpty() {
    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionStagedPayloadChangeType.ADD_POSITION))
        .thenReturn(List.of(addedStagedPayloadForPosition(UUID.randomUUID())));

    assertThat(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(LICENCE_CORRECTION, UUID.randomUUID()))
        .isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"REF-1", "ref-1"})
  void isCorrectionReferenceInUse_whenReferenceMatchesExistingIgnoringCase_returnsTrue(String correctionReference) {
    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionStagedPayloadChangeType.ADD_POSITION))
        .thenReturn(List.of(addedStagedPayloadWithReference("REF-1")));

    assertThat(licencePositionStagedPayloadService.isCorrectionReferenceInUse(LICENCE_CORRECTION, correctionReference))
        .isTrue();
  }

  @Test
  void isCorrectionReferenceInUse_whenReferenceDoesNotMatchAnyExisting_returnsFalse() {
    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(LICENCE_CORRECTION, LicencePositionStagedPayloadChangeType.ADD_POSITION))
        .thenReturn(List.of(addedStagedPayloadWithReference("REF-1")));

    assertThat(licencePositionStagedPayloadService.isCorrectionReferenceInUse(LICENCE_CORRECTION, "REF-2")).isFalse();
  }

  @Test
  void removeExecutedPosition_savesRemovePositionStagedPayloadWithNoPayload() {
    licencePositionStagedPayloadService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var saved = licencePositionStagedPayloadCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.REMOVE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(saved.getPayload()).isNull();
  }

  @Test
  void removeExecutedPosition_whenUpdatePositionStagedPayloadExists_deletesUpdatePositionStagedPayloadFirst() {
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(updateStagedPayload));

    licencePositionStagedPayloadService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionStagedPayloadRepository).delete(updateStagedPayload);
    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getChangeType())
        .isEqualTo(LicencePositionStagedPayloadChangeType.REMOVE_POSITION);
  }

  @Test
  void removeExecutedPosition_whenNoUpdatePositionStagedPayloadExists_doesNotDelete() {
    licencePositionStagedPayloadService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
  }

  @Test
  void removeExecutedPosition_whenPositionAlreadyTargetedByRemovePositionStagedPayload_throwsAndDoesNotSave() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThatThrownBy(() ->
        licencePositionStagedPayloadService.removeExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isInstanceOf(IllegalStateException.class);

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void removeExecutedPosition_whenPositionNotExecuted_throwsAndDoesNotSave() {
    var nonExecutedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicencePositionStatus.SUBMITTED)
        .build();

    assertThatThrownBy(() ->
        licencePositionStagedPayloadService.removeExecutedPosition(LICENCE_CORRECTION, nonExecutedPosition))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("is not executed");

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void canRemovePosition_whenPositionNotAlreadyTargetedByRemovePositionStagedPayload_returnsTrue() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThat(licencePositionStagedPayloadService.canRemovePosition(LICENCE_CORRECTION, LICENCE_POSITION)).isTrue();
  }

  @Test
  void canRemovePosition_whenPositionAlreadyTargetedByRemovePositionStagedPayload_returnsFalse() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThat(licencePositionStagedPayloadService.canRemovePosition(LICENCE_CORRECTION, LICENCE_POSITION)).isFalse();
  }

  @Test
  void canRemovePosition_whenPositionNotExecuted_returnsFalse() {
    var nonExecutedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicencePositionStatus.SUBMITTED)
        .build();

    assertThat(licencePositionStagedPayloadService.canRemovePosition(LICENCE_CORRECTION, nonExecutedPosition)).isFalse();
  }

  @Test
  void reinstateDeletedPositionStagedPayload_whenMarkedForRemoval_deletesRemovePositionStagedPayloads() {
    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(true);
    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(Optional.of(removeStagedPayload));

    licencePositionStagedPayloadService.reinstateDeletedPositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionStagedPayloadRepository).delete(removeStagedPayload);
  }

  @Test
  void reinstateDeletedPositionStagedPayload_whenNotMarkedForRemoval_throwsAndDoesNotDelete() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThatThrownBy(() ->
        licencePositionStagedPayloadService.reinstateDeletedPositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .isInstanceOf(IllegalStateException.class);

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
  }

  @Test
  void canReinstateDeletedPositionStagedPayload_whenPositionTargetedByRemovePositionStagedPayload_returnsTrue() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(true);

    assertThat(licencePositionStagedPayloadService
        .canReinstateDeletedPositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION)).isTrue();
  }

  @Test
  void canReinstateDeletedPositionStagedPayload_whenPositionNotTargetedByRemovePositionStagedPayload_returnsFalse() {
    when(licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.REMOVE_POSITION))
        .thenReturn(false);

    assertThat(licencePositionStagedPayloadService
        .canReinstateDeletedPositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION)).isFalse();
  }

  @Test
  void correctPositionDate_whenNoExistingUpdatePositionStagedPayload_savesNewUpdatePositionStagedPayload() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionStagedPayloadService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var saved = licencePositionStagedPayloadCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);

    assertThat(saved.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);

    assertThat(saved.getPayload()).isInstanceOf(UpdateLicencePositionPayload.class);
    var payload = (UpdateLicencePositionPayload) saved.getPayload();

    var expectedPayload = new UpdateLicencePositionPayload(
        newDate, 1, LICENCE_CORRECTION.getCorrectionReference(), List.of()
    );
    assertThat(payload).usingRecursiveComparison().isEqualTo(expectedPayload);
  }

  @Test
  void correctPositionDate_whenExistingUpdatePositionStagedPayload_updatesItInPlace() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    var existing = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(2026, Month.JANUARY, 1))
            .withEffectiveDateOrder(9)
            .build())
        .build();

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionStagedPayloadService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var saved = licencePositionStagedPayloadCaptor.getValue();

    assertThat(saved).isSameAs(existing);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.effectiveDate()).isEqualTo(newDate);
  }

  @Test
  void correctPositionDate_whenExistingUpdatePositionStagedPayloadHasChanges_preservesChangesAndReference() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    var existingChange = LicencePositionChangeType.addChange()
        .withChangeId("change-id")
        .build();
    var existingPayload = new UpdateLicencePositionPayload(
        LocalDate.of(2026, Month.JANUARY, 1), 9, "REF-9", List.of(existingChange));

    var existing = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(existingPayload)
        .build();

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionStagedPayloadService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();

    assertThat(payload.effectiveDate()).isEqualTo(newDate);
    assertThat(payload.correctionReference()).isEqualTo("REF-9");
    assertThat(payload.changes()).containsExactly(existingChange);
  }

  @Test
  void correctPositionDate_setsEffectiveDateOrderFromLiveMaxOnTargetDate() {
    var newDate = LocalDate.of(2026, Month.JULY, 10);

    when(licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());
    givenExecutedPositions(executedPosition(UUID.randomUUID(), newDate, 4, "REF-EXISTING"));

    licencePositionStagedPayloadService.correctPositionDate(LICENCE_CORRECTION, LICENCE_POSITION, newDate);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload.effectiveDateOrder()).isEqualTo(5);
  }

  @Test
  void getOrderableSameDatePositions_whenPositionNotFound_returnsEmpty() {
    assertThat(licencePositionStagedPayloadService
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

    assertThat(licencePositionStagedPayloadService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
        .containsExactly(
            new OrderablePosition(sameDateEarlierId, POSITION_DATE, 1, "REF-EARLIER", false),
            new OrderablePosition(movedId, POSITION_DATE, 2, "REF-MOVED", false));
  }

  @Test
  void getOrderableSameDatePositions_appliesUpdatePositionStagedPayloadOverrides() {
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
    givenPositionStagedPayloads(updateStagedPayloadFor(moved, updatePayload));

    assertThat(licencePositionStagedPayloadService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
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
    givenPositionStagedPayloads(removeStagedPayloadFor(removed));

    assertThat(licencePositionStagedPayloadService.getOrderableSameDatePositions(LICENCE_CORRECTION, movedId))
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

    givenPositionStagedPayloads(addStagedPayloadFor(addPayload));

    assertThat(licencePositionStagedPayloadService.getOrderableSameDatePositions(LICENCE_CORRECTION, addedId))
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
    givenPositionStagedPayloads(removeStagedPayloadFor(removed), addStagedPayloadFor(addPayload));

    assertThat(licencePositionStagedPayloadService.getOrderablePositionsOnDate(LICENCE_CORRECTION, POSITION_DATE))
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

    var lookup = licencePositionStagedPayloadService
        .findSameTransactionPositionOnDate(LICENCE_CORRECTION, sourceId, targetDate);

    assertThat(lookup).isEqualTo(new SameTransactionPositionLookup(transaction.getId(), matchId));
  }

  @Test
  void findSameTransactionPositionOnDate_whenTheDateIsTheSourcesOwnDate_thenDoesNotMatchTheSource() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var sourceId = UUID.randomUUID();

    givenExecutedPositions(executedPosition(sourceId, POSITION_DATE, 1, transaction));

    var lookup = licencePositionStagedPayloadService
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

    var lookup = licencePositionStagedPayloadService
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

    var lookup = licencePositionStagedPayloadService
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

    givenPositionStagedPayloads(addStagedPayloadFor(sourcePayload), addStagedPayloadFor(matchPayload));

    var lookup = licencePositionStagedPayloadService
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

    assertThat(licencePositionStagedPayloadService.getOrderableDatePositions(LICENCE_CORRECTION))
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
    givenPositionStagedPayloads(removeStagedPayloadFor(removed), addStagedPayloadFor(addPayload));

    assertThat(licencePositionStagedPayloadService.getOrderableDatePositions(LICENCE_CORRECTION))
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
    givenPositionStagedPayloads(addStagedPayloadFor(addPayload));

    assertThat(licencePositionStagedPayloadService.getOrderableDatePositions(LICENCE_CORRECTION))
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
    givenPositionStagedPayloads(addStagedPayloadFor(addPayload));

    assertThat(licencePositionStagedPayloadService.getOrderableDatePosition(LICENCE_CORRECTION, positionId))
        .isEqualTo(new OrderablePosition(positionId, POSITION_DATE, 1, "ADD-REF", true));
  }

  @Test
  void getOrderableDatePosition_whenPositionNotFound_thenThrows() {
    var positionId = UUID.randomUUID();

    givenExecutedPositions(executedPosition(UUID.randomUUID(), POSITION_DATE, 1, "REF-OTHER"));

    assertThatThrownBy(() -> licencePositionStagedPayloadService
        .getOrderableDatePosition(LICENCE_CORRECTION, positionId))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void correctPositionOrder_whenMovedPositionNotOnSameDate_throwsAndWritesNothing() {
    var movedId = UUID.randomUUID();
    var targetId = UUID.randomUUID();

    assertThatThrownBy(() -> licencePositionStagedPayloadService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, targetId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not orderable on the same date");

    verify(licencePositionStagedPayloadRepository, never()).save(any());
    verify(licencePositionStagedPayloadRepository, never()).delete(any());
  }

  @Test
  void correctPositionOrder_whenTargetIsSameAsMovedPosition_throws() {
    var movedId = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    givenExecutedPositions(
        executedPosition(movedId, POSITION_DATE, 1, "REF-A"),
        executedPosition(otherId, POSITION_DATE, 2, "REF-B"));

    assertThatThrownBy(() -> licencePositionStagedPayloadService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, movedId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void correctPositionOrder_whenTargetPositionNotOnSameDate_throws() {
    var movedId = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    var unknownTargetId = UUID.randomUUID();
    givenExecutedPositions(
        executedPosition(movedId, POSITION_DATE, 1, "REF-A"),
        executedPosition(otherId, POSITION_DATE, 2, "REF-B"));

    assertThatThrownBy(() -> licencePositionStagedPayloadService
        .correctPositionOrder(LICENCE_CORRECTION, movedId, unknownTargetId, PositionMoveDirection.AFTER))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licencePositionStagedPayloadRepository, never()).save(any());
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

    licencePositionStagedPayloadService.correctPositionOrder(LICENCE_CORRECTION, aId, cId, PositionMoveDirection.AFTER);

    verify(licencePositionStagedPayloadRepository, times(3)).save(licencePositionStagedPayloadCaptor.capture());

    var savedPayloadsByTargetId = licencePositionStagedPayloadCaptor.getAllValues().stream()
        .collect(Collectors.toMap(
            positionStagedPayload -> positionStagedPayload.getTargetLicencePosition().getId(),
            positionStagedPayload -> (UpdateLicencePositionPayload) positionStagedPayload.getPayload()));

    assertThat(savedPayloadsByTargetId)
        .usingRecursiveComparison()
        .isEqualTo(Map.of(
            bId, new UpdateLicencePositionPayload(POSITION_DATE, 1, null, List.of()),
            cId, new UpdateLicencePositionPayload(POSITION_DATE, 2, null, List.of()),
            aId, new UpdateLicencePositionPayload(POSITION_DATE, 3, null, List.of())));
    assertThat(licencePositionStagedPayloadCaptor.getAllValues())
        .allSatisfy(positionStagedPayload -> assertThat(positionStagedPayload.getChangeType())
            .isEqualTo(LicencePositionStagedPayloadChangeType.UPDATE_POSITION));
  }

  @Test
  void correctPositionOrder_whenPositionKeepsLiveOrderAndHasNoExistingPositionStagedPayload_isNotSaved() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();
    var cId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(aId, POSITION_DATE, 1, "REF-A"),
        executedPosition(bId, POSITION_DATE, 2, "REF-B"),
        executedPosition(cId, POSITION_DATE, 3, "REF-C"));

    licencePositionStagedPayloadService.correctPositionOrder(LICENCE_CORRECTION, bId, cId, PositionMoveDirection.AFTER);

    verify(licencePositionStagedPayloadRepository, times(2)).save(licencePositionStagedPayloadCaptor.capture());

    assertThat(licencePositionStagedPayloadCaptor.getAllValues())
        .extracting(positionStagedPayload -> positionStagedPayload.getTargetLicencePosition().getId())
        .containsExactlyInAnyOrder(cId, bId)
        .doesNotContain(aId);
  }

  @Test
  void correctPositionOrder_whenMoveRestoresLiveOrder_deletesRedundantUpdatePositionStagedPayloads() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();

    var positionA = executedPosition(aId, POSITION_DATE, 1, "REF-A");
    var positionB = executedPosition(bId, POSITION_DATE, 2, "REF-B");

    var updateA = updateStagedPayloadFor(positionA, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(2).withCorrectionReference(null).build());
    var updateB = updateStagedPayloadFor(positionB, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(1).withCorrectionReference(null).build());

    givenExecutedPositions(positionA, positionB);
    givenPositionStagedPayloads(updateA, updateB);

    licencePositionStagedPayloadService.correctPositionOrder(LICENCE_CORRECTION, aId, bId, PositionMoveDirection.BEFORE);

    verify(licencePositionStagedPayloadRepository).delete(updateA);
    verify(licencePositionStagedPayloadRepository).delete(updateB);
    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void correctPositionOrder_whenExistingPositionStagedPayloadCarriesReference_updatesOrderAndPreservesReference() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();
    var cId = UUID.randomUUID();

    var positionA = executedPosition(aId, POSITION_DATE, 1, "REF-A");
    var positionB = executedPosition(bId, POSITION_DATE, 2, "REF-B");
    var positionC = executedPosition(cId, POSITION_DATE, 3, "REF-C");

    var updateA = updateStagedPayloadFor(positionA, UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(POSITION_DATE).withEffectiveDateOrder(1).withCorrectionReference("KEEP-REF").build());

    givenExecutedPositions(positionA, positionB, positionC);
    givenPositionStagedPayloads(updateA);

    licencePositionStagedPayloadService.correctPositionOrder(LICENCE_CORRECTION, cId, aId, PositionMoveDirection.BEFORE);

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
    verify(licencePositionStagedPayloadRepository, times(3)).save(licencePositionStagedPayloadCaptor.capture());

    var savedForA = licencePositionStagedPayloadCaptor.getAllValues().stream()
        .filter(positionStagedPayload -> aId.equals(positionStagedPayload.getTargetLicencePosition().getId()))
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
    givenPositionStagedPayloads(addStagedPayloadFor(addPayload));

    licencePositionStagedPayloadService
        .correctPositionOrder(LICENCE_CORRECTION, addedId, executedId, PositionMoveDirection.BEFORE);

    verify(licencePositionStagedPayloadRepository, times(2)).save(licencePositionStagedPayloadCaptor.capture());

    var savedAdded = licencePositionStagedPayloadCaptor.getAllValues().stream()
        .filter(positionStagedPayload -> positionStagedPayload.getPayload() instanceof CreateLicencePositionPayload)
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
  void correctPositionOrder_loadsPositionsAndPositionStagedPayloadsWithoutDuplicateQueries() {
    var aId = UUID.randomUUID();
    var bId = UUID.randomUUID();

    givenExecutedPositions(
        executedPosition(aId, POSITION_DATE, 1, "REF-A"),
        executedPosition(bId, POSITION_DATE, 2, "REF-B"));

    licencePositionStagedPayloadService.correctPositionOrder(LICENCE_CORRECTION, aId, bId, PositionMoveDirection.AFTER);

    verify(licencePositionService, times(1)).getExecutedChronologicalLicencePositions(LICENCE);
    verify(licencePositionStagedPayloadRepository, times(1)).findByLicenceCorrection(LICENCE_CORRECTION);
    verify(licencePositionStagedPayloadRepository, never())
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(any(), any(), any());
  }

  @Test
  void getAddOperationsOfType_whenNoMatchingChange_returnsEmpty() {
    assertThat(licencePositionStagedPayloadService.getAddOperationsOfType(List.of(), SetEquityOperation.class))
        .isEmpty();
  }

  @Test
  void getAddOperationsOfType_returnsTheOperationsFromMatchingAddChanges() {
    var changes = List.of(setEquityChange(List.of(setEquityOp(1, 40), setEquityOp(2, 60))));

    assertThat(licencePositionStagedPayloadService.getAddOperationsOfType(changes, SetEquityOperation.class))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1, 2);
  }

  @Test
  void replaceAddChangeFor_whenNoExistingChange_createsOneWithTheOperations() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
  }

  @Test
  void replaceAddChangeFor_whenExistingChange_replacesItsOperations() {
    var positionStagedPayload = positionStagedPayloadWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, SetEquityOperation.class, List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    var change = (AddChange) payload.changes().getFirst();
    assertThat(payload.changes()).hasSize(1);
    assertThat(change.operations()).hasSize(2);
  }

  @Test
  void replaceAddChangeFor_whenNoOperations_dropsTheChange() {
    var positionStagedPayload = positionStagedPayloadWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionStagedPayloadService.replaceAddChangeFor(positionStagedPayload, SetEquityOperation.class, List.of());

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload.changes()).isEmpty();
  }

  @Test
  void replaceAddChangeFor_retainsOtherChangeTypes() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    // seed an unrelated change type (administrator change) on the same position
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(ADMINISTRATOR_ID)
        .build();
    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(
        positionStagedPayload.getPayload(),
        List.of(AddChange.buildOperationsChange(List.of(administratorOperation), 1))));

    licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    var payload = (CreateLicencePositionPayload) positionStagedPayload.getPayload();
    assertThat(payload.changes()).hasSize(2);
    assertThat(licencePositionStagedPayloadService.getAddOperationsOfType(payload.changes(), SetEquityOperation.class))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1);
  }

  private CreateLicencePositionPayload captureSavedPayload() {
    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(CreateLicencePositionPayload.class);
    return (CreateLicencePositionPayload) payload;
  }

  @Test
  void findPositionStagedPayload_whenExecutedPosition_thenItsUpdatePositionStagedPayload() {
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(updateStagedPayload));

    var result = licencePositionStagedPayloadService
        .findPositionStagedPayload(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).containsSame(updateStagedPayload);
  }

  @Test
  void findPositionStagedPayload_whenStagedPosition_thenItsAddedPositionStagedPayload() {
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().withLicenceCorrection(LICENCE_CORRECTION).build();

    var result = licencePositionStagedPayloadService.findPositionStagedPayload(new CorrectionPosition.Staged(addStagedPayload));

    assertThat(result).containsSame(addStagedPayload);
  }

  @Test
  void getOrBuildPositionStagedPayload_whenExecutedPosition_thenItsUpdatePositionStagedPayload() {
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(updateStagedPayload));

    var result = licencePositionStagedPayloadService
        .getOrBuildPositionStagedPayload(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).isSameAs(updateStagedPayload);
  }

  @Test
  void getOrBuildPositionStagedPayload_whenStagedPosition_thenItsAddedPositionStagedPayload() {
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().withLicenceCorrection(LICENCE_CORRECTION).build();

    var result = licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(new CorrectionPosition.Staged(addStagedPayload));

    assertThat(result).isSameAs(addStagedPayload);
  }

  @Test
  void getEffectivePositionDate_whenStagedPosition_thenThePayloadEffectiveDate() {
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(POSITION_DATE)
            .build())
        .build();

    var result = licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Staged(addStagedPayload));

    assertThat(result).isEqualTo(POSITION_DATE);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOn_whenExecutedPosition_collectsFeatureIdsFromLiveAndStagedChanges() {
    var liveFeatureId = UUID.randomUUID();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(liveFeatureId, List.of(), List.of())))
        .build();
    var stagedFeatureId = UUID.randomUUID();
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(
                List.of(new SubareaOperation(stagedFeatureId, List.of(), List.of())), 2)))
            .build())
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(updateStagedPayload));
    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionStagedPayloadService
        .blockFeatureIdsAlreadyOperatedOn(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).containsExactlyInAnyOrder(liveFeatureId, stagedFeatureId);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOn_whenStagedPositionAndChangeIdExcluded_omitsThatChangesFeatureIds() {
    var excludedChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var retainedFeatureId = UUID.randomUUID();
    var retainedChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(retainedFeatureId, List.of(), List.of())), 2);
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(excludedChange, retainedChange))
            .build())
        .build();

    var result = licencePositionStagedPayloadService
        .blockFeatureIdsAlreadyOperatedOn(new CorrectionPosition.Staged(addStagedPayload), excludedChange.changeId());

    assertThat(result).containsExactly(retainedFeatureId);
  }

  private LicencePositionStagedPayload addedStagedPayloadForPosition(UUID licencePositionId) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(licencePositionId.toString())
            .build())
        .build();
  }

  private LicencePositionStagedPayload addedStagedPayloadWithReference(String correctionReference) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withCorrectionReference(correctionReference)
        .build();

    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(payload)
        .build();
  }

  private LicencePositionStagedPayload draftStagedPayloadWith(LocalDate effectiveDate, int effectiveDateOrder) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(UUID.randomUUID().toString())
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .build();

    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(payload)
        .build();
  }

  private static SetEquityOperation setEquityOp(int transferTo, int equity) {
    return new SetEquityOperation(transferTo, BigDecimal.valueOf(equity));
  }

  private static LicencePositionStagedPayload positionStagedPayloadWithSetEquity(
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
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  @Test
  void getOrBuildUpdatePositionStagedPayload_whenExistingUpdatePositionStagedPayload_returnsExistingWithoutSaving() {
    var existing = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    assertThat(licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .isSameAs(existing);

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void getOrBuildUpdatePositionStagedPayload_whenNoExistingPositionStagedPayload_buildsNewUpdatePositionStagedPayloadWithoutSaving() {
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    var result = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(result.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(result.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    assertThat(result.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(result.getPayload()).isInstanceOf(UpdateLicencePositionPayload.class);

    var payload = (UpdateLicencePositionPayload) result.getPayload();
    assertThat(payload.correctionReference()).isEqualTo(LICENCE_CORRECTION.getCorrectionReference());
    assertThat(payload.changes()).isEmpty();

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void stageRemovalOfExecutedChange_whenNothingIsStagedAgainstTheChange_thenStagesARemoveChange() {
    var changeId = UUID.randomUUID().toString();
    var otherChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    givenUpdatePositionStagedPayload(List.of(otherChange));

    licencePositionStagedPayloadService.stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, changeId);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes()).containsExactly(
        otherChange,
        LicencePositionChangeType.removeChange().withChangeId(changeId).build());
  }

  @Test
  void stageRemovalOfExecutedChange_whenTheChangeIsAlreadyCorrected_thenReplacesTheCorrectionWithARemoveChange() {
    var changeId = UUID.randomUUID().toString();
    var otherChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    givenUpdatePositionStagedPayload(List.of(
        UpdateChangeOperations.buildUpdateChange(changeId,
            LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()),
        otherChange));

    licencePositionStagedPayloadService.stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, changeId);

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes()).containsExactly(
        otherChange,
        LicencePositionChangeType.removeChange().withChangeId(changeId).build());
  }

  @Test
  void dropStagedChangesOf_whenChangesOfTheOperationTypeAreStaged_thenDropsOnlyThose() {
    var addedAdministratorChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    var correctedAdministratorChange = UpdateChangeOperations.buildUpdateChange(UUID.randomUUID().toString(),
        LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build());
    var licenseeChange = AddChange.buildOperationsChange(List.of(LicenceOperation.newLicenseeOperation().build()), 2);
    givenUpdatePositionStagedPayload(List.of(addedAdministratorChange, correctedAdministratorChange, licenseeChange));

    licencePositionStagedPayloadService.dropStagedChangesOf(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        AdministratorOperation.class
    );

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes()).containsExactly(licenseeChange);
  }

  @Test
  void dropStagedChangesOf_whenNothingIsStagedAgainstThePosition_thenSavesNothing() {
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionStagedPayloadService.dropStagedChangesOf(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        AdministratorOperation.class
    );

    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  private void givenUpdatePositionStagedPayload(List<LicencePositionChangeType> changes) {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
  }

  @Test
  void replaceAddChangeFor_whenUpdatePayload_rebuildsAsUpdatePayloadPreservingType() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, SetEquityOperation.class, List.of(setEquityOp(1, 100)));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = licencePositionStagedPayloadCaptor.getValue().getPayload();
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
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(positions));
  }

  private void givenPositionStagedPayloads(LicencePositionStagedPayload... corrections) {
    when(licencePositionStagedPayloadRepository.findByLicenceCorrection(LICENCE_CORRECTION))
        .thenReturn(List.of(corrections));
  }

  private LicencePositionStagedPayload updateStagedPayloadFor(LicencePosition target, UpdateLicencePositionPayload payload) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(target)
        .withPayload(payload)
        .build();
  }

  private LicencePositionStagedPayload removeStagedPayloadFor(LicencePosition target) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(target)
        .build();
  }

  private LicencePositionStagedPayload addStagedPayloadFor(CreateLicencePositionPayload payload) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withPayload(payload)
        .build();
  }

  @Test
  void resolveEffectiveDate_whenAddedPosition_returnsThePayloadEffectiveDate() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(POSITION_DATE)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).isEqualTo(POSITION_DATE);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithCorrectedDate_returnsTheCorrectedDate() {
    var correctedDate = POSITION_DATE.plusMonths(1);
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withPositionDate(POSITION_DATE).build())
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(correctedDate)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).isEqualTo(correctedDate);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithNoCorrectedDate_returnsThePositionDate() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withPositionDate(POSITION_DATE).build())
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(null)
            .withChanges(List.of())
            .build())
        .build();

    assertThat(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).isEqualTo(POSITION_DATE);
  }

  @Test
  void resolveEffectiveDate_whenExecutedPositionWithNoCorrectedDateAndNoTarget_thenThrows() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(null)
            .withChanges(List.of())
            .build())
        .build();

    assertThatThrownBy(() -> licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(("Cannot resolve the effective date of licence position staged payload %s as it updates a position "
            + "but has no target").formatted(positionStagedPayload.getId()));
  }

  @Test
  void getEffectivePositionDate_whenNoUpdatePositionStagedPayload_returnsThePositionDate() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    assertThat(licencePositionStagedPayloadService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(POSITION_DATE);
  }

  @Test
  void getEffectivePositionDate_whenDateCorrectedInThisCorrection_returnsTheCorrectedDate() {
    var correctedDate = POSITION_DATE.plusMonths(1);
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionStagedPayloadTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(correctedDate)
                .withChanges(List.of())
                .build())
            .build()));

    assertThat(licencePositionStagedPayloadService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(correctedDate);
  }

  @Test
  void getEffectivePositionDate_whenUpdatePositionStagedPayloadHasNoDate_returnsThePositionDate() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, licencePosition, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionStagedPayloadTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(null)
                .withChanges(List.of())
                .build())
            .build()));

    assertThat(licencePositionStagedPayloadService.getEffectivePositionDate(LICENCE_CORRECTION, licencePosition))
        .isEqualTo(POSITION_DATE);
  }

  @Test
  void getCommittedChangeOfType_whenNoPositionStagedPayload_returnsEmpty() {
    assertThat(licencePositionStagedPayloadService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .isEmpty();
  }

  @Test
  void getCommittedChangeOfType_whenChangeOfTypeStaged_returnsTheOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(operation), 1)))
            .build())
        .build();

    assertThat(licencePositionStagedPayloadService.getCommittedChangeOfType(positionStagedPayload, PartialSurrenderOperation.class))
        .contains(operation);
  }

  @Test
  void getCommittedChangeOfType_whenNoChangeOfType_returnsEmpty() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    assertThat(licencePositionStagedPayloadService.getCommittedChangeOfType(positionStagedPayload, PartialSurrenderOperation.class))
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

    assertThat(licencePositionStagedPayloadService.getAddOperationsOfType(changes, PartialSurrenderOperation.class))
        .containsExactly(added);
  }

  @Test
  void getChangesForExecutedPosition_whenNoUpdatePositionStagedPayload_foldsLiveChangesOnly() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionStagedPayloadService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null
    );

    assertThat(result).containsExactly(
        new PositionChange(liveChange.getId().toString(), 1, null, List.of(operation)));
  }

  @Test
  void getChangesForExecutedPosition_whenUpdatePositionStagedPayloadPresent_foldsLiveAndStagedChanges() {
    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(liveOperation))
        .build();

    var stagedOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedChange = AddChange.buildOperationsChange(List.of(stagedOperation), 2);
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(stagedChange))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionStagedPayloadService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateStagedPayload
    );

    assertThat(result).containsExactly(
        new PositionChange(liveChange.getId().toString(), 1, null, List.of(liveOperation)),
        new PositionChange(stagedChange.changeId(), 2, LicencePositionChangeType.ADD_CHANGE, List.of(stagedOperation)));
  }

  @Test
  void getChangesForAddedPosition_foldsAddedPositionStagedPayloadChanges() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var addChange = AddChange.buildOperationsChange(List.of(operation), 1);
    var addedStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(addChange))
            .build())
        .build();

    var result = licencePositionStagedPayloadService.getChangesForAddedPosition(addedStagedPayload);

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
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(new SubareaOperation(stagedFeatureId, List.of(), List.of())), 2)))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionStagedPayloadService
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION, updateStagedPayload);

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

    var removalStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(LicencePositionChangeType.removeChange()
                .withChangeId(liveChange.getId().toString())
                .build()))
            .build())
        .build();

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    var result = licencePositionStagedPayloadService
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION, removalStagedPayload);

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

    var result = licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
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
    var addedStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(
                AddChange.buildOperationsChange(List.of(new SubareaOperation(subareaFeatureId, List.of(), List.of())), 1),
                AddChange.buildOperationsChange(List.of(
                    new PartialSurrenderOperation(
                        null, List.of(surrenderFeatureIdOne, surrenderFeatureIdTwo), null)), 2)))
            .build())
        .build();

    var result = licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(addedStagedPayload);

    assertThat(result).containsExactlyInAnyOrder(subareaFeatureId, surrenderFeatureIdOne, surrenderFeatureIdTwo);
  }

  @Test
  void blockFeatureIdsAlreadyOperatedOnForAddedPosition_whenChangeIdExcluded_omitsThatChangesFeatureIds() {
    var excludedFeatureId = UUID.randomUUID();
    var excludedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(excludedFeatureId, List.of(), List.of())), 1);
    var retainedFeatureId = UUID.randomUUID();
    var retainedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(retainedFeatureId, List.of(), List.of())), 2);
    var addedStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(excludedChange, retainedChange))
            .build())
        .build();

    var result = licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        addedStagedPayload, excludedChange.changeId());

    assertThat(result).containsExactly(retainedFeatureId);
  }

  @Test
  void replaceAddChangeFor_whenOperationToReplaceGiven_thenReplacesOnlyTheMatchingChange() {
    var blockFeatureId = UUID.randomUUID();
    var subareaA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var subareaB = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var subareaBOperation = new SubareaSetEquityOperation(blockFeatureId, subareaB, 2, BigDecimal.valueOf(100));
    var replacementOperation = new SubareaSetEquityOperation(blockFeatureId, subareaA, 3, BigDecimal.valueOf(100));
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(
                AddChange.buildOperationsChange(
                    List.of(new SubareaSetEquityOperation(blockFeatureId, subareaA, 1, BigDecimal.valueOf(100))), 1),
                AddChange.buildOperationsChange(List.of(subareaBOperation), 2)))
            .build())
        .build();

    licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload,
        SubareaSetEquityOperation.class,
        operation -> operation.subarea().equals(subareaA),
        List.of(replacementOperation));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(licencePositionStagedPayloadService.getAddOperationsOfType(payload.changes(), SubareaSetEquityOperation.class))
        .containsExactly(subareaBOperation, replacementOperation);
  }

  @Test
  void replaceAddChangeForExecutedPosition_whenLiveChangeOrderedAfterStagedChanges_thenOrdersNewChangeAfterLiveChange() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(2)
        .withOperations(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())))
        .build();
    var updateStagedPayload = updatePositionStagedPayloadWith(List.of());
    var newOperation = new SubareaSetEquityOperation(
        UUID.randomUUID(), new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"), 1, BigDecimal.valueOf(100));

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    licencePositionStagedPayloadService.replaceAddChangeForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateStagedPayload,
        SubareaSetEquityOperation.class,
        operation -> true,
        List.of(newOperation));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFields("changeId", "operations.operationId")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(newOperation), 3)));
  }

  @Test
  void replaceAddChangeForExecutedPosition_whenReplacingStagedChange_thenOrdersAfterTheRemainingChanges() {
    var blockFeatureId = UUID.randomUUID();
    var subareaA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var subareaB = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())))
        .build();
    var subareaBChange = AddChange.buildOperationsChange(
        List.of(new SubareaSetEquityOperation(blockFeatureId, subareaB, 2, BigDecimal.valueOf(100))), 2);
    var updateStagedPayload = updatePositionStagedPayloadWith(List.of(
        subareaBChange,
        AddChange.buildOperationsChange(
            List.of(new SubareaSetEquityOperation(blockFeatureId, subareaA, 1, BigDecimal.valueOf(100))), 3)
    ));
    var replacementOperation = new SubareaSetEquityOperation(blockFeatureId, subareaA, 3, BigDecimal.valueOf(100));

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    licencePositionStagedPayloadService.replaceAddChangeForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateStagedPayload,
        SubareaSetEquityOperation.class,
        operation -> operation.subarea().equals(subareaA),
        List.of(replacementOperation));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFields("changeId", "operations.operationId")
        .isEqualTo(List.of(subareaBChange, AddChange.buildOperationsChange(List.of(replacementOperation), 3)));
  }

  @Test
  void replaceAddChangeForExecutedPosition_whenStagedRemovalHasNoLiveChange_thenIgnoresItsPlaceholderOrder() {
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(UUID.randomUUID().toString()).build();
    var updateStagedPayload = updatePositionStagedPayloadWith(List.of(removeChange));
    var newOperation = new SubareaSetEquityOperation(
        UUID.randomUUID(), new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"), 1, BigDecimal.valueOf(100));

    licencePositionStagedPayloadService.replaceAddChangeForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateStagedPayload,
        SubareaSetEquityOperation.class,
        operation -> true,
        List.of(newOperation));

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFields("changeId", "operations.operationId")
        .isEqualTo(List.of(removeChange, AddChange.buildOperationsChange(List.of(newOperation), 1)));
  }

  @Test
  void addChanges() {
    var blockFeatureId = UUID.randomUUID();
    var stagedChange = AddChange.buildOperationsChange(
        List.of(new SubareaSetEquityOperation(
            blockFeatureId, new SubareaDetails(UUID.randomUUID(), "Subarea C", "C"), 1, BigDecimal.valueOf(100))),
        1
    );
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(stagedChange));
    var subareaAOperation = new SubareaSetEquityOperation(
        blockFeatureId, new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"), 2, BigDecimal.valueOf(100));
    var subareaBOperation = new SubareaSetEquityOperation(
        blockFeatureId, new SubareaDetails(UUID.randomUUID(), "Subarea B", "B"), 3, BigDecimal.valueOf(100));

    licencePositionStagedPayloadService.addChanges(
        positionStagedPayload,
        List.of(List.of(subareaAOperation), List.of(subareaBOperation))
    );

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFields("changeId", "operations.operationId")
        .isEqualTo(List.of(
            stagedChange,
            AddChange.buildOperationsChange(List.of(subareaAOperation), 2),
            AddChange.buildOperationsChange(List.of(subareaBOperation), 3)
        ));
  }

  @Test
  void addChangesForExecutedPosition() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(LICENCE_POSITION)
        .withChangeOrder(2)
        .withOperations(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())))
        .build();
    var updateStagedPayload = updatePositionStagedPayloadWith(List.of());
    var blockFeatureId = UUID.randomUUID();
    var subareaAOperation = new SubareaSetEquityOperation(
        blockFeatureId, new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"), 1, BigDecimal.valueOf(100));
    var subareaBOperation = new SubareaSetEquityOperation(
        blockFeatureId, new SubareaDetails(UUID.randomUUID(), "Subarea B", "B"), 1, BigDecimal.valueOf(100));

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(liveChange));

    licencePositionStagedPayloadService.addChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        updateStagedPayload,
        List.of(List.of(subareaAOperation), List.of(subareaBOperation))
    );

    verify(licencePositionChangeService).findByLicencePositionId(LICENCE_POSITION.getId());
    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue().getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFields("changeId", "operations.operationId")
        .isEqualTo(List.of(
            AddChange.buildOperationsChange(List.of(subareaAOperation), 3),
            AddChange.buildOperationsChange(List.of(subareaBOperation), 4)
        ));
  }

  @Test
  void getStagedChangeOrThrow_whenTheChangeIsStaged_thenReturnsIt() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(change));

    var result = licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, change.changeId());

    assertThat(result).isEqualTo(change);
  }

  @Test
  void getStagedChangeOrThrow_whenOnlyAChangeOrderIsStagedForThatId_thenThrows() {
    var changeId = UUID.randomUUID().toString();
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(
        LicencePositionChangeType.updateChangeOrder().withChangeId(changeId).withChangeOrder(2).build()));

    assertThatThrownBy(() -> licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);
  }

  @Test
  void getStagedChangeOrThrow_whenTheChangeIsNotStaged_thenThrows() {
    var changeId = UUID.randomUUID().toString();
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of());

    assertThatThrownBy(() -> licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);
  }

  @Test
  void resolveStagedChangeOperations_whenTheChangeStagesItsOwnOperations_thenReturnsThem() {
    var operation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var change = AddChange.buildOperationsChange(List.of(operation), 1);

    var result = licencePositionStagedPayloadService.resolveStagedChangeOperations(change);

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

    var result = licencePositionStagedPayloadService.resolveStagedChangeOperations(
        LicencePositionChangeType.removeChange().withChangeId(liveChangeId.toString()).build());

    assertThat(result).containsExactly(operation);
  }

  @Test
  void findStagedChangeOperation_whenTheChangeStagesAnOperationOfTheType_thenReturnsIt() {
    var subareaOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build();
    var change = AddChange.buildOperationsChange(List.of(administratorOperation, subareaOperation), 1);

    var result = licencePositionStagedPayloadService.findStagedChangeOperation(change, SubareaOperation.class);

    assertThat(result).contains(subareaOperation);
  }

  @Test
  void findStagedChangeOperation_whenTheChangeStagesNoOperationOfTheType_thenReturnsEmpty() {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build();
    var change = AddChange.buildOperationsChange(List.of(administratorOperation), 1);

    var result = licencePositionStagedPayloadService.findStagedChangeOperation(change, SubareaOperation.class);

    assertThat(result).isEmpty();
  }

  @Test
  void dropStagedChange_whenThePositionStagedPayloadIsLeftEmptyAndTheDateAndOrderAreUnchanged_thenDeletesIt() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(change));

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, change.changeId());

    verify(licencePositionStagedPayloadRepository).delete(positionStagedPayload);
    verify(licencePositionStagedPayloadRepository, never()).save(any());
  }

  @Test
  void dropStagedChange_whenOtherChangesRemain_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var retainedChange = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 2);
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(change, retainedChange));

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, change.changeId());

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChange_whenAChangeOrderIsStagedForTheSameChange_thenThatIsKeptAndThePositionStagedPayloadSaved() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var changeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId(change.changeId()).withChangeOrder(2).build();
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(change, changeOrder));

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, change.changeId());

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(changeOrder);
  }

  @Test
  void dropStagedChange_whenThePositionDateWasAlsoCorrected_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LICENCE_POSITION.getPositionDate().plusMonths(1))
            .withEffectiveDateOrder(LICENCE_POSITION.getPositionDateOrder())
            .withChanges(List.of(change))
            .build())
        .build();

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, change.changeId());

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).isEmpty();
  }

  @Test
  void dropStagedChange_whenThePositionStagedPayloadAddsThePosition_thenSavesWithoutDeleting() {
    var change = AddChange.buildOperationsChange(List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())), 1);
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of(change)).build())
        .build();

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, change.changeId());

    verify(licencePositionStagedPayloadRepository, never()).delete(any());
    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).isEmpty();
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
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            movedChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        LicencePositionChangeType.updateChangeOrder().withChangeId(movedChangeId).withChangeOrder(2).build(),
        retainedChange
    ));

    when(licencePositionChangeService.findById(movedLiveChange.getId())).thenReturn(Optional.of(movedLiveChange));

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, movedChangeId);

    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChange_whenALiveChangeOnThisPositionWasCorrected_thenItsChangeOrderIsKept() {
    var liveChange = liveChangeWithOrder(1);
    var liveChangeId = liveChange.getId().toString();
    var changeOrder = LicencePositionChangeType.updateChangeOrder().withChangeId(liveChangeId).withChangeOrder(2).build();
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            liveChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        changeOrder
    ));

    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, liveChangeId);

    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(changeOrder);
  }

  @Test
  void dropStagedChangeAndOrder_whenOtherChangesRemain_thenDropsTheChangeAndItsOrder() {
    var liveChange = liveChangeWithOrder(1);
    var liveChangeId = liveChange.getId().toString();
    var retainedChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        2
    );
    var positionStagedPayload = updatePositionStagedPayloadWith(List.of(
        UpdateChangeOperations.buildUpdateChange(
            liveChangeId,
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        ),
        LicencePositionChangeType.updateChangeOrder().withChangeId(liveChangeId).withChangeOrder(3).build(),
        retainedChange
    ));

    licencePositionStagedPayloadService.dropStagedChangeAndOrder(positionStagedPayload, liveChangeId);

    verify(licencePositionStagedPayloadRepository).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(retainedChange);
  }

  @Test
  void dropStagedChangeAndOrder_whenThePositionStagedPayloadWasOnlyBuilt_thenNeitherSavesNorDeletesIt() {
    var builtStagedPayload =
        licencePositionStagedPayloadService.newUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION);

    licencePositionStagedPayloadService.dropStagedChangeAndOrder(builtStagedPayload, UUID.randomUUID().toString());

    verify(licencePositionStagedPayloadRepository, never()).save(any());
    verify(licencePositionStagedPayloadRepository, never()).delete(any());
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
    var addedStagedPayload = addStagedPayloadFor(addedPayload);

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionStagedPayloadChangeType.ADD_POSITION
    )).thenReturn(List.of(addedStagedPayload));

    licencePositionStagedPayloadService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        targetPositionId,
        List.of(movedChange(2))
    );

    verify(licencePositionStagedPayloadRepository).save(addedStagedPayload);
    assertThat(addedStagedPayload.getPayload())
        .isEqualTo(LicencePositionPayload.withChanges(addedPayload, List.of(existingChange, movedChange(2))));
  }

  @Test
  void stageChangesOnPosition_whenExecutedTargetHasNoUpdatePositionStagedPayload_thenBuildsOne() {
    givenExecutedTargetPosition(Optional.empty());

    licencePositionStagedPayloadService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION.getId(),
        List.of(movedChange(3))
    );

    var expected = new LicencePositionStagedPayload();
    expected.setLicenceCorrection(LICENCE_CORRECTION);
    expected.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    expected.setTargetLicencePosition(LICENCE_POSITION);
    expected.setPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
        .withCorrectionReference(LICENCE_CORRECTION.getCorrectionReference())
        .withChanges(List.of(movedChange(3)))
        .build());

    verify(licencePositionStagedPayloadRepository).save(licencePositionStagedPayloadCaptor.capture());
    assertThat(licencePositionStagedPayloadCaptor.getValue()).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void stageChangesOnPosition_whenTargetPositionIsNotOnTheLicence_thenThrowsAndSavesNothing() {
    var targetPositionId = UUID.randomUUID();

    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionStagedPayloadChangeType.ADD_POSITION
    )).thenReturn(List.of());
    when(licencePositionService.getPositionForLicence(LICENCE, targetPositionId))
        .thenThrow(new LmsEntityNotFoundException("licencePosition", targetPositionId));
    List<LicencePositionChangeType> changes = List.of(movedChange(1));

    assertThatThrownBy(() -> licencePositionStagedPayloadService.stageChangesOnPosition(
        LICENCE_CORRECTION,
        targetPositionId,
        changes
    )).isInstanceOf(LmsEntityNotFoundException.class);

    verify(licencePositionStagedPayloadRepository, never()).save(any());
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

    var result = LicencePositionStagedPayloadService.getUpdatedChangePositionIds(List.of(
        updateStagedPayloadFor(
            LICENCE_POSITION,
            UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withChanges(List.of(updatedOnExecutedPosition))
                .build()
        ),
        addStagedPayloadFor(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(addedPositionId.toString())
            .withChanges(List.of(updatedOnAddedPosition, addChange))
            .build()),
        removeStagedPayloadFor(LicencePositionTestUtil.newBuilder().build())
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

    var result = LicencePositionStagedPayloadService.withoutMovedAwayChanges(
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
    var otherPositionStagedPayload = addStagedPayloadFor(CreateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(List.of(UpdateChangeOperations.buildUpdateChange(
            movedAwayChange.getId().toString(),
            new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
        )))
        .build());

    when(licencePositionChangeService.findByLicencePositionId(LICENCE_POSITION.getId()))
        .thenReturn(List.of(remainingChange, movedAwayChange));
    givenPositionStagedPayloads(otherPositionStagedPayload);

    var result = licencePositionStagedPayloadService.getChangesForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null
    );

    assertThat(result).containsExactly(
        new PositionChange(remainingChange.getId().toString(), 1, null, List.of(remainingOperation))
    );
  }

  @Test
  void hasPendingChange_whenChangeIsPending_returnsTrue() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    var operation = LicenceOperation.newStorageOperatorOperation().withOperator(1).build();
    var change = UpdateChangeOperations.buildUpdateChange(
        UUID.randomUUID().toString(),
        operation
    );
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionStagedPayloadTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(null)
                .withChanges(List.of(change))
                .build())
            .build()));
    assertThat(licencePositionStagedPayloadService.hasPendingChange(LICENCE_POSITION, LICENCE_CORRECTION, StorageOperatorOperation.class))
        .isTrue();
  }

  @Test
  void hasPendingChange_whenChangeIsNotPending_returnsFalse() {
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionStagedPayloadChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(LicencePositionStagedPayloadTestUtil.newBuilder()
            .withTargetLicencePosition(licencePosition)
            .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
            .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
                .withEffectiveDate(null)
                .withChanges(List.of())
                .build())
            .build()));
    assertThat(licencePositionStagedPayloadService.hasPendingChange(LICENCE_POSITION, LICENCE_CORRECTION, StorageOperatorOperation.class))
        .isFalse();
  }

  private void givenExecutedTargetPosition(Optional<LicencePositionStagedPayload> updateStagedPayload) {
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndChangeType(
        LICENCE_CORRECTION,
        LicencePositionStagedPayloadChangeType.ADD_POSITION
    )).thenReturn(List.of());
    when(licencePositionService.getPositionForLicence(LICENCE, LICENCE_POSITION.getId()))
        .thenReturn(LICENCE_POSITION);
    when(licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        LicencePositionStagedPayloadChangeType.UPDATE_POSITION
    )).thenReturn(updateStagedPayload);
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

  private static LicencePositionStagedPayload updatePositionStagedPayloadWith(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }
}
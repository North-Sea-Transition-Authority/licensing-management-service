package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

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

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

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
  void removeExecutedPosition_whenPositionNotExecuted_throwsAndDoesNotSave() {
    var nonExecutedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withIsExecuted(false)
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
  void getCommittedSetEquityOperations_whenNoSetEquityChange_returnsEmpty() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    assertThat(licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection)).isEmpty();
  }

  @Test
  void getCommittedSetEquityOperations_returnsTheOperationsFromTheSetEquityChange() {
    var positionCorrection = positionCorrectionWithSetEquity(List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    assertThat(licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1, 2);
  }

  @Test
  void getSetEquityViews_mapsOperationsToViewsWithResolvedNames() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, "Org One", 2, "Org Two"));

    var views = licencePositionCorrectionService.getSetEquityViews(List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    assertThat(views)
        .extracting(SetEquityRow::organisationName, SetEquityRow::equity)
        .containsExactly(
            tuple("Org One", BigDecimal.valueOf(40)),
            tuple("Org Two", BigDecimal.valueOf(60))
        );
  }

  @Test
  void getSetEquityViews_whenNameUnknown_usesEmptyString() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1))).thenReturn(Map.of());

    var views = licencePositionCorrectionService.getSetEquityViews(List.of(setEquityOp(1, 40)));

    assertThat(views).singleElement().extracting(SetEquityRow::organisationName).isEqualTo("");
  }

  @Test
  void commitSetEquity_whenNoExistingChange_createsOneWithTheOperations() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionCorrectionService.commitSetEquity(positionCorrection, List.of(setEquityOp(1, 100)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
  }

  @Test
  void commitSetEquity_whenExistingChange_replacesItsOperations() {
    var positionCorrection = positionCorrectionWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionCorrectionService.commitSetEquity(positionCorrection, List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    var change = (AddChange) payload.changes().getFirst();
    assertThat(payload.changes()).hasSize(1);
    assertThat(change.operations()).hasSize(2);
  }

  @Test
  void commitSetEquity_whenNoOperations_dropsTheChange() {
    var positionCorrection = positionCorrectionWithSetEquity(List.of(setEquityOp(1, 40)));

    licencePositionCorrectionService.commitSetEquity(positionCorrection, List.of());

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).isEmpty();
  }

  @Test
  void commitSetEquity_retainsOtherChangeTypes() {
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

    licencePositionCorrectionService.commitSetEquity(positionCorrection, List.of(setEquityOp(1, 100)));

    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    assertThat(payload.changes()).hasSize(2);
    assertThat(licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1);
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
  void commitSetEquityForExecutedPosition_whenNoExistingCorrection_createsUpdatePositionCorrection() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    licencePositionCorrectionService.commitSetEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, List.of(setEquityOp(1, 100)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();
    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);
    assertThat(saved.getPayload()).isInstanceOf(UpdateLicencePositionPayload.class);
    assertThat(saved.getPayload().changes()).hasSize(1);
  }

  @Test
  void commitSetEquityForExecutedPosition_whenCorrectionExists_updatesExistingCorrection() {
    var existing = updatePositionCorrectionWithSetEquity(List.of(setEquityOp(1, 40)));
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(existing));

    licencePositionCorrectionService.commitSetEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    verify(licencePositionCorrectionRepository).save(existing);
    var payload = (UpdateLicencePositionPayload) existing.getPayload();
    var change = (AddChange) payload.changes().getFirst();
    assertThat(change.operations()).hasSize(2);
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
  void commitSetEquity_whenUpdatePayload_rebuildsAsUpdatePayloadPreservingType() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();

    licencePositionCorrectionService.commitSetEquity(positionCorrection, List.of(setEquityOp(1, 100)));

    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(UpdateLicencePositionPayload.class);
    assertThat(payload.changes()).hasSize(1);
  }

  @Test
  void getCommittedSetEquityOperationsForExecutedPosition_whenUpdateCorrectionExists_returnsOperations() {
    var positionCorrection = updatePositionCorrectionWithSetEquity(List.of(setEquityOp(1, 40), setEquityOp(2, 60)));
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.of(positionCorrection));

    assertThat(licencePositionCorrectionService
        .getCommittedSetEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .extracting(SetEquityOperation::transferTo)
        .containsExactly(1, 2);
  }

  @Test
  void getCommittedSetEquityOperationsForExecutedPosition_whenNoCorrection_returnsEmptyWithoutSaving() {
    when(licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        LICENCE_CORRECTION, LICENCE_POSITION, LicencePositionCorrectionChangeType.UPDATE_POSITION))
        .thenReturn(Optional.empty());

    assertThat(licencePositionCorrectionService
        .getCommittedSetEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isEmpty();
    verify(licencePositionCorrectionRepository, never()).save(any());
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

  private static LicencePositionCorrection updatePositionCorrectionWithSetEquity(List<SetEquityOperation> operations) {
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withChanges(List.of(setEquityChange(operations)))
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
  }

  private LicencePosition executedPosition(UUID id, LocalDate positionDate, int order, String reference) {
    return LicencePositionTestUtil.newBuilder()
        .withId(id)
        .withLicence(LICENCE)
        .withPositionDate(positionDate)
        .withPositionOrder(order)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference(reference).build())
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
}

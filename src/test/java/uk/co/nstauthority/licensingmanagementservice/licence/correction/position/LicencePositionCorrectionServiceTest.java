package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;

@ExtendWith(MockitoExtension.class)
class LicencePositionCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String CORRECTION_REFERENCE = "TEST-REF";

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
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();

    licencePositionCorrectionService.undoPositionCorrection(positionCorrection);

    verify(licencePositionCorrectionRepository).delete(positionCorrection);
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

  private CreateLicencePositionPayload captureSavedPayload() {
    verify(licencePositionCorrectionRepository).save(licencePositionCorrectionCaptor.capture());
    var payload = licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload).isInstanceOf(CreateLicencePositionPayload.class);
    return (CreateLicencePositionPayload) payload;
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
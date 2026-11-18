package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectionPositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withId(1).build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private CorrectionPositionService correctionPositionService;

  @Test
  void getCorrectionPositions() {
    var executedFirstJune = executedPosition(LocalDate.of(2026, Month.JUNE, 1), 1);
    var executedTenthJune = executedPosition(LocalDate.of(2026, Month.JUNE, 10), 1);
    var stagedFirstJune = stagedPosition(LocalDate.of(2026, Month.JUNE, 1), 2);
    var stagedFifthJune = stagedPosition(LocalDate.of(2026, Month.JUNE, 5), 1);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(executedFirstJune, executedTenthJune));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(LICENCE_CORRECTION))
        .thenReturn(List.of(stagedFifthJune, stagedFirstJune));

    var result = correctionPositionService.getCorrectionPositions(LICENCE_CORRECTION);

    assertThat(result).containsExactly(
        new CorrectionPosition.Executed(LICENCE_CORRECTION, executedFirstJune),
        new CorrectionPosition.Staged(stagedFirstJune),
        new CorrectionPosition.Staged(stagedFifthJune),
        new CorrectionPosition.Executed(LICENCE_CORRECTION, executedTenthJune)
    );
  }

  @Test
  void findCorrectionPosition_whenPositionStagedInCorrection_thenStagedPosition() {
    var stagedPosition = stagedPosition(LocalDate.of(2026, Month.JUNE, 1), 1);

    when(licencePositionCorrectionService
        .findFirstAddedPositionCorrection(LICENCE_CORRECTION, stagedPosition.getPositionId()))
        .thenReturn(Optional.of(stagedPosition));

    var result = correctionPositionService.findCorrectionPosition(LICENCE_CORRECTION, stagedPosition.getPositionId());

    assertThat(result).contains(new CorrectionPosition.Staged(stagedPosition));
  }

  @Test
  void findCorrectionPosition_whenExecutedPositionOnTheCorrectionLicence_thenExecutedPosition() {
    var position = LicencePositionTestUtil.newBuilder().withLicence(LICENCE).build();

    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(LICENCE_CORRECTION, position.getId()))
        .thenReturn(Optional.empty());
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));

    var result = correctionPositionService.findCorrectionPosition(LICENCE_CORRECTION, position.getId());

    assertThat(result).contains(new CorrectionPosition.Executed(LICENCE_CORRECTION, position));
  }

  @Test
  void findCorrectionPosition_whenExecutedPositionOnAnotherLicence_thenEmpty() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withId(2).build())
        .build();

    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(LICENCE_CORRECTION, position.getId()))
        .thenReturn(Optional.empty());
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));

    var result = correctionPositionService.findCorrectionPosition(LICENCE_CORRECTION, position.getId());

    assertThat(result).isEmpty();
  }

  @Test
  void findCorrectionPosition_whenNoSuchPosition_thenEmpty() {
    var positionId = UUID.randomUUID();

    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(LICENCE_CORRECTION, positionId))
        .thenReturn(Optional.empty());
    when(licencePositionService.findById(positionId)).thenReturn(Optional.empty());

    var result = correctionPositionService.findCorrectionPosition(LICENCE_CORRECTION, positionId);

    assertThat(result).isEmpty();
  }

  private static LicencePosition executedPosition(LocalDate positionDate, int positionOrder) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(positionDate)
        .withPositionOrder(positionOrder)
        .build();
  }

  private static LicencePositionCorrection stagedPosition(LocalDate effectiveDate, int effectiveDateOrder) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(effectiveDate)
            .withEffectiveDateOrder(effectiveDateOrder)
            .build())
        .build();
  }
}

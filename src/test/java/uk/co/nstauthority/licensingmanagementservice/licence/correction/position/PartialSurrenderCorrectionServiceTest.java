package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  private static LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();
  }

  @Test
  void getCommittedPartialSurrender_whenNoPartialSurrenderChange_returnsEmpty() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrender_whenPartialSurrenderChange_returnsTheOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .contains(operation);
  }

  @Test
  void getCommittedPartialSurrenderForExecutedPosition_whenNoUpdatePositionCorrection_returnsEmpty() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    assertThat(partialSurrenderCorrectionService
        .getCommittedPartialSurrenderForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderForExecutedPosition_whenUpdatePositionCorrection_returnsTheStagedOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService
        .getCommittedPartialSurrenderForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .contains(operation);
  }

  @Test
  void commitPartialSurrender_whenFeatureIds_replacesAddChangeWithTheOperation() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation));
  }

  @Test
  void commitPartialSurrenderForExecutedPosition_replacesAddChangeOnTheResolvedPositionCorrection() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);

    partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, operation);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation));
  }

  @Test
  void hasStagedPartialSurrender_whenStaged_returnsTrue() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .build()));

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).isTrue();
  }

  @Test
  void hasStagedPartialSurrender_whenNotStaged_returnsFalse() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).isFalse();
  }
}

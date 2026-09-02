package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class SubareaChangeServiceTest {

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private SubareaChangeService subareaChangeService;

  @Test
  void hasStagedSubareaChange_whenSubareaChangeStaged_returnsTrue() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operation = new SubareaOperation(UUID.randomUUID());
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    var result = subareaChangeService.hasStagedSubareaChange(positionCorrection);

    assertThat(result).isTrue();
  }

  @Test
  void hasStagedSubareaChange_whenNoSubareaChangeStaged_returnsFalse() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, SubareaOperation.class))
        .thenReturn(Optional.empty());

    var result = subareaChangeService.hasStagedSubareaChange(positionCorrection);

    assertThat(result).isFalse();
  }

  @Test
  void commitSubareaChangeForExecutedPosition_resolvesUpdatePositionCorrectionAndStagesOperation() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var positionCorrection = givenUpdatePositionCorrection(licencePosition);
    var operation = new SubareaOperation(UUID.randomUUID());

    subareaChangeService.commitSubareaChangeForExecutedPosition(LICENCE_CORRECTION, licencePosition, operation);

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, SubareaOperation.class, List.of(operation));
  }

  @Test
  void commitSubareaChange_stagesOperationOnGivenPositionCorrection() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operation = new SubareaOperation(UUID.randomUUID());

    subareaChangeService.commitSubareaChange(positionCorrection, operation);

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, SubareaOperation.class, List.of(operation));
  }

  private LicencePositionCorrection givenUpdatePositionCorrection(
      LicencePosition licencePosition
  ) {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    return positionCorrection;
  }
}

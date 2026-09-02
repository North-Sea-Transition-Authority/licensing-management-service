package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
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
  void commitSubareaChangeForExecutedPosition_whenNoExistingSubareaChangeForBlock_addsAddChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var positionCorrection = positionCorrectionWithChanges(List.of());
    var operation = new SubareaOperation(UUID.randomUUID());
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    subareaChangeService.commitSubareaChangeForExecutedPosition(LICENCE_CORRECTION, licencePosition, operation);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
  }

  @Test
  void commitSubareaChange_whenNoExistingSubareaChangeForBlock_addsAddChange() {
    var positionCorrection = positionCorrectionWithChanges(List.of());
    var operation = new SubareaOperation(UUID.randomUUID());
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    subareaChangeService.commitSubareaChange(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
  }

  @Test
  void commitSubareaChange_whenExistingSubareaChangeForSameBlock_replacesOnlyThatChange() {
    var blockFeatureId = UUID.randomUUID();
    var existingOperation = new SubareaOperation(blockFeatureId);
    var existingChange = AddChange.buildOperationsChange(List.of(existingOperation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(existingChange));
    var operation = new SubareaOperation(blockFeatureId);
    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(existingChange), SubareaOperation.class))
        .thenReturn(List.of(existingOperation));
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    subareaChangeService.commitSubareaChange(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
  }

  @Test
  void commitSubareaChange_whenExistingSubareaChangeForDifferentBlock_leavesItIntactAndAppends() {
    var otherBlockFeatureId = UUID.randomUUID();
    var otherOperation = new SubareaOperation(otherBlockFeatureId);
    var otherChange = AddChange.buildOperationsChange(List.of(otherOperation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(otherChange));
    var operation = new SubareaOperation(UUID.randomUUID());
    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(otherChange), SubareaOperation.class))
        .thenReturn(List.of(otherOperation));
    when(licencePositionCorrectionService.nextChangeOrder(List.of(otherChange))).thenReturn(2);

    subareaChangeService.commitSubareaChange(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId")
        .isEqualTo(List.of(otherChange, AddChange.buildOperationsChange(List.of(operation), 2)));
  }

  private LicencePositionCorrection positionCorrectionWithChanges(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(changes)
            .build())
        .build();
  }
}

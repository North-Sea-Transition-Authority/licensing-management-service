package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@ExtendWith(MockitoExtension.class)
class SubareaChangeServiceTest {

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private FeatureService featureService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @InjectMocks
  private SubareaChangeService subareaChangeService;

  @Test
  void commitSubareaChangeForExecutedPosition() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var positionCorrection = positionCorrectionWithChanges(List.of());
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    givenCommandJourneyCreatedForSubareas(positionCorrection);
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    var result = subareaChangeService.commitSubareaChangeForExecutedPosition(
        LICENCE_CORRECTION,
        licencePosition,
        BLOCK_FEATURE_ID
    );

    assertThat(result).isSameAs(positionCorrection);
    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId", ".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(expectedOperation()), 1)));
  }

  @Test
  void commitSubareaChange_whenSubareaHasNoFeature_createsJourneyForOnlySubareasWithFeatures() {
    var positionCorrection = positionCorrectionWithChanges(List.of());
    var subareaFeature = FeatureTestUtil.builder().build();
    when(licencePositionSpatialService.getSubareasGoingIntoChange(positionCorrection, BLOCK_FEATURE_ID, null))
        .thenReturn(List.of(
            new SubareaDetails(subareaFeature.getId(), "ALL", "A"),
            new SubareaDetails(null, "DISCOVERY AREA", "B")
        ));
    when(featureService.getFeaturesByIds(Set.of(subareaFeature.getId()))).thenReturn(List.of(subareaFeature));
    when(commandJourneyService.createAndAssignCommandJourney(List.of(subareaFeature)))
        .thenReturn(commandJourney());
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    subareaChangeService.commitSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId", ".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(expectedOperation()), 1)));
  }

  @Test
  void commitSubareaChange_whenExistingSubareaChangeForSameBlock_replacesOnlyThatChange() {
    var existingOperation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var existingChange = AddChange.buildOperationsChange(List.of(existingOperation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(existingChange));
    givenCommandJourneyCreatedForSubareas(positionCorrection);
    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(existingChange), SubareaOperation.class))
        .thenReturn(List.of(existingOperation));
    when(licencePositionCorrectionService.nextChangeOrder(List.of())).thenReturn(1);

    subareaChangeService.commitSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId", ".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(AddChange.buildOperationsChange(List.of(expectedOperation()), 1)));
  }

  @Test
  void commitSubareaChange_whenExistingSubareaChangeForDifferentBlock_leavesItIntactAndAppends() {
    var otherOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var otherChange = AddChange.buildOperationsChange(List.of(otherOperation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(otherChange));
    givenCommandJourneyCreatedForSubareas(positionCorrection);
    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(otherChange), SubareaOperation.class))
        .thenReturn(List.of(otherOperation));
    when(licencePositionCorrectionService.nextChangeOrder(List.of(otherChange))).thenReturn(2);

    subareaChangeService.commitSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*changeId", ".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(otherChange, AddChange.buildOperationsChange(List.of(expectedOperation()), 2)));
  }

  @Test
  void getSubareaOperationOrThrow_whenSubareaChangeForBlock_returnsIt() {
    var otherOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var operation = expectedOperation();
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(otherOperation), 1),
        AddChange.buildOperationsChange(List.of(operation), 2)
    );
    var positionCorrection = positionCorrectionWithChanges(changes);
    when(licencePositionCorrectionService.getAddOperationsOfType(changes, SubareaOperation.class))
        .thenReturn(List.of(otherOperation, operation));

    var result = subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getSubareaOperationOrThrow_whenNoSubareaChangeForBlock_throws() {
    var otherOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var changes = List.<LicencePositionChangeType>of(AddChange.buildOperationsChange(List.of(otherOperation), 1));
    var positionCorrection = positionCorrectionWithChanges(changes);
    when(licencePositionCorrectionService.getAddOperationsOfType(changes, SubareaOperation.class))
        .thenReturn(List.of(otherOperation));

    assertThatThrownBy(() -> subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  private void givenCommandJourneyCreatedForSubareas(LicencePositionCorrection positionCorrection) {
    var subareaFeature = FeatureTestUtil.builder().build();
    when(licencePositionSpatialService.getSubareasGoingIntoChange(positionCorrection, BLOCK_FEATURE_ID, null))
        .thenReturn(List.of(new SubareaDetails(subareaFeature.getId(), "ALL", "A")));
    when(featureService.getFeaturesByIds(Set.of(subareaFeature.getId()))).thenReturn(List.of(subareaFeature));
    when(commandJourneyService.createAndAssignCommandJourney(List.of(subareaFeature)))
        .thenReturn(commandJourney());
  }

  private static CommandJourney commandJourney() {
    var commandJourney = new CommandJourney();
    commandJourney.setId(COMMAND_JOURNEY_ID);
    return commandJourney;
  }

  private static SubareaOperation expectedOperation() {
    return new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, List.of(), List.of(), COMMAND_JOURNEY_ID);
  }

  private LicencePositionCorrection positionCorrectionWithChanges(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(changes)
            .build())
        .build();
  }
}

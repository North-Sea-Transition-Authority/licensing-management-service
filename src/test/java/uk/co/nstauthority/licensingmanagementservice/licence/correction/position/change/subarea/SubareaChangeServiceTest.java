package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaSetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
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

  @Mock
  private SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  @InjectMocks
  private SubareaChangeService subareaChangeService;

  @Captor
  private ArgumentCaptor<Predicate<SubareaOperation>> operationToReplaceCaptor;

  @Captor
  private ArgumentCaptor<List<? extends LicenceOperation>> operationsCaptor;

  @Test
  void commitSubareaChangeForExecutedPosition() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var positionCorrection = positionCorrectionWithChanges(List.of());
    var savedPositionCorrection = positionCorrectionWithChanges(List.of());
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    givenCommandJourneyCreatedForSubareas(positionCorrection);
    when(licencePositionCorrectionService.replaceAddChangeForExecutedPosition(
        eq(LICENCE_CORRECTION),
        eq(licencePosition),
        eq(positionCorrection),
        eq(SubareaOperation.class),
        operationToReplaceCaptor.capture(),
        operationsCaptor.capture()
    )).thenReturn(savedPositionCorrection);

    var result = subareaChangeService.commitSubareaChangeForExecutedPosition(
        LICENCE_CORRECTION,
        licencePosition,
        BLOCK_FEATURE_ID
    );

    assertThat(result).isSameAs(savedPositionCorrection);
    assertThat(operationsCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(List.of(expectedOperation()));
    assertThat(operationToReplaceCaptor.getValue())
        .accepts(new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of()))
        .rejects(new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
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

    subareaChangeService.commitSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection),
        eq(SubareaOperation.class),
        operationToReplaceCaptor.capture(),
        operationsCaptor.capture()
    );
    assertThat(operationsCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(List.of(expectedOperation()));
  }

  @Test
  void commitSubareaChange_whenAllSubareasHaveFeatures_thenReplacesOnlyTheBlocksSubareaChange() {
    var positionCorrection = positionCorrectionWithChanges(List.of());
    givenCommandJourneyCreatedForSubareas(positionCorrection);

    subareaChangeService.commitSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection),
        eq(SubareaOperation.class),
        operationToReplaceCaptor.capture(),
        operationsCaptor.capture()
    );
    assertThat(operationsCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(List.of(expectedOperation()));
    assertThat(operationToReplaceCaptor.getValue())
        .accepts(new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of()))
        .rejects(new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
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

    var result = subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getSubareaOperationOrThrow_whenCorrectionToLiveSubareaChangeForBlock_returnsIt() {
    var operation = expectedOperation();
    var positionCorrection = positionCorrectionWithChanges(List.of(
        UpdateChangeOperations.buildUpdateChange(UUID.randomUUID().toString(), operation)
    ));

    var result = subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getSubareaOperationOrThrow_whenNoSubareaChangeForBlock_throws() {
    var otherOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var changes = List.<LicencePositionChangeType>of(AddChange.buildOperationsChange(List.of(otherOperation), 1));
    var positionCorrection = positionCorrectionWithChanges(changes);

    assertThatThrownBy(() -> subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getOrCreateCommandJourney_whenStagedOperationHasJourney_thenReturnsIt() {
    var operation = expectedOperation();
    var addChange = AddChange.buildOperationsChange(List.of(operation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(addChange));
    when(licencePositionCorrectionService.findStagedChangeOperation(addChange, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    var result = subareaChangeService.getOrCreateCommandJourney(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(COMMAND_JOURNEY_ID);
    verifyNoInteractions(commandJourneyService);
    verify(licencePositionCorrectionService, never()).save(positionCorrection);
  }

  @Test
  void getOrCreateCommandJourney_whenRemovalOfLiveChangeOnSameBlockIsStagedFirst_thenReturnsAddedChangeJourney() {
    var operation = expectedOperation();
    var addChange = AddChange.buildOperationsChange(List.of(operation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(
        new RemoveChange(UUID.randomUUID().toString()),
        addChange
    ));
    when(licencePositionCorrectionService.findStagedChangeOperation(addChange, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    var result = subareaChangeService.getOrCreateCommandJourney(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(COMMAND_JOURNEY_ID);
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void getOrCreateCommandJourney_whenStagedUpdateHasNoJourney_thenStagesUpdateWithNewJourney() {
    var changeId = UUID.randomUUID().toString();
    var movedOperation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var orderChange = new UpdateChangeOrder(changeId, 3);
    var movedChange = UpdateChangeOperations.buildUpdateChange(changeId, movedOperation);
    var positionCorrection = positionCorrectionWithChanges(List.of(orderChange, movedChange));
    when(licencePositionCorrectionService.findStagedChangeOperation(movedChange, SubareaOperation.class))
        .thenReturn(Optional.of(movedOperation));
    givenCommandJourneyCreatedForSubareas(positionCorrection, changeId);

    var result = subareaChangeService.getOrCreateCommandJourney(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(result).isEqualTo(COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(orderChange, UpdateChangeOperations.buildUpdateChange(changeId, expectedOperation())));
  }

  @Test
  void getOrCreateCommandJourney_whenNoSubareaChangeForBlock_thenThrows() {
    var otherOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var addChange = AddChange.buildOperationsChange(List.of(otherOperation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(addChange));

    assertThatThrownBy(() -> subareaChangeService.getOrCreateCommandJourney(positionCorrection, BLOCK_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessage("No subarea change staged for block %s on position correction %s"
            .formatted(BLOCK_FEATURE_ID, positionCorrection.getId()));
  }

  @Test
  void getStagedSubareaOperationOrThrow_whenChangeIsASubareaChange_returnsItsOperation() {
    var operation = expectedOperation();
    var change = AddChange.buildOperationsChange(List.of(operation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    var result = subareaChangeService.getStagedSubareaOperationOrThrow(positionCorrection, change.changeId());

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getStagedSubareaOperationOrThrow_whenChangeIsNotASubareaChange_throws() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(2).build();
    var change = AddChange.buildOperationsChange(List.of(operation), 1);
    var changeId = change.changeId();
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> subareaChangeService.getStagedSubareaOperationOrThrow(positionCorrection, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Change with id %s is not a subarea change".formatted(changeId));
  }

  @Test
  void getLiveSubareaOperationOrThrow_whenChangeIsASubareaChange_returnsItsOperation() {
    var operation = expectedOperation();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withOperations(List.<LicenceOperation>of(operation))
        .build();

    var result = subareaChangeService.getLiveSubareaOperationOrThrow(liveChange);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getLiveSubareaOperationOrThrow_whenChangeIsNotASubareaChange_throws() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withOperations(List.<LicenceOperation>of(LicenceOperation.newAdministratorChange().withOperator(2).build()))
        .build();

    assertThatThrownBy(() -> subareaChangeService.getLiveSubareaOperationOrThrow(liveChange))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Change with id %s is not a subarea change".formatted(liveChange.getId()));
  }

  @Test
  void getBlockName() {
    var blockFeature = FeatureTestUtil.builder().withId(BLOCK_FEATURE_ID).withFeatureName("30/1").build();
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(blockFeature);

    var result = subareaChangeService.getBlockName(expectedOperation());

    assertThat(result).isEqualTo("30/1");
  }

  @Test
  void undoSubareaChange_whenCommandJourneyStarted_deletesJourneyAndDropsChange() {
    var operation = expectedOperation();
    var change = AddChange.buildOperationsChange(List.of(operation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    subareaChangeService.undoSubareaChange(positionCorrection, change.changeId());

    verify(commandJourneyService).deleteCommandJourney(COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, change.changeId());
  }

  @Test
  void undoSubareaChange_whenNoCommandJourneyStarted_onlyDropsChange() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var change = AddChange.buildOperationsChange(List.of(operation), 1);
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    subareaChangeService.undoSubareaChange(positionCorrection, change.changeId());

    verifyNoInteractions(commandJourneyService);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, change.changeId());
  }

  @Test
  void undoSubareaChange_whenRemovalOfLiveChange_thenOnlyDropsChange() {
    var change = new RemoveChange(UUID.randomUUID().toString());
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(expectedOperation()));

    subareaChangeService.undoSubareaChange(positionCorrection, change.changeId());

    verifyNoInteractions(commandJourneyService);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, change.changeId());
  }

  @Test
  void undoSubareaChange_whenCorrectionToLiveChange_thenDeletesJourneyAndDropsChange() {
    var operation = expectedOperation();
    var change = UpdateChangeOperations.buildUpdateChange(UUID.randomUUID().toString(), operation);
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    subareaChangeService.undoSubareaChange(positionCorrection, change.changeId());

    verify(commandJourneyService).deleteCommandJourney(COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, change.changeId());
  }

  @Test
  void correctExistingSubareaChange_whenNotCorrectedYet_thenStagesUpdateWithJourneyFromSubareasGoingIntoChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = liveSubareaChange();
    var liveChangeId = liveChange.getId().toString();
    var otherChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        1
    );
    var positionCorrection = positionCorrectionWithChanges(List.of(otherChange));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.findStagedChange(positionCorrection, liveChangeId))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedForSubareas(positionCorrection, liveChangeId);

    var result = subareaChangeService.correctExistingSubareaChange(LICENCE_CORRECTION, licencePosition, liveChange);

    assertThat(result).isSameAs(positionCorrection);
    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(otherChange, UpdateChangeOperations.buildUpdateChange(liveChangeId, expectedOperation())));
  }

  @Test
  void correctExistingSubareaChange_whenStagedCorrectionHasNoJourney_thenReplacesItWithNewJourney() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = liveSubareaChange();
    var liveChangeId = liveChange.getId().toString();
    var movedOperation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var movedChange = UpdateChangeOperations.buildUpdateChange(liveChangeId, movedOperation);
    var positionCorrection = positionCorrectionWithChanges(List.of(movedChange));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.findStagedChange(positionCorrection, liveChangeId))
        .thenReturn(Optional.of(movedChange));
    when(licencePositionCorrectionService.findStagedChangeOperation(movedChange, SubareaOperation.class))
        .thenReturn(Optional.of(movedOperation));
    givenCommandJourneyCreatedForSubareas(positionCorrection, liveChangeId);

    subareaChangeService.correctExistingSubareaChange(LICENCE_CORRECTION, licencePosition, liveChange);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(UpdateChangeOperations.buildUpdateChange(liveChangeId, expectedOperation())));
  }

  @Test
  void correctExistingSubareaChange_whenStagedCorrectionHasLiveChangeJourney_thenReplacesItWithNewJourney() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var liveJourneyId = UUID.randomUUID();
    var liveOperation = new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, List.of(), List.of(), liveJourneyId);
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withOperations(List.<LicenceOperation>of(liveOperation))
        .build();
    var liveChangeId = liveChange.getId().toString();
    var movedChange = UpdateChangeOperations.buildUpdateChange(liveChangeId, liveOperation);
    var positionCorrection = positionCorrectionWithChanges(List.of(movedChange));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.findStagedChange(positionCorrection, liveChangeId))
        .thenReturn(Optional.of(movedChange));
    when(licencePositionCorrectionService.findStagedChangeOperation(movedChange, SubareaOperation.class))
        .thenReturn(Optional.of(liveOperation));
    givenCommandJourneyCreatedForSubareas(positionCorrection, liveChangeId);

    subareaChangeService.correctExistingSubareaChange(LICENCE_CORRECTION, licencePosition, liveChange);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(UpdateChangeOperations.buildUpdateChange(liveChangeId, expectedOperation())));
  }

  @Test
  void correctExistingSubareaChange_whenStagedCorrectionHasItsOwnJourney_thenKeepsIt() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var liveChange = liveSubareaChange();
    var liveChangeId = liveChange.getId().toString();
    var operation = expectedOperation();
    var stagedChange = UpdateChangeOperations.buildUpdateChange(liveChangeId, operation);
    var positionCorrection = positionCorrectionWithChanges(List.of(stagedChange));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, licencePosition))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.findStagedChange(positionCorrection, liveChangeId))
        .thenReturn(Optional.of(stagedChange));
    when(licencePositionCorrectionService.findStagedChangeOperation(stagedChange, SubareaOperation.class))
        .thenReturn(Optional.of(operation));

    var result = subareaChangeService.correctExistingSubareaChange(LICENCE_CORRECTION, licencePosition, liveChange);

    assertThat(result).isSameAs(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).isEqualTo(List.of(stagedChange));
    verifyNoInteractions(commandJourneyService);
    verify(licencePositionCorrectionService, never()).save(positionCorrection);
  }

  @Test
  void completeSubareaChange_whenSubareaSplit_recordsSubareasOnTheSameChangeAndCascadesToEachOutput() {
    var subareaA = subareaFeature("A");
    var subareaB = subareaFeature("B");
    var subareaA1 = subareaFeature("A1");
    var subareaA2 = subareaFeature("A2");
    var change = AddChange.buildOperationsChange(List.of(expectedOperation()), 1);
    var otherBlockChange = AddChange.buildOperationsChange(
        List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of())),
        2
    );
    var positionCorrection = positionCorrectionWithChanges(List.of(change, otherBlockChange));
    var stateGoingIntoChange = stateWithSubareas(subareaA, subareaB);
    var stateWithoutChange = stateGoingIntoChange
        .withSubareaEquity(BLOCK_FEATURE_ID, SubareaDetails.from(subareaA), 1, BigDecimal.TEN);
    givenCompletedJourney(
        positionCorrection,
        change,
        stateGoingIntoChange,
        stateWithoutChange,
        Map.of(
            subareaA1.getId(), Set.of(subareaA.getId()),
            subareaA2.getId(), Set.of(subareaA.getId())
        ),
        List.of(subareaA1, subareaA2)
    );

    subareaChangeService.completeSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(
            new AddChange(
                change.changeId(),
                1,
                AddChange.buildOperationsChange(
                    List.of(completedOperation(List.of(subareaA), List.of(subareaA1, subareaA2))),
                    1
                ).operations()
            ),
            otherBlockChange
        ));
    verify(subareaSetEquityCorrectionService).cascadeSubareaEquity(
        positionCorrection,
        stateWithoutChange,
        BLOCK_FEATURE_ID,
        List.of(SubareaDetails.from(subareaA)),
        SubareaDetails.from(subareaA1)
    );
    verify(subareaSetEquityCorrectionService).cascadeSubareaEquity(
        positionCorrection,
        stateWithoutChange,
        BLOCK_FEATURE_ID,
        List.of(SubareaDetails.from(subareaA)),
        SubareaDetails.from(subareaA2)
    );
  }

  @Test
  void completeSubareaChange_whenCorrectingLiveChangeWithMerges_keepsLiveChangeIdAndCascadesEachOutputsOriginals() {
    var subareaA = subareaFeature("A");
    var subareaB = subareaFeature("B");
    var subareaC = subareaFeature("C");
    var subareaA1B = subareaFeature("A1B");
    var subareaA2C = subareaFeature("A2C");
    var liveChangeId = UUID.randomUUID().toString();
    var change = UpdateChangeOperations.buildUpdateChange(liveChangeId, expectedOperation());
    var positionCorrection = positionCorrectionWithChanges(List.of(change));
    var stateGoingIntoChange = stateWithSubareas(subareaA, subareaB, subareaC);
    var stateWithoutChange = stateGoingIntoChange
        .withSubareaEquity(BLOCK_FEATURE_ID, SubareaDetails.from(subareaB), 1, BigDecimal.TEN);
    givenCompletedJourney(
        positionCorrection,
        change,
        stateGoingIntoChange,
        stateWithoutChange,
        Map.of(
            subareaA1B.getId(), Set.of(subareaA.getId(), subareaB.getId()),
            subareaA2C.getId(), Set.of(subareaA.getId(), subareaC.getId())
        ),
        List.of(subareaA1B, subareaA2C)
    );

    subareaChangeService.completeSubareaChange(positionCorrection, BLOCK_FEATURE_ID);

    assertThat(positionCorrection.getPayload().changes())
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*operationId", ".*operation\\.id")
        .isEqualTo(List.of(UpdateChangeOperations.buildUpdateChange(
            liveChangeId,
            completedOperation(List.of(subareaA, subareaB, subareaC), List.of(subareaA1B, subareaA2C))
        )));
    verify(subareaSetEquityCorrectionService).cascadeSubareaEquity(
        positionCorrection,
        stateWithoutChange,
        BLOCK_FEATURE_ID,
        List.of(SubareaDetails.from(subareaA), SubareaDetails.from(subareaB)),
        SubareaDetails.from(subareaA1B)
    );
    verify(subareaSetEquityCorrectionService).cascadeSubareaEquity(
        positionCorrection,
        stateWithoutChange,
        BLOCK_FEATURE_ID,
        List.of(SubareaDetails.from(subareaA), SubareaDetails.from(subareaC)),
        SubareaDetails.from(subareaA2C)
    );
  }

  private void givenCompletedJourney(
      LicencePositionCorrection positionCorrection,
      LicencePositionChangeType change,
      LicencePositionState stateGoingIntoChange,
      LicencePositionState stateWithoutChange,
      Map<UUID, Set<UUID>> initialFeatureIdsByCreatedFeatureId,
      List<Feature> createdFeatures
  ) {
    when(licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .thenReturn(Optional.of(expectedOperation()));
    when(licencePositionSpatialService.getStateGoingIntoChange(positionCorrection, change.changeId()))
        .thenReturn(stateGoingIntoChange);
    when(licencePositionSpatialService.getStateAtEndOfPositionWithoutChange(positionCorrection, change.changeId()))
        .thenReturn(stateWithoutChange);
    when(commandJourneyService.getInitialFeatureIdsByCreatedFeatureId(COMMAND_JOURNEY_ID))
        .thenReturn(initialFeatureIdsByCreatedFeatureId);
    when(featureService.getFeaturesByIds(initialFeatureIdsByCreatedFeatureId.keySet())).thenReturn(createdFeatures);
  }

  private static LicencePositionState stateWithSubareas(Feature... subareas) {
    var subareaDetails = Arrays.stream(subareas)
        .map(SubareaDetails::from)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return new LicencePositionState(
        null,
        null,
        Set.of(),
        Map.of(),
        Map.of(BLOCK_FEATURE_ID, subareaDetails),
        Map.of()
    );
  }

  private static Feature subareaFeature(String name) {
    return FeatureTestUtil.subareaFeature(UUID.randomUUID(), name);
  }

  private static SubareaOperation completedOperation(
      List<Feature> replacedSubareas,
      List<Feature> outputSubareas
  ) {
    return new SubareaOperation(
        UUID.randomUUID(),
        BLOCK_FEATURE_ID,
        replacedSubareas.stream().map(SubareaDetails::from).toList(),
        outputSubareas.stream().map(SubareaDetails::from).toList(),
        COMMAND_JOURNEY_ID
    );
  }

  private static LicencePositionChange liveSubareaChange() {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withOperations(List.<LicenceOperation>of(new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of())))
        .build();
  }

  private void givenCommandJourneyCreatedForSubareas(LicencePositionCorrection positionCorrection) {
    givenCommandJourneyCreatedForSubareas(positionCorrection, null);
  }

  private void givenCommandJourneyCreatedForSubareas(LicencePositionCorrection positionCorrection, String changeId) {
    var subareaFeature = FeatureTestUtil.builder().build();
    when(licencePositionSpatialService.getSubareasGoingIntoChange(positionCorrection, BLOCK_FEATURE_ID, changeId))
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

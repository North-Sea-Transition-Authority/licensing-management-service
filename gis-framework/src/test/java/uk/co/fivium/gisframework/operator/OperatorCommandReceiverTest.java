package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.CommandJourneyTestUtil;
import uk.co.fivium.gisframework.command.CommandStatus;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
import uk.co.fivium.gisframework.command.OperatorCommand;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.OperatorCommandTestUtil;
import uk.co.fivium.gisframework.command.TransformationType;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@ExtendWith(MockitoExtension.class)
class OperatorCommandReceiverTest {

  private static final List<List<List<BigDecimal>>> CUTTER_LINE_ORIGINAL_SRS_COORDINATES = List.of(
      List.of(
          List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.2)),
          List.of(BigDecimal.valueOf(3.3), BigDecimal.valueOf(4.4))
      )
  );

  private static final String CUTTER_LINE_ESRI_JSON = "dummy cutter line esriJson";

  @Mock
  private SplitOperatorService splitOperatorService;

  @Mock
  private FeatureService featureService;

  @Mock
  private FeatureJourneyStateService featureJourneyStateService;

  @Mock
  private OperatorCommandService operatorCommandService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private MergeOperatorService mergeOperatorService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private StrataSplitOperatorService strataSplitOperatorService;

  @InjectMocks
  private OperatorCommandReceiver operatorCommandReceiver;

  @Test
  void executeSplit_whenFeatureIsSplit_deactivatesInputAndActivatesOutput() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, null);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();

    var outputFeature1 = FeatureTestUtil.newBuilder().build();
    var outputFeature2 = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON, null))
        .thenReturn(List.of(outputFeature1, outputFeature2));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).containsExactly(outputFeature1, outputFeature2);

    verify(featureJourneyStateService).deactivateFeatures(commandJourney, List.of(inputFeature));
    verify(featureJourneyStateService)
        .createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(outputFeature1, outputFeature2));
  }

  @Test
  void executeSplit_whenUndoneCommandsExist_clearsUndoStackBeforeCreatingNewCommand() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, null);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var outputFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    var undoneCommand = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();
    var orphanedFeature = FeatureTestUtil.newBuilder().build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON, null))
        .thenReturn(List.of(outputFeature));
    when(operatorCommandService.getUndoneCommands(commandJourney)).thenReturn(List.of(undoneCommand));
    when(featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(List.of(undoneCommand)))
        .thenReturn(List.of(orphanedFeature));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    operatorCommandReceiver.executeSplit(request);

    verify(featureJourneyStateService).deleteFeatureJourneyStatesCreatedByCommands(List.of(undoneCommand));
    verify(featureService).deleteAll(List.of(orphanedFeature));
    verify(operatorCommandService).deleteCommands(List.of(undoneCommand));
  }

  @Test
  void executeSplit_whenNoUndoneCommandsExist_doesNotClearUndoStack() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, null);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var outputFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON, null))
        .thenReturn(List.of(outputFeature));
    when(operatorCommandService.getUndoneCommands(commandJourney)).thenReturn(List.of());
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    operatorCommandReceiver.executeSplit(request);

    verify(featureJourneyStateService, never()).deleteFeatureJourneyStatesCreatedByCommands(any());
    verify(featureService, never()).deleteAll(any());
    verify(operatorCommandService, never()).deleteCommands(any());
  }

  @Test
  void executeSplit_whenNoFeatureIsSplit_returnsEmptyListAndDoesNotRecordCommand() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, null);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON, null)).thenReturn(List.of());

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).isEmpty();
    verify(operatorCommandService, never()).createOperatorCommand(any(), any(), any());
    verify(featureJourneyStateService, never()).deactivateFeatures(any(), any());
    verify(featureJourneyStateService, never()).createFeatureJourneyStatesForCommandOutput(any(), any(), any());
  }

  @Test
  void executeSplit_whenDepthProvided_onlySplitsFeaturesCoveringDepth() {
    var commandJourneyId = UUID.randomUUID();
    var depth = -150L;
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, depth);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var shallowFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var deepFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var shallowPolygon = PolygonTestUtil.newBuilder()
        .withFeature(shallowFeature)
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();
    var deepPolygon = PolygonTestUtil.newBuilder()
        .withFeature(deepFeature)
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();

    var outputFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    var activeFeatures = List.of(shallowFeature, deepFeature);
    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(activeFeatures);
    when(polygonService.getPolygons(activeFeatures)).thenReturn(List.of(shallowPolygon, deepPolygon));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(deepFeature, CUTTER_LINE_ESRI_JSON, depth)).thenReturn(List.of(outputFeature));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(deepFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).containsExactly(outputFeature);
    verify(splitOperatorService, never()).splitPolygon(shallowFeature, CUTTER_LINE_ESRI_JSON, depth);
    verify(featureJourneyStateService).deactivateFeatures(commandJourney, List.of(deepFeature));
    verify(featureJourneyStateService)
        .createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(outputFeature));
  }

  @Test
  void executeSplit_whenNoFeatureCoversDepth_returnsEmptyListAndDoesNotSplit() {
    var commandJourneyId = UUID.randomUUID();
    var depth = -150L;
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId, depth);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var shallowFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var shallowPolygon = PolygonTestUtil.newBuilder()
        .withFeature(shallowFeature)
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(shallowFeature));
    when(polygonService.getPolygons(List.of(shallowFeature))).thenReturn(List.of(shallowPolygon));

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).isEmpty();
    verifyNoInteractions(grpcClientService, splitOperatorService, operatorCommandService);
  }

  @Test
  void executeMerge_whenFeaturesMerged_deactivatesInputsAndActivatesOutput() {
    var commandJourneyId = UUID.randomUUID();
    var inputFeature1 = FeatureTestUtil.newBuilder().build();
    var inputFeature2 = FeatureTestUtil.newBuilder().build();
    var request = new MergeFromMapRequest(
        List.of(inputFeature1.getId(), inputFeature2.getId()), commandJourneyId);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney))
        .thenReturn(List.of(inputFeature1, inputFeature2));
    when(mergeOperatorService.mergePolygons(List.of(inputFeature1, inputFeature2))).thenReturn(mergedFeature);
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature1.getId(), inputFeature2.getId()), TransformationType.MERGE))
        .thenReturn(command);

    var result = operatorCommandReceiver.executeMerge(request);

    assertThat(result).contains(mergedFeature);
    verify(featureJourneyStateService).deactivateFeatures(commandJourney, List.of(inputFeature1, inputFeature2));
    verify(featureJourneyStateService)
        .createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(mergedFeature));
  }

  @Test
  void executeMerge_whenARequestedFeatureIsNotActiveInJourney_throwsAndRecordsNothing() {
    var commandJourneyId = UUID.randomUUID();
    var activeFeature = FeatureTestUtil.newBuilder().build();
    var staleFeatureId = UUID.randomUUID();
    var request = new MergeFromMapRequest(List.of(activeFeature.getId(), staleFeatureId), commandJourneyId);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(activeFeature));

    assertThatThrownBy(() -> operatorCommandReceiver.executeMerge(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features must be active members of command journey %s".formatted(commandJourneyId));

    verifyNoInteractions(mergeOperatorService);
    verify(operatorCommandService, never()).createOperatorCommand(any(), any(), any());
    verify(featureJourneyStateService, never()).deactivateFeatures(any(), any());
    verify(featureJourneyStateService, never()).createFeatureJourneyStatesForCommandOutput(any(), any(), any());
  }

  @Test
  void executeMerge_whenNoFeatureIds_returnsEmptyAndRecordsNothing() {
    var request = new MergeFromMapRequest(List.of(), UUID.randomUUID());

    var result = operatorCommandReceiver.executeMerge(request);

    assertThat(result).isEmpty();
    verifyNoInteractions(commandJourneyService, featureService, mergeOperatorService, operatorCommandService);
    verify(featureJourneyStateService, never()).deactivateFeatures(any(), any());
    verify(featureJourneyStateService, never()).createFeatureJourneyStatesForCommandOutput(any(), any(), any());
  }

  @Test
  void executeStrataSplit_whenSomeFeaturesSplit_deactivatesOnlySplitInputsAndActivatesOutputs() {
    var commandJourneyId = UUID.randomUUID();
    var depth = -2000L;
    var splitFeature = FeatureTestUtil.newBuilder().build();
    var unsplitFeature = FeatureTestUtil.newBuilder().build();
    var request = new StrataSplitFromMapRequest(
        List.of(splitFeature.getId(), unsplitFeature.getId()), commandJourneyId, depth);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var aboveFeature = FeatureTestUtil.newBuilder().build();
    var belowFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(splitFeature, unsplitFeature));
    when(strataSplitOperatorService.strataSplitPolygon(splitFeature, depth))
        .thenReturn(List.of(aboveFeature, belowFeature));
    when(strataSplitOperatorService.strataSplitPolygon(unsplitFeature, depth)).thenReturn(List.of());
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(splitFeature.getId()), TransformationType.STRATA_SPLIT)).thenReturn(command);

    var result = operatorCommandReceiver.executeStrataSplit(request);

    assertThat(result).containsExactly(aboveFeature, belowFeature);
    verify(featureJourneyStateService).deactivateFeatures(commandJourney, List.of(splitFeature));
    verify(featureJourneyStateService)
        .createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(aboveFeature, belowFeature));
  }

  @Test
  void executeStrataSplit_whenUndoneCommandsExist_clearsUndoStackBeforeCreatingNewCommand() {
    var commandJourneyId = UUID.randomUUID();
    var depth = -2000L;
    var inputFeature = FeatureTestUtil.newBuilder().build();
    var request = new StrataSplitFromMapRequest(List.of(inputFeature.getId()), commandJourneyId, depth);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var aboveFeature = FeatureTestUtil.newBuilder().build();
    var belowFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();
    var undoneCommand = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();
    var orphanedFeature = FeatureTestUtil.newBuilder().build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(strataSplitOperatorService.strataSplitPolygon(inputFeature, depth))
        .thenReturn(List.of(aboveFeature, belowFeature));
    when(operatorCommandService.getUndoneCommands(commandJourney)).thenReturn(List.of(undoneCommand));
    when(featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(List.of(undoneCommand)))
        .thenReturn(List.of(orphanedFeature));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.STRATA_SPLIT)).thenReturn(command);

    operatorCommandReceiver.executeStrataSplit(request);

    verify(featureService).deleteAll(List.of(orphanedFeature));
    verify(operatorCommandService).deleteCommands(List.of(undoneCommand));
  }

  @Test
  void executeStrataSplit_whenNoFeatureIsSplit_returnsEmptyListAndRecordsNothing() {
    var commandJourneyId = UUID.randomUUID();
    var depth = -2000L;
    var inputFeature = FeatureTestUtil.newBuilder().build();
    var request = new StrataSplitFromMapRequest(List.of(inputFeature.getId()), commandJourneyId, depth);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(strataSplitOperatorService.strataSplitPolygon(inputFeature, depth)).thenReturn(List.of());

    var result = operatorCommandReceiver.executeStrataSplit(request);

    assertThat(result).isEmpty();
    verifyNoInteractions(operatorCommandService);
    verify(featureJourneyStateService, never()).deactivateFeatures(any(), any());
    verify(featureJourneyStateService, never()).createFeatureJourneyStatesForCommandOutput(any(), any(), any());
  }

  @Test
  void executeStrataSplit_whenNoFeatureIds_returnsEmptyListAndRecordsNothing() {
    var request = new StrataSplitFromMapRequest(List.of(), UUID.randomUUID(), -2000L);

    var result = operatorCommandReceiver.executeStrataSplit(request);

    assertThat(result).isEmpty();
    verifyNoInteractions(
        commandJourneyService, strataSplitOperatorService, operatorCommandService, featureJourneyStateService);
  }

  @Test
  void executeStrataSplit_whenNoDepth_throwsAndRecordsNothing() {
    var request = new StrataSplitFromMapRequest(List.of(UUID.randomUUID()), UUID.randomUUID(), null);

    assertThatThrownBy(() -> operatorCommandReceiver.executeStrataSplit(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A depth must be provided to strata split");

    verifyNoInteractions(
        commandJourneyService, strataSplitOperatorService, operatorCommandService, featureJourneyStateService);
  }

  @Test
  void executeStrataSplit_whenARequestedFeatureIsNotActiveInJourney_throwsAndRecordsNothing() {
    var commandJourneyId = UUID.randomUUID();
    var activeFeature = FeatureTestUtil.newBuilder().build();
    var request = new StrataSplitFromMapRequest(
        List.of(activeFeature.getId(), UUID.randomUUID()), commandJourneyId, -2000L);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(activeFeature));

    assertThatThrownBy(() -> operatorCommandReceiver.executeStrataSplit(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features must be active members of command journey %s".formatted(commandJourneyId));

    verifyNoInteractions(strataSplitOperatorService, operatorCommandService, featureJourneyStateService);
  }

  @Test
  void undo_whenActiveCommandExists_deactivatesOutputAndActivatesInput() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var inputFeature = FeatureTestUtil.newBuilder().build();
    var activeCommand = OperatorCommandTestUtil.newBuilder()
        .withInputFeatureIds(Set.of(inputFeature.getId()))
        .build();

    when(operatorCommandService.getCurrentActiveCommand(commandJourney)).thenReturn(Optional.of(activeCommand));
    when(featureService.getFeaturesByIds(activeCommand.getInputFeatureIds())).thenReturn(List.of(inputFeature));

    var result = operatorCommandReceiver.undo(commandJourney);

    assertThat(result).containsExactly(inputFeature);
    verify(featureJourneyStateService).deactivateFeaturesCreatedByCommand(activeCommand);
    verify(featureJourneyStateService).activateFeatures(commandJourney, List.of(inputFeature));
    verify(operatorCommandService).markUndone(activeCommand);
  }

  @Test
  void undo_whenNoActiveCommandExists_returnsEmptyListAndDoesNothing() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    when(operatorCommandService.getCurrentActiveCommand(commandJourney)).thenReturn(Optional.empty());

    var result = operatorCommandReceiver.undo(commandJourney);

    assertThat(result).isEmpty();
    verify(featureJourneyStateService, never()).deactivateFeaturesCreatedByCommand(any());
    verify(featureJourneyStateService, never()).activateFeatures(any(), any());
    verify(operatorCommandService, never()).markUndone(any());
    verifyNoInteractions(featureService);
  }

  @Test
  void redo_whenUndoneCommandExists_deactivatesInputAndActivatesOutput() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var inputFeature = FeatureTestUtil.newBuilder().build();
    var outputFeature = FeatureTestUtil.newBuilder().build();
    var undoneCommand = OperatorCommandTestUtil.newBuilder()
        .withInputFeatureIds(Set.of(inputFeature.getId()))
        .build();

    when(operatorCommandService.getNextRedoCommand(commandJourney)).thenReturn(Optional.of(undoneCommand));
    when(featureService.getFeaturesByIds(undoneCommand.getInputFeatureIds())).thenReturn(List.of(inputFeature));
    when(featureJourneyStateService.activateFeaturesCreatedByCommand(undoneCommand)).thenReturn(List.of(outputFeature));

    var result = operatorCommandReceiver.redo(commandJourney);

    assertThat(result).containsExactly(outputFeature);
    verify(featureJourneyStateService).deactivateFeatures(commandJourney, List.of(inputFeature));
    verify(featureJourneyStateService).activateFeaturesCreatedByCommand(undoneCommand);
    verify(operatorCommandService).markRedone(undoneCommand);
  }

  @Test
  void redo_whenNoUndoneCommandExists_returnsEmptyListAndDoesNothing() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    when(operatorCommandService.getNextRedoCommand(commandJourney)).thenReturn(Optional.empty());

    var result = operatorCommandReceiver.redo(commandJourney);

    assertThat(result).isEmpty();
    verify(featureJourneyStateService, never()).deactivateFeatures(any(), any());
    verify(featureJourneyStateService, never()).activateFeaturesCreatedByCommand(any());
    verify(operatorCommandService, never()).markRedone(any());
    verifyNoInteractions(featureService);
  }
}

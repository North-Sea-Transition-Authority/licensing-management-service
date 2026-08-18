package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
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

  @InjectMocks
  private OperatorCommandReceiver operatorCommandReceiver;

  @Test
  void executeSplit_whenFeatureIsSplit_deactivatesInputAndActivatesOutput() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();

    var outputFeature1 = FeatureTestUtil.newBuilder().build();
    var outputFeature2 = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON))
        .thenReturn(List.of(outputFeature1, outputFeature2));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).containsExactly(outputFeature1, outputFeature2);

    verify(featureJourneyStateService).deactivateFeatures(List.of(inputFeature));
    verify(featureJourneyStateService)
        .createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(outputFeature1, outputFeature2));
  }

  @Test
  void executeSplit_whenUndoneCommandsExist_clearsUndoStackBeforeCreatingNewCommand() {
    var commandJourneyId = UUID.randomUUID();
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId);

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
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON))
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
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var outputFeature = FeatureTestUtil.newBuilder().build();
    var command = new OperatorCommand();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON))
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
    var request = new SplitFromMapRequest(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, commandJourneyId);

    var commandJourney = CommandJourneyTestUtil.newBuilder().withId(commandJourneyId).build();
    var inputFeature = FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(commandJourneyService.getActiveFeatures(commandJourney)).thenReturn(List.of(inputFeature));
    when(grpcClientService.convertPointsToPolyline(CUTTER_LINE_ORIGINAL_SRS_COORDINATES, 4230))
        .thenReturn(CUTTER_LINE_ESRI_JSON);
    when(splitOperatorService.splitPolygon(inputFeature, CUTTER_LINE_ESRI_JSON)).thenReturn(List.of());

    var result = operatorCommandReceiver.executeSplit(request);

    assertThat(result).isEmpty();
    verify(operatorCommandService, never()).createOperatorCommand(any(), any(), any());
    verify(featureJourneyStateService, never()).deactivateFeatures(any());
    verify(featureJourneyStateService, never()).createFeatureJourneyStatesForCommandOutput(any(), any(), any());
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
    verify(featureJourneyStateService).activateFeatures(List.of(inputFeature));
    verify(operatorCommandService).markUndone(activeCommand);
  }

  @Test
  void undo_whenNoActiveCommandExists_returnsEmptyListAndDoesNothing() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    when(operatorCommandService.getCurrentActiveCommand(commandJourney)).thenReturn(Optional.empty());

    var result = operatorCommandReceiver.undo(commandJourney);

    assertThat(result).isEmpty();
    verify(featureJourneyStateService, never()).deactivateFeaturesCreatedByCommand(any());
    verify(featureJourneyStateService, never()).activateFeatures(any());
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
    verify(featureJourneyStateService).deactivateFeatures(List.of(inputFeature));
    verify(featureJourneyStateService).activateFeaturesCreatedByCommand(undoneCommand);
    verify(operatorCommandService).markRedone(undoneCommand);
  }

  @Test
  void redo_whenNoUndoneCommandExists_returnsEmptyListAndDoesNothing() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    when(operatorCommandService.getNextRedoCommand(commandJourney)).thenReturn(Optional.empty());

    var result = operatorCommandReceiver.redo(commandJourney);

    assertThat(result).isEmpty();
    verify(featureJourneyStateService, never()).deactivateFeatures(any());
    verify(featureJourneyStateService, never()).activateFeaturesCreatedByCommand(any());
    verify(operatorCommandService, never()).markRedone(any());
    verifyNoInteractions(featureService);
  }
}

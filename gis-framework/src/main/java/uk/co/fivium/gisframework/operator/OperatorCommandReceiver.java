package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.TransformationType;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class OperatorCommandReceiver {

  private final SplitOperatorService splitOperatorService;
  private final FeatureService featureService;
  private final FeatureJourneyStateService featureJourneyStateService;
  private final OperatorCommandService operatorCommandService;
  private final CommandJourneyService commandJourneyService;
  private final GrpcClientService grpcClientService;

  public OperatorCommandReceiver(
      SplitOperatorService splitOperatorService,
      FeatureService featureService,
      FeatureJourneyStateService featureJourneyStateService,
      OperatorCommandService operatorCommandService,
      CommandJourneyService commandJourneyService,
      GrpcClientService grpcClientService) {
    this.splitOperatorService = splitOperatorService;
    this.featureService = featureService;
    this.featureJourneyStateService = featureJourneyStateService;
    this.operatorCommandService = operatorCommandService;
    this.commandJourneyService = commandJourneyService;
    this.grpcClientService = grpcClientService;
  }

  /**
   * Splits each of the given active features for a command journey using the cutter line, recording a
   * {@link TransformationType#SPLIT} command against their shared journey. Features that the cutter line actually
   * crossed are deactivated in favour of the new features it produces; features it didn't cross are left untouched.
   *
   * @param request Contains the command journey and cutter line coordinates
   * @return the newly created features, or an empty list if the cutter line didn't cross any input feature.
   */
  @Transactional
  public List<Feature> executeSplit(SplitFromMapRequest request) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(request.commandJourneyId());
    var inputFeatures = commandJourneyService.getActiveFeatures(commandJourney);

    if (CollectionUtils.isEmpty(inputFeatures)) {
      return List.of();
    }

    int wkid = CoordinateSystemUtils.getWkid(inputFeatures.getFirst().getCoordinateSystem());
    String cutterLineEsriJson = grpcClientService
        .convertPointsToPolyline(request.cutterLineOriginalSrsCoordinates(), wkid);
    List<Feature> affectedInputFeatures = new ArrayList<>();
    List<Feature> outputFeatures = new ArrayList<>();

    for (Feature inputFeature : inputFeatures) {
      List<Feature> splitResult = splitOperatorService.splitPolygon(inputFeature, cutterLineEsriJson);
      if (!splitResult.isEmpty()) {
        affectedInputFeatures.add(inputFeature);
        outputFeatures.addAll(splitResult);
      }
    }

    if (outputFeatures.isEmpty()) {
      return List.of();
    }
    clearUndoStack(commandJourney);

    var affectedInputFeatureIds = affectedInputFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var command = operatorCommandService.createOperatorCommand(commandJourney, affectedInputFeatureIds,
        TransformationType.SPLIT);

    featureJourneyStateService.deactivateFeatures(affectedInputFeatures);
    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(commandJourney, command, outputFeatures);

    return outputFeatures;
  }

  /**
   * Undoes the most recent active command in the given journey: the features it produced are deactivated and the
   * features it consumed are reactivated.
   *
   * @param commandJourney the journey to undo the latest command for.
   * @return the reactivated input features, or an empty list if the journey has no active command to undo.
   */
  @Transactional
  public List<Feature> undo(CommandJourney commandJourney) {
    var currentActiveCommandOpt = operatorCommandService.getCurrentActiveCommand(commandJourney);

    if (currentActiveCommandOpt.isEmpty()) {
      return List.of();
    }

    var currentActiveCommand = currentActiveCommandOpt.get();

    featureJourneyStateService.deactivateFeaturesCreatedByCommand(currentActiveCommand);

    var inputFeatures = featureService.getFeaturesByIds(currentActiveCommand.getInputFeatureIds());
    featureJourneyStateService.activateFeatures(inputFeatures);

    operatorCommandService.markUndone(currentActiveCommand);

    return inputFeatures;
  }

  /**
   * Redoes the earliest undone command in the given journey: the features it consumed are deactivated and the
   * features it produced are reactivated.
   *
   * @param commandJourney the journey to redo the next undone command for.
   * @return the reactivated output features, or an empty list if the journey has no undone command to redo.
   */
  @Transactional
  public List<Feature> redo(CommandJourney commandJourney) {
    var nextRedoCommandOpt = operatorCommandService.getNextRedoCommand(commandJourney);

    if (nextRedoCommandOpt.isEmpty()) {
      return List.of();
    }

    var nextRedoCommand = nextRedoCommandOpt.get();
    var inputFeatures = featureService.getFeaturesByIds(nextRedoCommand.getInputFeatureIds());
    featureJourneyStateService.deactivateFeatures(inputFeatures);
    var outputFeatures = featureJourneyStateService.activateFeaturesCreatedByCommand(nextRedoCommand);
    operatorCommandService.markRedone(nextRedoCommand);
    return outputFeatures;
  }

  /**
   * Hard-deletes all commands with status UNDONE and their output features.
   *
   * @param commandJourney the journey whose undone commands should be discarded.
   */
  private void clearUndoStack(CommandJourney commandJourney) {
    var undoneCommands = operatorCommandService.getUndoneCommands(commandJourney);
    if (undoneCommands.isEmpty()) {
      return;
    }
    var orphanedFeatures = featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(undoneCommands);
    featureService.deleteAll(orphanedFeatures);
    operatorCommandService.deleteCommands(undoneCommands);
  }
}

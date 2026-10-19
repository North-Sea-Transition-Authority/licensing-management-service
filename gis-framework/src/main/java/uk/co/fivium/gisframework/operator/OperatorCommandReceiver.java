package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class OperatorCommandReceiver {

  private final SplitOperatorService splitOperatorService;
  private final MergeOperatorService mergeOperatorService;
  private final StrataSplitOperatorService strataSplitOperatorService;
  private final FeatureService featureService;
  private final FeatureJourneyStateService featureJourneyStateService;
  private final OperatorCommandService operatorCommandService;
  private final CommandJourneyService commandJourneyService;
  private final GrpcClientService grpcClientService;
  private final PolygonService polygonService;

  public OperatorCommandReceiver(
      SplitOperatorService splitOperatorService,
      MergeOperatorService mergeOperatorService,
      StrataSplitOperatorService strataSplitOperatorService,
      FeatureService featureService,
      FeatureJourneyStateService featureJourneyStateService,
      OperatorCommandService operatorCommandService,
      CommandJourneyService commandJourneyService,
      GrpcClientService grpcClientService,
      PolygonService polygonService
  ) {
    this.splitOperatorService = splitOperatorService;
    this.mergeOperatorService = mergeOperatorService;
    this.strataSplitOperatorService = strataSplitOperatorService;
    this.featureService = featureService;
    this.featureJourneyStateService = featureJourneyStateService;
    this.operatorCommandService = operatorCommandService;
    this.commandJourneyService = commandJourneyService;
    this.grpcClientService = grpcClientService;
    this.polygonService = polygonService;
  }

  /**
   * Splits each of the given active features for a command journey that cover the request's depth (or all if no depth provided)
   * using the cutter line, recording a {@link TransformationType#SPLIT} command against their shared journey.
   * Features that the cutter line actually crossed are deactivated in favour of the new features it produces;
   * features it didn't cross are left untouched.
   *
   * @param request Contains the command journey, cutter line coordinates and the depth
   * @return the newly created features, or an empty list if the cutter line didn't cross any input feature.
   */
  @Transactional
  public List<Feature> executeSplit(SplitFromMapRequest request) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(request.commandJourneyId());
    var inputFeatures = getFeaturesToSplit(commandJourney, request.depth());

    if (CollectionUtils.isEmpty(inputFeatures)) {
      return List.of();
    }

    int wkid = CoordinateSystemUtils.getWkid(inputFeatures.getFirst().getCoordinateSystem());
    String cutterLineEsriJson = grpcClientService
        .convertPointsToPolyline(request.cutterLineOriginalSrsCoordinates(), wkid);
    List<Feature> affectedInputFeatures = new ArrayList<>();
    List<Feature> outputFeatures = new ArrayList<>();

    for (Feature inputFeature : inputFeatures) {
      List<Feature> splitResult = splitOperatorService.splitPolygon(inputFeature, cutterLineEsriJson, request.depth());
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

    featureJourneyStateService.deactivateFeatures(commandJourney, affectedInputFeatures);
    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(commandJourney, command, outputFeatures);

    return outputFeatures;
  }

  /**
   * Merges each of the given active features for a command journey, recording a
   * {@link TransformationType#MERGE} command against their shared journey.
   *
   * @param request Contains the command journey ids of the features to be merged
   * @return the newly created feature, or an empty optional if no feature ids were provided.
   */
  @Transactional
  public Optional<Feature> executeMerge(MergeFromMapRequest request) {
    if (request.featureIds().isEmpty()) {
      return Optional.empty();
    }

    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(request.commandJourneyId());
    var features = getActiveFeaturesOrThrow(commandJourney, request.featureIds());

    var mergeResult = mergeOperatorService.mergePolygons(features);

    clearUndoStack(commandJourney);

    var affectedInputFeatureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    var command = operatorCommandService.createOperatorCommand(
        commandJourney,
        affectedInputFeatureIds,
        TransformationType.MERGE
    );

    featureJourneyStateService.deactivateFeatures(commandJourney, features);
    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(commandJourney, command, List.of(mergeResult));

    return Optional.of(mergeResult);
  }

  @Transactional
  public List<Feature> executeStrataSplit(StrataSplitFromMapRequest request) {
    if (request.depth() == null) {
      throw new IllegalArgumentException("A depth must be provided to strata split");
    }

    if (request.featureIds().isEmpty()) {
      return List.of();
    }

    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(request.commandJourneyId());
    var inputFeatures = getActiveFeaturesOrThrow(commandJourney, request.featureIds());

    List<Feature> affectedInputFeatures = new ArrayList<>();
    List<Feature> outputFeatures = new ArrayList<>();
    for (Feature inputFeature : inputFeatures) {
      List<Feature> strataSplitResult = strataSplitOperatorService.strataSplitPolygon(inputFeature, request.depth());
      if (!strataSplitResult.isEmpty()) {
        affectedInputFeatures.add(inputFeature);
        outputFeatures.addAll(strataSplitResult);
      }
    }

    if (outputFeatures.isEmpty()) {
      return List.of();
    }
    clearUndoStack(commandJourney);

    var affectedInputFeatureIds = affectedInputFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var command = operatorCommandService.createOperatorCommand(
        commandJourney,
        affectedInputFeatureIds,
        TransformationType.STRATA_SPLIT
    );

    featureJourneyStateService.deactivateFeatures(commandJourney, affectedInputFeatures);
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
    featureJourneyStateService.activateFeatures(commandJourney, inputFeatures);

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
    featureJourneyStateService.deactivateFeatures(commandJourney, inputFeatures);
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

  private List<Feature> getActiveFeaturesOrThrow(
      CommandJourney commandJourney,
      List<UUID> featureIds
  ) {
    var features = commandJourneyService.getActiveFeatures(commandJourney).stream()
        .filter(feature -> featureIds.contains(feature.getId()))
        .toList();

    if (features.size() != featureIds.size()) {
      throw new IllegalArgumentException("All features must be active members of command journey %s"
          .formatted(commandJourney.getId()));
    }
    return features;
  }

  private List<Feature> getFeaturesToSplit(CommandJourney commandJourney, Long depth) {
    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourney);

    if (depth == null) {
      return activeFeatures;
    }

    var featureIdsCoveringDepth = polygonService.getPolygons(activeFeatures).stream()
        .filter(polygon -> polygon.coversDepth(depth))
        .map(polygon -> polygon.getFeature().getId())
        .collect(Collectors.toSet());

    return activeFeatures.stream()
        .filter(feature -> featureIdsCoveringDepth.contains(feature.getId()))
        .toList();
  }
}

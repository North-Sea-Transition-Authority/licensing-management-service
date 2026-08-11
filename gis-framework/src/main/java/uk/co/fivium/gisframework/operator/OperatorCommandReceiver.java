package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.fivium.gisframework.command.CommandJourneyService;
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
  private final OperatorCommandService operatorCommandService;
  private final CommandJourneyService commandJourneyService;
  private final GrpcClientService grpcClientService;

  public OperatorCommandReceiver(
      SplitOperatorService splitOperatorService,
      FeatureService featureService,
      OperatorCommandService operatorCommandService,
      CommandJourneyService commandJourneyService,
      GrpcClientService grpcClientService) {
    this.splitOperatorService = splitOperatorService;
    this.featureService = featureService;
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
    int wkid = CoordinateSystemUtils.getWkid(inputFeatures.getFirst().getCoordinateSystem());
    String cutterLineEsriJson = grpcClientService.convertPointsToPolyline(request.cutterLineOriginalSrsCoordinates(),
        wkid);

    if (CollectionUtils.isEmpty(inputFeatures)) {
      return List.of();
    }

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

    var affectedInputFeatureIds = affectedInputFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var command = operatorCommandService.createOperatorCommand(commandJourney, affectedInputFeatureIds,
        TransformationType.SPLIT);

    affectedInputFeatures.forEach(feature -> feature.setActive(false));
    featureService.saveFeatures(affectedInputFeatures);

    outputFeatures.forEach(feature -> {
      feature.setCreatedByCommand(command);
      feature.setCommandJourney(commandJourney);
      feature.setActive(true);
    });
    featureService.saveFeatures(outputFeatures);

    return outputFeatures;
  }
}

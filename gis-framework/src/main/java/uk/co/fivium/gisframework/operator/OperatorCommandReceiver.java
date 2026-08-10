package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.TransformationType;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;

@Service
public class OperatorCommandReceiver {

  private final SplitOperatorService splitOperatorService;
  private final FeatureService featureService;
  private final OperatorCommandService operatorCommandService;

  public OperatorCommandReceiver(
      SplitOperatorService splitOperatorService,
      FeatureService featureService,
      OperatorCommandService operatorCommandService
  ) {
    this.splitOperatorService = splitOperatorService;
    this.featureService = featureService;
    this.operatorCommandService = operatorCommandService;
  }

  /**
   * Splits each of the given features using the cutter line, recording a {@link TransformationType#SPLIT} command
   * against their shared journey. Features that the cutter line actually crossed are deactivated in favour of the
   * new features it produces; features it didn't cross are left untouched.
   *
   * @param inputFeatures      the journey's active features to attempt to split. Must all share the same journey.
   * @param cutterLineEsriJson the splitting line, in the same coordinate system as the input features.
   * @return the newly created features, or an empty list if the cutter line didn't cross any input feature.
   */
  @Transactional
  public List<Feature> executeSplit(List<Feature> inputFeatures, String cutterLineEsriJson) {
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

    CommandJourney commandJourney = inputFeatures.getFirst().getCommandJourney();
    var affectedInputFeatureIds = affectedInputFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var command = operatorCommandService.createOperatorCommand(commandJourney, affectedInputFeatureIds, TransformationType.SPLIT);

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

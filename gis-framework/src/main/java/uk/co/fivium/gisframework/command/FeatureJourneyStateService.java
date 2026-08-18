package uk.co.fivium.gisframework.command;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Service
public class FeatureJourneyStateService {

  private final FeatureJourneyStateRepository featureJourneyStateRepository;

  public FeatureJourneyStateService(FeatureJourneyStateRepository featureJourneyStateRepository) {
    this.featureJourneyStateRepository = featureJourneyStateRepository;
  }

  @Transactional
  public void createInitialFeatureJourneyStates(CommandJourney commandJourney, List<Feature> features) {
    var states = features.stream()
        .map(feature -> newState(feature, commandJourney, null, true))
        .toList();
    featureJourneyStateRepository.saveAll(states);
  }

  public List<Feature> getActiveFeatures(CommandJourney commandJourney) {
    return featureJourneyStateRepository.findActiveFeaturesByCommandJourney(commandJourney);
  }

  @Transactional
  public void deactivateFeatures(List<Feature> features) {
    var featureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    var states = featureJourneyStateRepository.findAllByFeature_IdIn(featureIds);
    states.forEach(state -> state.setActive(false));
    featureJourneyStateRepository.saveAll(states);
  }

  @Transactional
  public void activateFeatures(List<Feature> features) {
    var featureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    var states = featureJourneyStateRepository.findAllByFeature_IdIn(featureIds);
    states.forEach(state -> state.setActive(true));
    featureJourneyStateRepository.saveAll(states);
  }

  @Transactional
  public void deactivateFeaturesCreatedByCommand(OperatorCommand createdByCommand) {
    var states = featureJourneyStateRepository.findAllByCreatedByCommand(createdByCommand);
    states.forEach(state -> state.setActive(false));
    featureJourneyStateRepository.saveAll(states);
  }

  /**
   * Creates a FeatureJourneyState for the output features of an OperatorCommand. The state will be marked as active.
   *
   * @param commandJourney   The command journey
   * @param createdByCommand The command that created the features
   * @param outputFeatures   The output features of the createdByCommand
   */
  @Transactional
  public void createFeatureJourneyStatesForCommandOutput(CommandJourney commandJourney,
                                                         OperatorCommand createdByCommand,
                                                         List<Feature> outputFeatures) {
    var states = outputFeatures.stream()
        .map(feature -> newState(feature, commandJourney, createdByCommand, true))
        .toList();
    featureJourneyStateRepository.saveAll(states);
  }

  /**
   * Used only for the GIS test page, will remove in the future.
   */
  public Feature findFeatureWithNoJourneyStateOrThrow(CoordinateSystem coordinateSystem) {
    return featureJourneyStateRepository.findFeaturesWithNoJourneyState(coordinateSystem, Limit.of(1)).stream()
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException(
            "Feature with coordinate system %s not found".formatted(coordinateSystem)));
  }

  private static FeatureJourneyState newState(Feature feature,
                                              CommandJourney commandJourney,
                                              OperatorCommand createdByCommand,
                                              boolean active) {
    var state = new FeatureJourneyState();
    state.setFeature(feature);
    state.setCommandJourney(commandJourney);
    state.setCreatedByCommand(createdByCommand);
    state.setActive(active);
    return state;
  }
}

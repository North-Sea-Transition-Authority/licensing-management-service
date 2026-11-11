package uk.co.fivium.gisframework.command;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        .map(feature -> newState(feature, commandJourney, null, null, true))
        .toList();
    featureJourneyStateRepository.saveAll(states);
  }

  public List<Feature> getActiveFeatures(CommandJourney commandJourney) {
    return featureJourneyStateRepository.findActiveFeaturesByCommandJourney(commandJourney);
  }

  @Transactional
  public void deactivateFeatures(CommandJourney commandJourney, List<Feature> features) {
    var featureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    var states = featureJourneyStateRepository.findAllByCommandJourneyAndFeature_IdIn(commandJourney, featureIds);
    states.forEach(state -> state.setActive(false));
    featureJourneyStateRepository.saveAll(states);
  }

  @Transactional
  public void activateFeatures(CommandJourney commandJourney, List<Feature> features) {
    var featureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    var states = featureJourneyStateRepository.findAllByCommandJourneyAndFeature_IdIn(commandJourney, featureIds);
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
   * Reactivates the features created by an OperatorCommand, used when redoing that command.
   *
   * @param createdByCommand The command whose output features should be reactivated
   * @return The reactivated features
   */
  @Transactional
  public List<Feature> activateFeaturesCreatedByCommand(OperatorCommand createdByCommand) {
    var states = featureJourneyStateRepository.findAllByCreatedByCommand(createdByCommand);
    states.forEach(state -> state.setActive(true));
    featureJourneyStateRepository.saveAll(states);
    return states.stream().map(FeatureJourneyState::getFeature).toList();
  }

  /**
   * Creates a FeatureJourneyState for the output features of an OperatorCommand. The state will be marked as active.
   *
   * @param commandJourney   The command journey
   * @param createdByCommand The command that created the features
   * @param inputFeatureIds  The ids of the input features the output features were made from
   * @param outputFeatures   The output features of the createdByCommand
   */
  @Transactional
  public void createFeatureJourneyStatesForCommandOutput(
      CommandJourney commandJourney,
      OperatorCommand createdByCommand,
      Set<UUID> inputFeatureIds,
      List<Feature> outputFeatures
  ) {
    var states = outputFeatures.stream()
        .map(feature -> newState(feature, commandJourney, createdByCommand, inputFeatureIds, true))
        .toList();
    featureJourneyStateRepository.saveAll(states);
  }

  /**
   * Finds the input features consumed by a command, i.e. those its output features were made from.
   *
   * @param createdByCommand The command whose input feature ids should be returned
   * @return The ids of the command's input features
   */
  public Set<UUID> getInputFeatureIds(OperatorCommand createdByCommand) {
    return featureJourneyStateRepository.findAllByCreatedByCommand(createdByCommand).stream()
        .flatMap(state -> state.getInputFeatureIds().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Finds the input features consumed by each of the given commands.
   *
   * @param createdByCommands The commands whose input feature ids should be returned
   * @return The input feature ids keyed by the id of the command that consumed them
   */
  public Map<UUID, Set<UUID>> getInputFeatureIdsByCommandId(Collection<OperatorCommand> createdByCommands) {
    return featureJourneyStateRepository.findAllByCreatedByCommandIn(createdByCommands).stream()
        .collect(Collectors.groupingBy(
            state -> state.getCreatedByCommand().getId(),
            Collectors.flatMapping(state -> state.getInputFeatureIds().stream(), Collectors.toSet())
        ));
  }

  /**
   * Finds the input features each output feature of the given commands was made from.
   *
   * @param createdByCommands The commands whose output features should be traced
   * @return The input feature ids keyed by the id of the output feature made from them
   */
  public Map<UUID, Set<UUID>> getInputFeatureIdsByCreatedFeatureId(Collection<OperatorCommand> createdByCommands) {
    return featureJourneyStateRepository.findAllByCreatedByCommandIn(createdByCommands).stream()
        .collect(Collectors.toMap(state -> state.getFeature().getId(), FeatureJourneyState::getInputFeatureIds));
  }

  /**
   * Finds the features produced by each of the given commands.
   *
   * @param createdByCommands The commands whose output features should be returned
   * @return The output features keyed by the id of the command that created them
   */
  public Map<UUID, List<Feature>> getFeaturesCreatedByCommands(Collection<OperatorCommand> createdByCommands) {
    return featureJourneyStateRepository.findAllByCreatedByCommandIn(createdByCommands).stream()
        .collect(Collectors.groupingBy(
            state -> state.getCreatedByCommand().getId(),
            Collectors.mapping(FeatureJourneyState::getFeature, Collectors.toList())
        ));
  }

  /**
   * Finds the journeys any of the given features is held by, whether as an initial feature or a command's output.
   *
   * @param features The features to find the journeys of
   * @return The ids of the journeys holding any of the features
   */
  public Set<UUID> findJourneyIdsHoldingFeatures(Collection<Feature> features) {
    if (features.isEmpty()) {
      return Set.of();
    }

    var featureIds = features.stream().map(Feature::getId).collect(Collectors.toSet());
    return featureJourneyStateRepository.findAllByFeature_IdIn(featureIds).stream()
        .map(state -> state.getCommandJourney().getId())
        .collect(Collectors.toSet());
  }

  /**
   * Hard-deletes the feature journey states created by the given commands, returning the features they were for
   * so their now-orphaned geometry and the features themselves can also be hard-deleted.
   *
   * @param createdByCommands The commands whose output feature journey states should be deleted
   * @return The features the deleted states were for that no journey holds any longer
   */
  @Transactional
  public List<Feature> deleteFeatureJourneyStatesCreatedByCommands(List<OperatorCommand> createdByCommands) {
    var states = featureJourneyStateRepository.findAllByCreatedByCommandIn(createdByCommands);
    return deleteStatesAndReturnOrphanedFeatures(states);
  }

  @Transactional
  public List<Feature> deleteInactiveFeatureJourneyStatesCreatedByCommands(List<OperatorCommand> createdByCommands) {
    var inactiveStates = featureJourneyStateRepository.findAllByCreatedByCommandIn(createdByCommands).stream()
        .filter(state -> !state.isActive())
        .toList();
    return deleteStatesAndReturnOrphanedFeatures(inactiveStates);
  }

  /**
   * The given features that no journey holds a state for, so are safe to hard-delete.
   */
  public List<Feature> getFeaturesWithoutJourney(Collection<Feature> features) {
    if (features.isEmpty()) {
      return List.of();
    }

    var featureIds = features.stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());

    var featureIdsHeldByAJourney = featureJourneyStateRepository.findAllByFeature_IdIn(featureIds).stream()
        .map(state -> state.getFeature().getId())
        .collect(Collectors.toSet());

    return features.stream()
        .filter(feature -> !featureIdsHeldByAJourney.contains(feature.getId()))
        .distinct()
        .toList();
  }

  private List<Feature> deleteStatesAndReturnOrphanedFeatures(List<FeatureJourneyState> states) {
    featureJourneyStateRepository.deleteAll(states);

    return getFeaturesWithoutJourney(states.stream().map(FeatureJourneyState::getFeature).toList());
  }


  /**
   * Hard-deletes the feature journey states associated to a command journey.
   *
   * @param commandJourney The commands journey whose feature journey states should be deleted
   */
  @Transactional
  public void deleteAllStatesForJourney(CommandJourney commandJourney) {
    featureJourneyStateRepository.deleteAllByCommandJourney(commandJourney);
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

  /**
   * Used only for the GIS test page, will remove in the future.
   */
  public Optional<CommandJourney> findJourneyForFeature(Feature feature) {
    return featureJourneyStateRepository.findFirstByFeature_IdAndCreatedByCommandIsNull(feature.getId())
        .map(FeatureJourneyState::getCommandJourney);
  }

  private static FeatureJourneyState newState(
      Feature feature,
      CommandJourney commandJourney,
      OperatorCommand createdByCommand,
      Set<UUID> inputFeatureIds,
      boolean active
  ) {
    var state = new FeatureJourneyState();
    state.setFeature(feature);
    state.setCommandJourney(commandJourney);
    state.setCreatedByCommand(createdByCommand);
    state.setInputFeatureIds(inputFeatureIds);
    state.setActive(active);
    return state;
  }
}

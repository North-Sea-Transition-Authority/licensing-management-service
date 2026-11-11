package uk.co.fivium.gisframework.command;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;

@Service
public class CommandJourneyService {

  private final CommandJourneyRepository commandJourneyRepository;
  private final FeatureJourneyStateService featureJourneyStateService;
  private final OperatorCommandService operatorCommandService;
  private final FeatureService featureService;
  private final EntityManager entityManager;

  CommandJourneyService(
      CommandJourneyRepository commandJourneyRepository,
      FeatureJourneyStateService featureJourneyStateService,
      OperatorCommandService operatorCommandService,
      FeatureService featureService,
      EntityManager entityManager
  ) {
    this.commandJourneyRepository = commandJourneyRepository;
    this.featureJourneyStateService = featureJourneyStateService;
    this.operatorCommandService = operatorCommandService;
    this.featureService = featureService;
    this.entityManager = entityManager;
  }

  /**
   * Creates a new journey and assigns the given features to it, so they can subsequently be transformed together
   * (e.g. split) and have those transformations recorded against the journey.
   *
   * @param features the features to scope to the new journey.
   * @return the newly created journey.
   */
  @Transactional
  public CommandJourney createAndAssignCommandJourney(List<Feature> features) {
    var commandJourney = commandJourneyRepository.save(new CommandJourney());
    featureJourneyStateService.createInitialFeatureJourneyStates(commandJourney, features);
    return commandJourney;
  }

  public CommandJourney getCommandJourneyOrThrow(UUID commandJourneyId) {
    return commandJourneyRepository.findById(commandJourneyId)
        .orElseThrow(() -> new EntityNotFoundException("CommandJourney %s not found".formatted(commandJourneyId)));
  }

  /**
   * Fetches the features currently scoped to a journey that have not been superseded by a later transformation.
   *
   * @param commandJourney the journey to fetch active features for.
   * @return the journey's active features.
   */
  public List<Feature> getActiveFeatures(CommandJourney commandJourney) {
    return featureJourneyStateService.getActiveFeatures(commandJourney);
  }

  /**
   * Fetches the features currently scoped to a journey that have not been superseded by a later transformation.
   *
   * @param commandJourneyId the id of the journey to fetch active features for.
   * @return the journey's active features.
   */
  public List<Feature> getActiveFeatures(UUID commandJourneyId) {
    return getActiveFeatures(getCommandJourneyOrThrow(commandJourneyId));
  }

  /**
   * Traces each active feature created by the journey's commands back to the features the journey started with.
   *
   * @param commandJourneyId the id of the journey to trace.
   * @return the ids of the journey's initial features each created active feature was made from, keyed by the id of
   *     the created feature.
   */
  public Map<UUID, Set<UUID>> getInitialFeatureIdsByCreatedFeatureId(UUID commandJourneyId) {
    var commandJourney = getCommandJourneyOrThrow(commandJourneyId);
    var inputFeatureIdsByCreatedFeatureId = featureJourneyStateService
        .getInputFeatureIdsByCreatedFeatureId(operatorCommandService.getActiveCommands(commandJourney));

    return getActiveFeatures(commandJourney).stream()
        .map(Feature::getId)
        .filter(inputFeatureIdsByCreatedFeatureId::containsKey)
        .collect(Collectors.toMap(
            featureId -> featureId,
            featureId -> getInitialFeatureIds(featureId, inputFeatureIdsByCreatedFeatureId)
        ));
  }

  /**
   * Deletes a command journey and everything associated with it: the feature journey states, features created,
   * operator commands, and the journey itself.
   *
   * @param commandJourneyId the id of the journey to delete.
   */
  @Transactional
  public void deleteCommandJourney(UUID commandJourneyId) {
    var commandJourney = getCommandJourneyOrThrow(commandJourneyId);
    var operatorCommands = operatorCommandService.getCommands(commandJourney);

    if (!operatorCommands.isEmpty()) {
      var orphanedFeatures = featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(operatorCommands);
      featureService.deleteAll(orphanedFeatures);
      operatorCommandService.deleteCommands(operatorCommands);
    }

    // remove remaining i.e. initial state for this command journey
    featureJourneyStateService.deleteAllStatesForJourney(commandJourney);
    commandJourneyRepository.delete(commandJourney);

    entityManager.flush();
  }

  /**
   * Deletes a command journey like {@link #deleteCommandJourney}, but keeps its active features.
   */
  @Transactional
  public void deleteAllExcludingActiveFeatures(UUID commandJourneyId) {
    var commandJourney = getCommandJourneyOrThrow(commandJourneyId);
    var operatorCommands = operatorCommandService.getCommands(commandJourney);

    if (!operatorCommands.isEmpty()) {
      var supersededFeatures =
          featureJourneyStateService.deleteInactiveFeatureJourneyStatesCreatedByCommands(operatorCommands);
      featureService.deleteAll(supersededFeatures);
    }

    featureJourneyStateService.deleteAllStatesForJourney(commandJourney);

    if (!operatorCommands.isEmpty()) {
      operatorCommandService.deleteCommands(operatorCommands);
    }

    commandJourneyRepository.delete(commandJourney);

    entityManager.flush();
  }

  /**
   * Hard-deletes the given features, except any a journey still holds.
   */
  @Transactional
  public void deleteFeaturesNotHeldByAJourney(Collection<Feature> features) {
    var deletableFeatures = featureJourneyStateService.getFeaturesWithoutJourney(features);
    if (!deletableFeatures.isEmpty()) {
      featureService.deleteAll(deletableFeatures);
    }
  }

  /**
   * Used only for the GIS test page, will remove in the future.
   */
  @Transactional
  public CommandJourney findOrCreateCommandJourneyForFeatures(List<Feature> features) {
    return features.stream()
        .map(featureJourneyStateService::findJourneyForFeature)
        .flatMap(Optional::stream)
        .filter(journey -> new HashSet<>(getActiveFeatures(journey)).containsAll(features))
        .findFirst()
        .orElseGet(() -> createAndAssignCommandJourney(features));
  }

  private static Set<UUID> getInitialFeatureIds(
      UUID featureId,
      Map<UUID, Set<UUID>> inputFeatureIdsByCreatedFeatureId
  ) {
    var inputFeatureIds = inputFeatureIdsByCreatedFeatureId.get(featureId);
    if (inputFeatureIds == null) {
      return Set.of(featureId);
    }
    return inputFeatureIds.stream()
        .flatMap(inputFeatureId -> getInitialFeatureIds(inputFeatureId, inputFeatureIdsByCreatedFeatureId).stream())
        .collect(Collectors.toSet());
  }
}

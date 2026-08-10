package uk.co.fivium.gisframework.command;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;

@Service
public class CommandJourneyService {

  private final CommandJourneyRepository commandJourneyRepository;
  private final FeatureService featureService;

  public CommandJourneyService(CommandJourneyRepository commandJourneyRepository, FeatureService featureService) {
    this.commandJourneyRepository = commandJourneyRepository;
    this.featureService = featureService;
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
    features.forEach(feature -> feature.setCommandJourney(commandJourney));
    featureService.saveFeatures(features);
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
    return featureService.findAllByCommandJourney(commandJourney).stream()
        .filter(Feature::isActive)
        .toList();
  }
}

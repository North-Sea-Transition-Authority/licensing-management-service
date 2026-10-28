package uk.co.fivium.gisframework.command;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.operator.JsonHistoryAction;

@Service
public class OperatorCommandService {

  private final OperatorCommandRepository operatorCommandRepository;
  private final FeatureService featureService;
  private final FeatureJourneyStateService featureJourneyStateService;

  public OperatorCommandService(
      OperatorCommandRepository operatorCommandRepository,
      FeatureService featureService,
      FeatureJourneyStateService featureJourneyStateService
  ) {
    this.operatorCommandRepository = operatorCommandRepository;
    this.featureService = featureService;
    this.featureJourneyStateService = featureJourneyStateService;
  }

  @Transactional
  public OperatorCommand createOperatorCommand(CommandJourney commandJourney,
                                               Set<UUID> inputFeatureIds,
                                               TransformationType transformationType) {
    int commandOrder = operatorCommandRepository.findMaxCommandOrderByCommandJourney(commandJourney)
        .map(order -> order + 1)
        .orElse(1);

    var command = new OperatorCommand();
    command.setCommandJourney(commandJourney);
    command.setInputFeatureIds(inputFeatureIds);
    command.setStatus(CommandStatus.ACTIVE);
    command.setTransformationType(transformationType);
    command.setCommandOrder(commandOrder);

    return operatorCommandRepository.save(command);
  }

  /**
   * Finds the most recently issued command that is active for a journey.
   *
   * @param commandJourney the journey to find the current active command for.
   * @return the current active command, or empty if the journey has no active command.
   */
  public Optional<OperatorCommand> getCurrentActiveCommand(CommandJourney commandJourney) {
    return operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(
        commandJourney, CommandStatus.ACTIVE);
  }

  /**
   * Finds all commands for a command journey.
   *
   * @param commandJourney the journey to find all commands for.
   * @return all operator commands for the journey.
   */
  public List<OperatorCommand> getCommands(CommandJourney commandJourney) {
    return operatorCommandRepository.findAllByCommandJourney(commandJourney);
  }

  @Transactional
  public void markUndone(OperatorCommand command) {
    command.setStatus(CommandStatus.UNDONE);
    operatorCommandRepository.save(command);
  }

  /**
   * Finds the earliest undone command in a journey, i.e. the command that "redo" would reapply.
   *
   * @param commandJourney the journey to find the next redo command for.
   * @return the next command to redo, or empty if the journey has no undone command.
   */
  public Optional<OperatorCommand> getNextRedoCommand(CommandJourney commandJourney) {
    return operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
        commandJourney, CommandStatus.UNDONE);
  }

  @Transactional
  public void markRedone(OperatorCommand command) {
    command.setStatus(CommandStatus.ACTIVE);
    operatorCommandRepository.save(command);
  }

  /**
   * Whether a journey has an active command that can be undone.
   *
   * @param commandJourney the journey to check.
   * @return true if the journey has an active command, false otherwise.
   */
  public boolean canUndo(CommandJourney commandJourney) {
    return getCurrentActiveCommand(commandJourney).isPresent();
  }

  /**
   * Whether a journey has an undone command that can be redone.
   *
   * @param commandJourney the journey to check.
   * @return true if the journey has an undone command, false otherwise.
   */
  public boolean canRedo(CommandJourney commandJourney) {
    return getNextRedoCommand(commandJourney).isPresent();
  }

  /**
   * Finds all undone commands in a journey, i.e. its whole redo stack.
   *
   * @param commandJourney the journey to find undone commands for.
   * @return the journey's undone commands.
   */
  public List<OperatorCommand> getUndoneCommands(CommandJourney commandJourney) {
    return operatorCommandRepository.findAllByCommandJourneyAndStatus(commandJourney, CommandStatus.UNDONE);
  }

  /**
   * Finds the active commands for a journey, oldest first.
   *
   * @param commandJourney the journey to find active commands for.
   * @return the journey's active commands ordered by when they were issued.
   */
  public List<OperatorCommand> getActiveCommands(CommandJourney commandJourney) {
    return operatorCommandRepository.findAllByCommandJourneyAndStatusOrderByCommandOrderAsc(
        commandJourney,
        CommandStatus.ACTIVE
    );
  }

  /**
   * Builds the actions taken in a journey that have not been undone, newest first.
   *
   * @param commandJourney the journey to build the history for.
   * @return the journey's active actions, numbered in the order they were taken.
   */
  public List<JsonHistoryAction> getActiveHistory(CommandJourney commandJourney) {
    var commands = getActiveCommands(commandJourney);
    if (commands.isEmpty()) {
      return List.of();
    }

    var inputFeatureIds = commands.stream()
        .flatMap(command -> command.getInputFeatureIds().stream())
        .collect(Collectors.toSet());
    var inputFeatureNamesById = featureService.getFeaturesByIds(inputFeatureIds).stream()
        .collect(Collectors.toMap(Feature::getId, Feature::getFeatureName));
    var outputFeaturesByCommandId = featureJourneyStateService.getFeaturesCreatedByCommands(commands);

    return commands.reversed().stream()
        .map(command -> new JsonHistoryAction(
            command.getCommandOrder(),
            command.getTransformationType(),
            command.getInputFeatureIds().stream()
                .map(inputFeatureNamesById::get)
                .sorted()
                .toList(),
            outputFeaturesByCommandId.getOrDefault(command.getId(), List.of()).stream()
                .map(Feature::getFeatureName)
                .sorted()
                .toList()
        ))
        .toList();
  }

  @Transactional
  public void deleteCommands(List<OperatorCommand> commands) {
    operatorCommandRepository.deleteAll(commands);
  }
}

package uk.co.fivium.gisframework.command;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorCommandService {

  private final OperatorCommandRepository operatorCommandRepository;

  public OperatorCommandService(OperatorCommandRepository operatorCommandRepository) {
    this.operatorCommandRepository = operatorCommandRepository;
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

  @Transactional
  public void markUndone(OperatorCommand command) {
    command.setStatus(CommandStatus.UNDONE);
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
}

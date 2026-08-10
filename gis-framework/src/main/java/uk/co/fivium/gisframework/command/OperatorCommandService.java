package uk.co.fivium.gisframework.command;

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
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;

@Service
class SubareaChangeValidator {

  private final CommandJourneyService commandJourneyService;
  private final OperatorCommandService operatorCommandService;

  SubareaChangeValidator(
      CommandJourneyService commandJourneyService,
      OperatorCommandService operatorCommandService
  ) {
    this.commandJourneyService = commandJourneyService;
    this.operatorCommandService = operatorCommandService;
  }

  public boolean hasErrors(UUID commandJourneyId) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    return !operatorCommandService.canUndo(commandJourney);
  }
}

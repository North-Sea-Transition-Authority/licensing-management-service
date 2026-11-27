package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;

@Service
public class SubareaChangeValidator {

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

  public List<String> getErrors(List<SubareaDetails> originalSubareas, List<SubareaTransfer> transfers) {
    if (transfers.isEmpty()) {
      return List.of(SubareaPositionRule.NO_CHANGE.getMessage());
    }

    var replacedSubareas = transfers.stream()
        .flatMap(transfer -> transfer.replacedSubareas().stream())
        .collect(Collectors.toSet());
    var subareasAfterChange = Stream.concat(
            originalSubareas.stream().filter(subarea -> !replacedSubareas.contains(subarea)),
            transfers.stream().flatMap(transfer -> transfer.outputSubareas().stream())
        )
        .distinct()
        .toList();

    var errors = new ArrayList<String>();

    if (SubareaDetails.hasDuplicateNames(subareasAfterChange)) {
      errors.add(SubareaPositionRule.SUBAREA_NAMES_MUST_BE_UNIQUE.getMessage());
    }

    if (SubareaDetails.hasDuplicateShortNames(subareasAfterChange)) {
      errors.add(SubareaPositionRule.MAP_DISPLAY_NAMES_MUST_BE_UNIQUE.getMessage());
    }

    return errors;
  }
}

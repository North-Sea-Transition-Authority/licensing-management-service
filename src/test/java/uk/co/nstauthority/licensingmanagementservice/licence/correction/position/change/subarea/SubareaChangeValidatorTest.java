package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;

@ExtendWith(MockitoExtension.class)
class SubareaChangeValidatorTest {

  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private OperatorCommandService operatorCommandService;

  @InjectMocks
  private SubareaChangeValidator subareaChangeValidator;

  @ParameterizedTest
  @CsvSource({
      "true, false",
      "false, true"
  })
  void hasErrors(boolean hasActiveCommand, boolean expectedHasErrors) {
    var commandJourney = new CommandJourney();
    when(commandJourneyService.getCommandJourneyOrThrow(COMMAND_JOURNEY_ID)).thenReturn(commandJourney);
    when(operatorCommandService.canUndo(commandJourney)).thenReturn(hasActiveCommand);

    var result = subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID);

    assertThat(result).isEqualTo(expectedHasErrors);
  }
}

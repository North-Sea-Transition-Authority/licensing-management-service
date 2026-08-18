package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorCommandServiceTest {

  @Mock
  private OperatorCommandRepository operatorCommandRepository;

  @InjectMocks
  private OperatorCommandService operatorCommandService;

  @Test
  void createCommand_whenNoExistingCommands_assignsCommandOrderOne() {
    var journey = new CommandJourney();
    var inputFeatureIds = Set.of(UUID.randomUUID());

    when(operatorCommandRepository.findMaxCommandOrderByCommandJourney(journey)).thenReturn(Optional.empty());
    when(operatorCommandRepository.save(any(OperatorCommand.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var command = operatorCommandService.createOperatorCommand(journey, inputFeatureIds, TransformationType.SPLIT);

    OperatorCommand expected = OperatorCommandTestUtil.newBuilder()
        .withCommandJourney(journey)
        .withInputFeatureIds(inputFeatureIds)
        .withStatus(CommandStatus.ACTIVE)
        .withTransformationType(TransformationType.SPLIT)
        .withCommandOrder(1)
        .build();
    assertThat(command).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
  }

  @Test
  void createCommand_whenExistingCommands_assignsNextCommandOrder() {
    var journey = new CommandJourney();
    var inputFeatureIds = Set.of(UUID.randomUUID());
    var commandCaptor = ArgumentCaptor.forClass(OperatorCommand.class);

    when(operatorCommandRepository.findMaxCommandOrderByCommandJourney(journey)).thenReturn(Optional.of(3));
    when(operatorCommandRepository.save(commandCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

    operatorCommandService.createOperatorCommand(journey, inputFeatureIds, TransformationType.SPLIT);

    assertThat(commandCaptor.getValue().getCommandOrder()).isEqualTo(4);
  }

  @Test
  void getCurrentActiveCommand() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().build();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(
        journey, CommandStatus.ACTIVE)).thenReturn(Optional.of(command));

    assertThat(operatorCommandService.getCurrentActiveCommand(journey)).contains(command);
  }

  @Test
  void markUndone_setsStatusToUndoneAndSaves() {
    var command = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.ACTIVE).build();

    when(operatorCommandRepository.save(command)).thenReturn(command);

    operatorCommandService.markUndone(command);

    assertThat(command.getStatus()).isEqualTo(CommandStatus.UNDONE);
    verify(operatorCommandRepository).save(command);
  }

  @Test
  void canUndo_whenActiveCommandExists_returnsTrue() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().build();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(
        journey, CommandStatus.ACTIVE)).thenReturn(Optional.of(command));

    assertThat(operatorCommandService.canUndo(journey)).isTrue();
  }

  @Test
  void canUndo_whenNoActiveCommand_returnsFalse() {
    var journey = new CommandJourney();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(
        journey, CommandStatus.ACTIVE)).thenReturn(Optional.empty());

    assertThat(operatorCommandService.canUndo(journey)).isFalse();
  }
}

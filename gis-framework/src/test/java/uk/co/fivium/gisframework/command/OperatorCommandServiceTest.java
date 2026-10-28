package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.operator.JsonHistoryAction;

@ExtendWith(MockitoExtension.class)
class OperatorCommandServiceTest {

  @Mock
  private OperatorCommandRepository operatorCommandRepository;

  @Mock
  private FeatureJourneyStateService featureJourneyStateService;

  @Mock
  private FeatureService featureService;

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

  @Test
  void getNextRedoCommand_whenUndoneCommandExists_returnsIt() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
        journey, CommandStatus.UNDONE)).thenReturn(Optional.of(command));

    assertThat(operatorCommandService.getNextRedoCommand(journey)).contains(command);
  }

  @Test
  void getNextRedoCommand_whenNoUndoneCommandExists_returnsEmpty() {
    var journey = new CommandJourney();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
        journey, CommandStatus.UNDONE)).thenReturn(Optional.empty());

    assertThat(operatorCommandService.getNextRedoCommand(journey)).isEmpty();
  }

  @Test
  void markRedone_setsStatusToActiveAndSaves() {
    var command = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();

    when(operatorCommandRepository.save(command)).thenReturn(command);

    operatorCommandService.markRedone(command);

    assertThat(command.getStatus()).isEqualTo(CommandStatus.ACTIVE);
    verify(operatorCommandRepository).save(command);
  }

  @Test
  void canRedo_whenUndoneCommandExists_returnsTrue() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
        journey, CommandStatus.UNDONE)).thenReturn(Optional.of(command));

    assertThat(operatorCommandService.canRedo(journey)).isTrue();
  }

  @Test
  void canRedo_whenNoUndoneCommand_returnsFalse() {
    var journey = new CommandJourney();

    when(operatorCommandRepository.findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
        journey, CommandStatus.UNDONE)).thenReturn(Optional.empty());

    assertThat(operatorCommandService.canRedo(journey)).isFalse();
  }

  @Test
  void getUndoneCommands_returnsUndoneCommandsForJourney() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().withStatus(CommandStatus.UNDONE).build();

    when(operatorCommandRepository.findAllByCommandJourneyAndStatus(journey, CommandStatus.UNDONE))
        .thenReturn(List.of(command));

    assertThat(operatorCommandService.getUndoneCommands(journey)).containsExactly(command);
  }

  @Test
  void getCommands() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().build();

    when(operatorCommandRepository.findAllByCommandJourney(journey)).thenReturn(List.of(command));

    assertThat(operatorCommandService.getCommands(journey)).containsExactly(command);
  }

  @Test
  void getActiveCommands() {
    var journey = new CommandJourney();
    var command = OperatorCommandTestUtil.newBuilder().build();

    when(operatorCommandRepository.findAllByCommandJourneyAndStatusOrderByCommandOrderAsc(
        journey, CommandStatus.ACTIVE)).thenReturn(List.of(command));

    assertThat(operatorCommandService.getActiveCommands(journey)).containsExactly(command);
  }

  @Test
  void getActiveHistory_whenNoActiveCommands_thenEmptyWithoutLoadingFeatures() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    when(operatorCommandService.getActiveCommands(commandJourney)).thenReturn(List.of());

    var result = operatorCommandService.getActiveHistory(commandJourney);

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService, featureJourneyStateService);
  }

  @Test
  void getActiveHistory_whenActiveCommands_thenNewestFirstWithSortedFeatureNames() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var originalFeature = feature("C");
    var firstSplitOutput = feature("C_1");
    var secondSplitOutput = feature("C_2");
    var otherFeature = feature("A");
    var mergeOutput = feature("mergeResult_ABCD");
    var splitCommand = OperatorCommandTestUtil.newBuilder()
        .withTransformationType(TransformationType.SPLIT)
        .withInputFeatureIds(Set.of(originalFeature.getId()))
        .withCommandOrder(1)
        .build();
    var mergeCommand = OperatorCommandTestUtil.newBuilder()
        .withTransformationType(TransformationType.MERGE)
        .withInputFeatureIds(Set.of(secondSplitOutput.getId(), otherFeature.getId()))
        .withCommandOrder(2)
        .build();
    var commands = List.of(splitCommand, mergeCommand);

    when(operatorCommandService.getActiveCommands(commandJourney)).thenReturn(commands);
    when(featureService.getFeaturesByIds(Set.of(originalFeature.getId(), secondSplitOutput.getId(), otherFeature.getId())))
        .thenReturn(List.of(originalFeature, secondSplitOutput, otherFeature));
    when(featureJourneyStateService.getFeaturesCreatedByCommands(commands)).thenReturn(Map.of(
        splitCommand.getId(), List.of(secondSplitOutput, firstSplitOutput),
        mergeCommand.getId(), List.of(mergeOutput)
    ));

    var result = operatorCommandService.getActiveHistory(commandJourney);

    assertThat(result).containsExactly(
        new JsonHistoryAction(2, TransformationType.MERGE, List.of("A", "C_2"), List.of("mergeResult_ABCD")),
        new JsonHistoryAction(1, TransformationType.SPLIT, List.of("C"), List.of("C_1", "C_2"))
    );
  }

  @Test
  void getActiveHistory_whenCommandHasNoOutputs_thenEmptyOutputFeatureNames() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var inputFeature = feature("A");
    var command = OperatorCommandTestUtil.newBuilder()
        .withTransformationType(TransformationType.STRATA_SPLIT)
        .withInputFeatureIds(Set.of(inputFeature.getId()))
        .withCommandOrder(1)
        .build();

    when(operatorCommandService.getActiveCommands(commandJourney)).thenReturn(List.of(command));
    when(featureService.getFeaturesByIds(Set.of(inputFeature.getId()))).thenReturn(List.of(inputFeature));
    when(featureJourneyStateService.getFeaturesCreatedByCommands(List.of(command))).thenReturn(Map.of());

    var result = operatorCommandService.getActiveHistory(commandJourney);

    assertThat(result).containsExactly(
        new JsonHistoryAction(1, TransformationType.STRATA_SPLIT, List.of("A"), List.of())
    );
  }

  private static Feature feature(String featureName) {
    return FeatureTestUtil.newBuilder().withFeatureName(featureName).build();
  }

  @Test
  void deleteCommands_delegatesToRepository() {
    var command = OperatorCommandTestUtil.newBuilder().build();

    operatorCommandService.deleteCommands(List.of(command));

    verify(operatorCommandRepository).deleteAll(List.of(command));
  }
}

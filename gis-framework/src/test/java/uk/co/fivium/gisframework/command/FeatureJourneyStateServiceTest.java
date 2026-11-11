package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@ExtendWith(MockitoExtension.class)
class FeatureJourneyStateServiceTest {

  @Mock
  private FeatureJourneyStateRepository featureJourneyStateRepository;

  @InjectMocks
  private FeatureJourneyStateService featureJourneyStateService;

  @Captor
  private ArgumentCaptor<List<FeatureJourneyState>> statesCaptor;

  @Test
  void findJourneyIdsHoldingFeatures_whenFeaturesHeld_returnsDistinctJourneyIds() {
    var firstJourney = CommandJourneyTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var secondJourney = CommandJourneyTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var initialFeature = FeatureTestUtil.newBuilder().build();
    var createdFeature = FeatureTestUtil.newBuilder().build();
    when(featureJourneyStateRepository.findAllByFeature_IdIn(Set.of(initialFeature.getId(), createdFeature.getId())))
        .thenReturn(List.of(
            FeatureJourneyStateTestUtil.newBuilder().withFeature(initialFeature).withCommandJourney(firstJourney).build(),
            FeatureJourneyStateTestUtil.newBuilder().withFeature(createdFeature).withCommandJourney(firstJourney).build(),
            FeatureJourneyStateTestUtil.newBuilder().withFeature(initialFeature).withCommandJourney(secondJourney).build()
        ));

    var result = featureJourneyStateService.findJourneyIdsHoldingFeatures(List.of(initialFeature, createdFeature));

    assertThat(result).isEqualTo(Set.of(firstJourney.getId(), secondJourney.getId()));
  }

  @Test
  void findJourneyIdsHoldingFeatures_whenNoFeatures_returnsEmptyWithoutQuerying() {
    var result = featureJourneyStateService.findJourneyIdsHoldingFeatures(List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureJourneyStateRepository);
  }

  @Test
  void createInitialFeatureJourneyStates_createsActiveStateWithNoCreatingCommand() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var feature1 = FeatureTestUtil.newBuilder().build();
    var feature2 = FeatureTestUtil.newBuilder().build();

    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    featureJourneyStateService.createInitialFeatureJourneyStates(commandJourney, List.of(feature1, feature2));

    verify(featureJourneyStateRepository).saveAll(statesCaptor.capture());
    assertThat(statesCaptor.getValue())
        .extracting(
            FeatureJourneyState::getFeature,
            FeatureJourneyState::getCommandJourney,
            FeatureJourneyState::getCreatedByCommand,
            FeatureJourneyState::isActive
        )
        .containsExactly(
            tuple(feature1, commandJourney, null, true),
            tuple(feature2, commandJourney, null, true)
        );
  }

  @Test
  void getActiveFeatures_delegatesToRepository() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    when(featureJourneyStateRepository.findActiveFeaturesByCommandJourney(commandJourney)).thenReturn(List.of(feature));

    assertThat(featureJourneyStateService.getActiveFeatures(commandJourney)).containsExactly(feature);
  }

  @Test
  void deactivateFeatures_setsExistingStatesInactive() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder().withFeature(feature).withActive(true).build();

    when(featureJourneyStateRepository.findAllByCommandJourneyAndFeature_IdIn(commandJourney, Set.of(feature.getId())))
        .thenReturn(List.of(state));
    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    featureJourneyStateService.deactivateFeatures(commandJourney, List.of(feature));

    assertThat(state.isActive()).isFalse();
    verify(featureJourneyStateRepository).saveAll(List.of(state));
  }

  @Test
  void activateFeatures_setsExistingStatesActive() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder().withFeature(feature).withActive(false).build();

    when(featureJourneyStateRepository.findAllByCommandJourneyAndFeature_IdIn(commandJourney, Set.of(feature.getId())))
        .thenReturn(List.of(state));
    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    featureJourneyStateService.activateFeatures(commandJourney, List.of(feature));

    verify(featureJourneyStateRepository).saveAll(statesCaptor.capture());
    assertThat(statesCaptor.getValue())
        .extracting(FeatureJourneyState::getFeature, FeatureJourneyState::isActive)
        .containsExactly(tuple(feature, true));
  }

  @Test
  void deactivateFeaturesCreatedByCommand_setsStatesInactive() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(feature)
        .withCreatedByCommand(command)
        .withActive(true)
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommand(command)).thenReturn(List.of(state));

    featureJourneyStateService.deactivateFeaturesCreatedByCommand(command);

    verify(featureJourneyStateRepository).saveAll(statesCaptor.capture());
    assertThat(statesCaptor.getValue())
        .extracting(FeatureJourneyState::getFeature, FeatureJourneyState::isActive)
        .containsExactly(tuple(feature, false));
  }

  @Test
  void activateFeaturesCreatedByCommand_setsStatesActiveAndReturnsFeatures() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(feature)
        .withCreatedByCommand(command)
        .withActive(false)
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommand(command)).thenReturn(List.of(state));
    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = featureJourneyStateService.activateFeaturesCreatedByCommand(command);

    assertThat(state.isActive()).isTrue();
    assertThat(result).containsExactly(feature);
    verify(featureJourneyStateRepository).saveAll(List.of(state));
  }

  @Test
  void createFeatureJourneyStatesForCommandOutput_createsActiveStateWithCreatingCommandAndInputs() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var command = OperatorCommandTestUtil.newBuilder().build();
    var inputFeatureIds = Set.of(UUID.randomUUID());
    var outputFeature = FeatureTestUtil.newBuilder().build();

    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(
        commandJourney,
        command,
        inputFeatureIds,
        List.of(outputFeature)
    );

    verify(featureJourneyStateRepository).saveAll(statesCaptor.capture());
    var expectedState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(outputFeature)
        .withCommandJourney(commandJourney)
        .withCreatedByCommand(command)
        .withInputFeatureIds(inputFeatureIds)
        .withActive(true)
        .build();
    assertThat(statesCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(List.of(expectedState));
  }

  @Test
  void getInputFeatureIds() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var inputFeatureIdA = UUID.randomUUID();
    var inputFeatureIdB = UUID.randomUUID();

    when(featureJourneyStateRepository.findAllByCreatedByCommand(command)).thenReturn(List.of(
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(command)
            .withInputFeatureIds(Set.of(inputFeatureIdA))
            .build(),
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(command)
            .withInputFeatureIds(Set.of(inputFeatureIdA))
            .build(),
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(command)
            .withInputFeatureIds(Set.of(inputFeatureIdB))
            .build()
    ));

    var result = featureJourneyStateService.getInputFeatureIds(command);

    assertThat(result).isEqualTo(Set.of(inputFeatureIdA, inputFeatureIdB));
  }

  @Test
  void getInputFeatureIdsByCommandId() {
    var splitCommand = OperatorCommandTestUtil.newBuilder().build();
    var mergeCommand = OperatorCommandTestUtil.newBuilder().build();
    var splitInputFeatureIdA = UUID.randomUUID();
    var splitInputFeatureIdB = UUID.randomUUID();
    var mergeInputFeatureIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
    var commands = List.of(splitCommand, mergeCommand);

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(commands)).thenReturn(List.of(
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(splitCommand)
            .withInputFeatureIds(Set.of(splitInputFeatureIdA))
            .build(),
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(mergeCommand)
            .withInputFeatureIds(mergeInputFeatureIds)
            .build(),
        FeatureJourneyStateTestUtil.newBuilder()
            .withCreatedByCommand(splitCommand)
            .withInputFeatureIds(Set.of(splitInputFeatureIdB))
            .build()
    ));

    var result = featureJourneyStateService.getInputFeatureIdsByCommandId(commands);

    assertThat(result).isEqualTo(Map.of(
        splitCommand.getId(), Set.of(splitInputFeatureIdA, splitInputFeatureIdB),
        mergeCommand.getId(), mergeInputFeatureIds
    ));
  }

  @Test
  void getInputFeatureIdsByCreatedFeatureId() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var outputFeatureA = FeatureTestUtil.newBuilder().build();
    var outputFeatureB = FeatureTestUtil.newBuilder().build();
    var inputFeatureIdA = UUID.randomUUID();
    var inputFeatureIdB = UUID.randomUUID();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command))).thenReturn(List.of(
        FeatureJourneyStateTestUtil.newBuilder()
            .withFeature(outputFeatureA)
            .withCreatedByCommand(command)
            .withInputFeatureIds(Set.of(inputFeatureIdA))
            .build(),
        FeatureJourneyStateTestUtil.newBuilder()
            .withFeature(outputFeatureB)
            .withCreatedByCommand(command)
            .withInputFeatureIds(Set.of(inputFeatureIdB))
            .build()
    ));

    var result = featureJourneyStateService.getInputFeatureIdsByCreatedFeatureId(List.of(command));

    assertThat(result).isEqualTo(Map.of(
        outputFeatureA.getId(), Set.of(inputFeatureIdA),
        outputFeatureB.getId(), Set.of(inputFeatureIdB)
    ));
  }

  @Test
  void getFeaturesCreatedByCommands() {
    var splitCommand = OperatorCommandTestUtil.newBuilder().build();
    var mergeCommand = OperatorCommandTestUtil.newBuilder().build();
    var splitOutput1 = FeatureTestUtil.newBuilder().build();
    var splitOutput2 = FeatureTestUtil.newBuilder().build();
    var mergeOutput = FeatureTestUtil.newBuilder().build();
    var commands = List.of(splitCommand, mergeCommand);

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(commands)).thenReturn(List.of(
        FeatureJourneyStateTestUtil.newBuilder().withCreatedByCommand(splitCommand).withFeature(splitOutput1).build(),
        FeatureJourneyStateTestUtil.newBuilder().withCreatedByCommand(mergeCommand).withFeature(mergeOutput).build(),
        FeatureJourneyStateTestUtil.newBuilder().withCreatedByCommand(splitCommand).withFeature(splitOutput2).build()
    ));

    var result = featureJourneyStateService.getFeaturesCreatedByCommands(commands);

    assertThat(result).isEqualTo(Map.of(
        splitCommand.getId(), List.of(splitOutput1, splitOutput2),
        mergeCommand.getId(), List.of(mergeOutput)
    ));
  }

  @Test
  void deleteFeatureJourneyStatesCreatedByCommands_whenNoJourneyHoldsTheFeature_thenStatesDeletedAndFeatureReturned() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(feature)
        .withCreatedByCommand(command)
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command))).thenReturn(List.of(state));
    when(featureJourneyStateRepository.findAllByFeature_IdIn(Set.of(feature.getId()))).thenReturn(List.of());

    var result = featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(List.of(command));

    assertThat(result).containsExactly(feature);
    verify(featureJourneyStateRepository).deleteAll(List.of(state));
  }

  @Test
  void deleteFeatureJourneyStatesCreatedByCommands_whenAnotherJourneyStillHoldsTheFeature_thenFeatureNotReturned() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var orphanedFeature = FeatureTestUtil.newBuilder().build();
    var sharedFeature = FeatureTestUtil.newBuilder().build();
    var orphanedState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(orphanedFeature)
        .withCreatedByCommand(command)
        .build();
    var sharedState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(sharedFeature)
        .withCreatedByCommand(command)
        .build();
    var stateInOtherJourney = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(sharedFeature)
        .withCommandJourney(CommandJourneyTestUtil.newBuilder().build())
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command)))
        .thenReturn(List.of(orphanedState, sharedState));
    when(featureJourneyStateRepository.findAllByFeature_IdIn(
        Set.of(orphanedFeature.getId(), sharedFeature.getId())))
        .thenReturn(List.of(stateInOtherJourney));

    var result = featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(List.of(command));

    assertThat(result).containsExactly(orphanedFeature);
    verify(featureJourneyStateRepository).deleteAll(List.of(orphanedState, sharedState));
  }

  @Test
  void deleteFeatureJourneyStatesCreatedByCommands_whenNoStatesFound_thenNoFeaturesReturned() {
    var command = OperatorCommandTestUtil.newBuilder().build();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command))).thenReturn(List.of());

    var result = featureJourneyStateService.deleteFeatureJourneyStatesCreatedByCommands(List.of(command));

    assertThat(result).isEmpty();
    verify(featureJourneyStateRepository).deleteAll(List.of());
  }

  @Test
  void deleteInactiveFeatureJourneyStatesCreatedByCommands_whenStatesAreMixed_thenOnlyInactiveStatesDeleted() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var activeFeature = FeatureTestUtil.newBuilder().build();
    var supersededFeature = FeatureTestUtil.newBuilder().build();
    var activeState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(activeFeature)
        .withCreatedByCommand(command)
        .withActive(true)
        .build();
    var supersededState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(supersededFeature)
        .withCreatedByCommand(command)
        .withActive(false)
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command)))
        .thenReturn(List.of(activeState, supersededState));
    when(featureJourneyStateRepository.findAllByFeature_IdIn(Set.of(supersededFeature.getId())))
        .thenReturn(List.of());

    var result = featureJourneyStateService.deleteInactiveFeatureJourneyStatesCreatedByCommands(List.of(command));

    assertThat(result).containsExactly(supersededFeature);
    verify(featureJourneyStateRepository).deleteAll(List.of(supersededState));
  }

  @Test
  void deleteInactiveFeatureJourneyStatesCreatedByCommands_whenAnotherJourneyHoldsTheFeature_thenFeatureNotReturned() {
    var command = OperatorCommandTestUtil.newBuilder().build();
    var sharedFeature = FeatureTestUtil.newBuilder().build();
    var supersededState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(sharedFeature)
        .withCreatedByCommand(command)
        .withActive(false)
        .build();
    var stateInOtherJourney = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(sharedFeature)
        .withCommandJourney(CommandJourneyTestUtil.newBuilder().build())
        .build();

    when(featureJourneyStateRepository.findAllByCreatedByCommandIn(List.of(command)))
        .thenReturn(List.of(supersededState));
    when(featureJourneyStateRepository.findAllByFeature_IdIn(Set.of(sharedFeature.getId())))
        .thenReturn(List.of(stateInOtherJourney));

    var result = featureJourneyStateService.deleteInactiveFeatureJourneyStatesCreatedByCommands(List.of(command));

    assertThat(result).isEmpty();
    verify(featureJourneyStateRepository).deleteAll(List.of(supersededState));
  }

  @Test
  void withoutFeaturesHeldByAJourney_whenAJourneyHoldsAFeature_thenItIsLeftOut() {
    var unheldFeature = FeatureTestUtil.newBuilder().build();
    var heldFeature = FeatureTestUtil.newBuilder().build();
    var heldState = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(heldFeature)
        .build();

    when(featureJourneyStateRepository.findAllByFeature_IdIn(Set.of(unheldFeature.getId(), heldFeature.getId())))
        .thenReturn(List.of(heldState));

    var result = featureJourneyStateService.getFeaturesWithoutJourney(List.of(unheldFeature, heldFeature));

    assertThat(result).containsExactly(unheldFeature);
  }

  @Test
  void withoutFeaturesHeldByAJourney_whenNoFeaturesGiven_thenNothingIsQueried() {
    var result = featureJourneyStateService.getFeaturesWithoutJourney(List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureJourneyStateRepository);
  }

  @Test
  void deleteAllStatesForJourney() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();

    featureJourneyStateService.deleteAllStatesForJourney(commandJourney);

    verify(featureJourneyStateRepository).deleteAllByCommandJourney(commandJourney);
  }

  @Test
  void findFeatureWithNoJourneyStateOrThrow_whenFound_returnsFeature() {
    var feature = FeatureTestUtil.newBuilder().build();
    when(featureJourneyStateRepository.findFeaturesWithNoJourneyState(CoordinateSystem.ED50, Limit.of(1)))
        .thenReturn(List.of(feature));

    assertThat(featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(CoordinateSystem.ED50))
        .isEqualTo(feature);
  }

  @Test
  void findFeatureWithNoJourneyStateOrThrow_whenNotFound_throws() {
    when(featureJourneyStateRepository.findFeaturesWithNoJourneyState(CoordinateSystem.ED50, Limit.of(1)))
        .thenReturn(List.of());

    assertThatThrownBy(() -> featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(CoordinateSystem.ED50))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Feature with coordinate system %s not found".formatted(CoordinateSystem.ED50));
  }

  @Test
  void findJourneyForFeature_whenStateFound_returnsCommandJourney() {
    var feature = FeatureTestUtil.newBuilder().build();
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var state = FeatureJourneyStateTestUtil.newBuilder()
        .withFeature(feature)
        .withCommandJourney(commandJourney)
        .build();

    when(featureJourneyStateRepository.findFirstByFeature_IdAndCreatedByCommandIsNull(feature.getId()))
        .thenReturn(Optional.of(state));

    assertThat(featureJourneyStateService.findJourneyForFeature(feature)).contains(commandJourney);
  }

  @Test
  void findJourneyForFeature_whenNoStateFound_returnsEmpty() {
    var feature = FeatureTestUtil.newBuilder().build();

    when(featureJourneyStateRepository.findFirstByFeature_IdAndCreatedByCommandIsNull(feature.getId()))
        .thenReturn(Optional.empty());

    assertThat(featureJourneyStateService.findJourneyForFeature(feature)).isEmpty();
  }
}

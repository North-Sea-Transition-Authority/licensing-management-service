package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
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
  void createFeatureJourneyStatesForCommandOutput_createsActiveStateWithCreatingCommand() {
    var commandJourney = CommandJourneyTestUtil.newBuilder().build();
    var command = OperatorCommandTestUtil.newBuilder().build();
    var outputFeature = FeatureTestUtil.newBuilder().build();

    when(featureJourneyStateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(
        commandJourney, command, List.of(outputFeature));

    verify(featureJourneyStateRepository).saveAll(statesCaptor.capture());
    assertThat(statesCaptor.getValue())
        .extracting(
            FeatureJourneyState::getFeature,
            FeatureJourneyState::getCommandJourney,
            FeatureJourneyState::getCreatedByCommand,
            FeatureJourneyState::isActive
        )
        .containsExactly(tuple(outputFeature, commandJourney, command, true));
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
}

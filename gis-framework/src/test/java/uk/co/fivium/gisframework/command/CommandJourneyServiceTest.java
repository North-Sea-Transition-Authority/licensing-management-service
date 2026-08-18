package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class CommandJourneyServiceTest {

  @Mock
  private CommandJourneyRepository commandJourneyRepository;

  @Mock
  private FeatureJourneyStateService featureJourneyStateService;

  @InjectMocks
  private CommandJourneyService commandJourneyService;

  @Test
  void createAndAssignCommandJourney_createsInitialFeatureJourneyStates() {
    var feature1 = FeatureTestUtil.newBuilder().build();
    var feature2 = FeatureTestUtil.newBuilder().build();
    var savedJourney = new CommandJourney();

    when(commandJourneyRepository.save(any(CommandJourney.class))).thenReturn(savedJourney);

    var result = commandJourneyService.createAndAssignCommandJourney(List.of(feature1, feature2));

    assertThat(result).isEqualTo(savedJourney);
    verify(featureJourneyStateService).createInitialFeatureJourneyStates(savedJourney, List.of(feature1, feature2));
  }

  @Test
  void getCommandJourneyOrThrow_whenFound_returnsJourney() {
    var journeyId = UUID.randomUUID();
    var journey = new CommandJourney();
    when(commandJourneyRepository.findById(journeyId)).thenReturn(Optional.of(journey));

    assertThat(commandJourneyService.getCommandJourneyOrThrow(journeyId)).isEqualTo(journey);
  }

  @Test
  void getCommandJourneyOrThrow_whenNotFound_throws() {
    var journeyId = UUID.randomUUID();
    when(commandJourneyRepository.findById(journeyId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commandJourneyService.getCommandJourneyOrThrow(journeyId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getActiveFeatures_delegatesToFeatureJourneyStateService() {
    var journey = new CommandJourney();
    var activeFeature = FeatureTestUtil.newBuilder().build();

    when(featureJourneyStateService.getActiveFeatures(journey)).thenReturn(List.of(activeFeature));

    assertThat(commandJourneyService.getActiveFeatures(journey)).containsExactly(activeFeature);
  }

  @Test
  void getActiveFeaturesByJourneyId_whenJourneyFound_returnsActiveFeatures() {
    var journeyId = UUID.randomUUID();
    var journey = new CommandJourney();
    var activeFeature = FeatureTestUtil.newBuilder().build();

    when(commandJourneyRepository.findById(journeyId)).thenReturn(Optional.of(journey));
    when(featureJourneyStateService.getActiveFeatures(journey)).thenReturn(List.of(activeFeature));

    assertThat(commandJourneyService.getActiveFeatures(journeyId)).containsExactly(activeFeature);
  }

  @Test
  void getActiveFeaturesByJourneyId_whenJourneyNotFound_throws() {
    var journeyId = UUID.randomUUID();
    when(commandJourneyRepository.findById(journeyId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commandJourneyService.getActiveFeatures(journeyId))
        .isInstanceOf(EntityNotFoundException.class);
  }
}

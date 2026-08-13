package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@DataJpaTest
@ActiveProfiles("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeatureJourneyStateRepositoryIntegrationTest {

  @Autowired
  private FeatureJourneyStateRepository featureJourneyStateRepository;

  @Autowired
  private CommandJourneyRepository commandJourneyRepository;

  @Autowired
  private OperatorCommandRepository operatorCommandRepository;

  @Autowired
  private TestEntityManager entityManager;

  private CommandJourney commandJourney;

  private CommandJourney otherCommandJourney;

  private OperatorCommand operatorCommand;

  @BeforeEach
  void setup() {
    commandJourney = commandJourneyRepository.save(CommandJourneyTestUtil.newBuilder().withId(null).build());
    otherCommandJourney = commandJourneyRepository.save(CommandJourneyTestUtil.newBuilder().withId(null).build());

    var inputFeature = persistFeature();
    operatorCommand = operatorCommandRepository.save(OperatorCommandTestUtil.newBuilder()
        .withId(null)
        .withCommandJourney(commandJourney)
        .withInputFeatureIds(Set.of(inputFeature.getId()))
        .build());
  }

  @Test
  void findActiveFeaturesByCommandJourney_returnsOnlyActiveFeaturesForThatJourney() {
    var activeFeature = persistFeature();
    var inactiveFeature = persistFeature();
    var otherJourneyFeature = persistFeature();

    featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(activeFeature)
        .withCommandJourney(commandJourney)
        .withActive(true)
        .build());
    featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(inactiveFeature)
        .withCommandJourney(commandJourney)
        .withActive(false)
        .build());
    featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(otherJourneyFeature)
        .withCommandJourney(otherCommandJourney)
        .withActive(true)
        .build());

    var result = featureJourneyStateRepository.findActiveFeaturesByCommandJourney(commandJourney);

    assertThat(result).containsExactly(activeFeature);
  }

  @Test
  void findAllByCreatedByCommand_returnsStatesCreatedByThatCommand() {
    var outputFeature1 = persistFeature();
    var outputFeature2 = persistFeature();
    var unrelatedFeature = persistFeature();

    var state1 = featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(outputFeature1)
        .withCommandJourney(commandJourney)
        .withCreatedByCommand(operatorCommand)
        .build());
    var state2 = featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(outputFeature2)
        .withCommandJourney(commandJourney)
        .withCreatedByCommand(operatorCommand)
        .build());
    featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(unrelatedFeature)
        .withCommandJourney(commandJourney)
        .withCreatedByCommand(null)
        .build());

    var result = featureJourneyStateRepository.findAllByCreatedByCommand(operatorCommand);

    assertThat(result).containsExactlyInAnyOrder(state1, state2);
  }

  @Test
  void findFeaturesWithNoJourneyState_returnsOnlyFeaturesWithoutAState() {
    var featureWithNoState = persistFeature();
    var featureWithState = persistFeature();

    featureJourneyStateRepository.save(FeatureJourneyStateTestUtil.newBuilder()
        .withId(null)
        .withFeature(featureWithState)
        .withCommandJourney(commandJourney)
        .build());

    var result = featureJourneyStateRepository.findFeaturesWithNoJourneyState(CoordinateSystem.ED50, Limit.of(10));

    assertThat(result).contains(featureWithNoState).doesNotContain(featureWithState);
  }

  @Test
  void findFeaturesWithNoJourneyState_filtersByCoordinateSystem() {
    var otherCoordinateSystemFeature = persistFeature(CoordinateSystem.WGS84);

    var result = featureJourneyStateRepository.findFeaturesWithNoJourneyState(CoordinateSystem.ED50, Limit.of(10));

    assertThat(result).doesNotContain(otherCoordinateSystemFeature);
  }

  @Test
  void findFeaturesWithNoJourneyState_respectsLimit() {
    persistFeature();
    persistFeature();

    var result = featureJourneyStateRepository.findFeaturesWithNoJourneyState(CoordinateSystem.ED50, Limit.of(1));

    assertThat(result).hasSize(1);
  }

  private Feature persistFeature() {
    return persistFeature(CoordinateSystem.ED50);
  }

  private Feature persistFeature(CoordinateSystem coordinateSystem) {
    var feature = FeatureTestUtil.newBuilder().withId(null).withCoordinateSystem(coordinateSystem).build();
    entityManager.persist(feature);
    return feature;
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = {FeatureJourneyState.class, Feature.class, CommandJourney.class, OperatorCommand.class})
  @EnableJpaRepositories(basePackageClasses = FeatureJourneyStateRepository.class)
  static class TestApplication {
  }
}

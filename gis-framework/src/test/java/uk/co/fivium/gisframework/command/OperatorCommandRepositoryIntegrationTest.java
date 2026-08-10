package uk.co.fivium.gisframework.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OperatorCommandRepositoryIntegrationTest {

  @Autowired
  private OperatorCommandRepository operatorCommandRepository;

  @Autowired
  private CommandJourneyRepository commandJourneyRepository;

  private CommandJourney commandJourneyWithCommands;

  private CommandJourney commandJourneyWithNoCommands;

  @BeforeEach
  void setup() {
    commandJourneyWithCommands = CommandJourneyTestUtil.newBuilder()
        .withId(null)
        .build();
    commandJourneyWithNoCommands = CommandJourneyTestUtil.newBuilder()
        .withId(null)
        .build();

    commandJourneyRepository.saveAll(
        List.of(commandJourneyWithCommands, commandJourneyWithNoCommands)
    );

    var command1 = OperatorCommandTestUtil.newBuilder()
        .withId(null)
        .withCommandJourney(commandJourneyWithCommands)
        .withCommandOrder(1)
        .build();
    var command2 = OperatorCommandTestUtil.newBuilder()
        .withId(null)
        .withCommandJourney(commandJourneyWithCommands)
        .withCommandOrder(3)
        .build();

    operatorCommandRepository.saveAll(
        List.of(command1, command2)
    );
  }

  @Test
  void findMaxCommandOrderByCommandJourney_whenCommandsExistForJourney_assertMaxCommandOrderReturned() {
    var result = operatorCommandRepository.findMaxCommandOrderByCommandJourney(commandJourneyWithCommands);

    assertThat(result).contains(3);
  }

  @Test
  void findMaxCommandOrderByCommandJourney_whenNoCommandsExistForJourney_assertEmptyOptionalReturned() {
    var result = operatorCommandRepository.findMaxCommandOrderByCommandJourney(commandJourneyWithNoCommands);

    assertThat(result).isEmpty();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = {OperatorCommand.class, CommandJourney.class})
  @EnableJpaRepositories(basePackageClasses = OperatorCommandRepository.class)
  static class TestApplication {
  }
}

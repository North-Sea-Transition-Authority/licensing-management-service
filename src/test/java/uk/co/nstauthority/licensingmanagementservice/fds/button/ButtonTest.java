package uk.co.nstauthority.licensingmanagementservice.fds.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ButtonTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("buttons")
  void button(Button actual, Button expected) {
    assertThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> buttons() {
    return Stream.of(
        arguments(Button.primaryButton("Continue"), new Button("Continue", "govuk-button")),
        arguments(Button.warningButton("Continue"), new Button("Continue", "govuk-button govuk-button--warning"))
    );
  }

}
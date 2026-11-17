package uk.co.nstauthority.licensingmanagementservice.fds.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TagTest {

  @ParameterizedTest
  @CsvSource({
      "GREY, govuk-tag--grey",
      "GREEN, govuk-tag--green",
      "TURQUOISE, govuk-tag--turquoise",
      "BLUE, govuk-tag--blue",
      "LIGHT_BLUE, govuk-tag--light-blue",
      "PURPLE, govuk-tag--purple",
      "PINK, govuk-tag--pink",
      "RED, govuk-tag--red",
      "ORANGE, govuk-tag--orange",
      "YELLOW, govuk-tag--yellow"
  })
  void tagClass(TagColour tagColour, String expectedTagClass) {
    var tag = new Tag("text", tagColour);

    var result = tag.tagClass();

    assertThat(result).isEqualTo(expectedTagClass);
  }
}

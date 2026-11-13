package uk.co.nstauthority.licensingmanagementservice.fds.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TagTest {

  @ParameterizedTest
  @CsvSource({
      "RED, govuk-tag--red",
      "YELLOW, govuk-tag--yellow",
      "GREY, govuk-tag--grey",
      "BLUE, govuk-tag--blue",
      "PURPLE, govuk-tag--purple"
  })
  void tagClass(TagColour tagColour, String expectedTagClass) {
    var tag = new Tag("text", tagColour);

    var result = tag.tagClass();

    assertThat(result).isEqualTo(expectedTagClass);
  }
}

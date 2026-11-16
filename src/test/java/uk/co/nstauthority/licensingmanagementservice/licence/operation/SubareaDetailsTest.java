package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

class SubareaDetailsTest {

  @Test
  void from() {
    var feature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "A");

    var result = SubareaDetails.from(feature);

    assertThat(result).isEqualTo(new SubareaDetails(feature.getId(), "SHAPE A", "A"));
  }

  @ParameterizedTest
  @MethodSource("displayNames")
  void displayName(String name, String expected) {
    var subareaDetails = new SubareaDetails(UUID.randomUUID(), name, "A");

    var result = subareaDetails.displayName();

    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> displayNames() {
    return Stream.of(
        Arguments.of(null, "Subarea A"),
        Arguments.of("", "Subarea A"),
        Arguments.of("   ", "Subarea A"),
        Arguments.of("SUBAREA A", "SUBAREA A"),
        Arguments.of("Subarea A", "Subarea A"),
        Arguments.of("subarea a", "subarea a"),
        Arguments.of("SHAPE A", "Subarea SHAPE A")
    );
  }
}

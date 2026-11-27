package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

  @ParameterizedTest
  @MethodSource("shortNames")
  void hasDuplicateShortNames(List<String> shortNames, boolean expected) {
    var result = SubareaDetails.hasDuplicateShortNames(withShortNames(shortNames));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void hasDuplicateNames_whenMapDisplayNamesRepeat_thenTrue() {
    var subareas = List.of(
        new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"),
        new SubareaDetails(UUID.randomUUID(), " SUBAREA A ", "B")
    );

    var result = SubareaDetails.hasDuplicateNames(subareas);

    assertThat(result).isTrue();
  }

  @Test
  void hasDuplicateNames_whenMapDisplayNamesUnique_thenFalse() {
    var subareas = List.of(
        new SubareaDetails(UUID.randomUUID(), "Subarea A1", "A"),
        new SubareaDetails(UUID.randomUUID(), "Subarea A2", "A")
    );

    var result = SubareaDetails.hasDuplicateNames(subareas);

    assertThat(result).isFalse();
  }

  private static Stream<Arguments> shortNames() {
    return Stream.of(
        Arguments.of(List.of("A", "B"), false),
        Arguments.of(List.of("A", " a "), true),
        Arguments.of(List.of("A", "B", "B"), true),
        Arguments.of(List.of(" ", " ", "A"), false)
    );
  }

  private static List<SubareaDetails> withShortNames(List<String> shortNames) {
    return shortNames.stream()
        .map(shortName -> new SubareaDetails(UUID.randomUUID(), "Subarea " + UUID.randomUUID(), shortName))
        .toList();
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

package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonFeatureRenameTest {

  @ParameterizedTest
  @MethodSource("namesAndExpectedNames")
  void constructor_whenNamesProvided_thenTrimsNames(String providedName, String expectedName) {
    var featureId = UUID.randomUUID();

    var featureRename = new JsonFeatureRename(featureId, providedName, providedName);

    assertThat(featureRename.featureId()).isEqualTo(featureId);
    assertThat(featureRename.featureName()).isEqualTo(expectedName);
    assertThat(featureRename.name()).isEqualTo(expectedName);
  }

  private static Stream<Arguments> namesAndExpectedNames() {
    return Stream.of(
        Arguments.of(" Alpha ", "Alpha"),
        Arguments.of("Alpha", "Alpha"),
        Arguments.of("   ", ""),
        Arguments.of(null, null)
    );
  }
}

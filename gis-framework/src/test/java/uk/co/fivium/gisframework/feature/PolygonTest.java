package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolygonTest {

  @ParameterizedTest
  @CsvSource(nullValues = "null", value = {
      "-100, -200, -100, true",
      "-100, -200, -150, true",
      "-100, -200, -200, false",
      "-100, -200, -50, false",
      "-100, -200, -250, false",
      "null, -200, 0, true",
      "null, -200, 10, false",
      "-100, null, -100000, true",
      "null, null, -100000, true"
  })
  void coversDepth(Long startDepth, Long endDepth, long depth, boolean expectedCoversDepth) {
    var polygon = PolygonTestUtil.newBuilder()
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();

    var result = polygon.coversDepth(depth);

    assertThat(result).isEqualTo(expectedCoversDepth);
  }
}

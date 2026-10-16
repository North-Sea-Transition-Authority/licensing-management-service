package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.operator.EsriJsonPolygonWithSources.DepthRange;

class EsriJsonPolygonWithSourcesTest {

  private static final String SHAPE_X = "shape x";
  private static final String SHAPE_Y = "shape y";
  private static final String COMBINED_SHAPE = "combined shape";

  @Test
  void combine() {
    var upper = polygon(0L, -100L);
    var middle = polygon(-100L, -200L);
    var lower = polygon(-200L, -300L);

    var result = EsriJsonPolygonWithSources.combine(COMBINED_SHAPE, List.of(
        new EsriJsonPolygonWithSources(SHAPE_X, List.of(upper, middle)),
        new EsriJsonPolygonWithSources(SHAPE_Y, List.of(lower))
    ));

    assertThat(result).isEqualTo(
        new EsriJsonPolygonWithSources(COMBINED_SHAPE, List.of(upper, middle, lower), new DepthRange(0L, -300L))
    );
  }

  @Test
  void depthRangeOf_whenSingleSource_thenSourceDepths() {
    var source = polygon(-50L, -150L);

    var result = DepthRange.of(List.of(source));

    assertThat(result).isEqualTo(new DepthRange(-50L, -150L));
  }

  @Test
  void depthRangeOf_whenSeveralSources_thenSpansAllSources() {
    var middle = polygon(-100L, -200L);
    var upper = polygon(0L, -100L);
    var lower = polygon(-200L, -300L);

    var result = DepthRange.of(List.of(middle, upper, lower));

    assertThat(result).isEqualTo(new DepthRange(0L, -300L));
  }

  @Test
  void depthRangeOf_whenDepthsNotSet_thenUsesEffectiveDepths() {
    var source = polygon(null, null);

    var result = DepthRange.of(List.of(source));

    assertThat(result).isEqualTo(new DepthRange(0L, Polygon.INFINITE_DEPTH));
  }

  @ParameterizedTest
  @CsvSource({
      "0, -100, -100, -200, true",
      "-100, -200, 0, -100, true",
      "0, -100, -150, -200, false",
      "0, -100, -50, -150, false",
      "0, -100, 0, -100, false"
  })
  void depthRangeMeets(long top, long bottom, long otherTop, long otherBottom, boolean expectedMeets) {
    var depthRange = new DepthRange(top, bottom);

    var result = depthRange.meets(new DepthRange(otherTop, otherBottom));

    assertThat(result).isEqualTo(expectedMeets);
  }

  private static Polygon polygon(Long startDepth, Long endDepth) {
    return PolygonTestUtil.newBuilder()
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
  }
}

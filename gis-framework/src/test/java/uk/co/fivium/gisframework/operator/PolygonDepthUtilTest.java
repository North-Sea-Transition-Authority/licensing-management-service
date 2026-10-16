package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;

class PolygonDepthUtilTest {

  @Test
  void topSource_whenSeveralSources_thenReturnsSourceWithHighestStartDepth() {
    var middle = polygon(-100L, -200L);
    var upper = polygon(0L, -100L);
    var lower = polygon(-200L, -300L);

    var result = PolygonDepthUtil.topSource(List.of(middle, upper, lower));

    assertThat(result).isSameAs(upper);
  }

  @Test
  void topSource_whenStartDepthNull_thenTreatedAsZero() {
    var lower = polygon(-50L, -100L);
    var unsetStart = polygon(null, -100L);

    var result = PolygonDepthUtil.topSource(List.of(lower, unsetStart));

    assertThat(result).isSameAs(unsetStart);
  }

  @Test
  void topSource_whenStartDepthsTie_thenReturnsFirstSource() {
    var zeroStart = polygon(0L, -100L);
    var unsetStart = polygon(null, -100L);

    var result = PolygonDepthUtil.topSource(List.of(zeroStart, unsetStart));

    assertThat(result).isSameAs(zeroStart);
  }

  @Test
  void bottomSource_whenSeveralSources_thenReturnsSourceWithLowestEndDepth() {
    var middle = polygon(-100L, -200L);
    var lower = polygon(-200L, -300L);
    var upper = polygon(0L, -100L);

    var result = PolygonDepthUtil.bottomSource(List.of(middle, lower, upper));

    assertThat(result).isSameAs(lower);
  }

  @Test
  void bottomSource_whenEndDepthNull_thenTreatedAsInfinite() {
    var unsetEnd = polygon(0L, null);
    var deep = polygon(0L, -100_000L);

    var result = PolygonDepthUtil.bottomSource(List.of(deep, unsetEnd));

    assertThat(result).isSameAs(unsetEnd);
  }

  @Test
  void bottomSource_whenEndDepthsTie_thenReturnsFirstSource() {
    var first = polygon(0L, -100L);
    var second = polygon(-50L, -100L);

    var result = PolygonDepthUtil.bottomSource(List.of(first, second));

    assertThat(result).isSameAs(first);
  }

  private static Polygon polygon(Long startDepth, Long endDepth) {
    return PolygonTestUtil.newBuilder()
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
  }
}

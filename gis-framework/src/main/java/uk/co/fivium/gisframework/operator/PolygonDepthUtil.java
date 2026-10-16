package uk.co.fivium.gisframework.operator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.co.fivium.gisframework.feature.Polygon;

public final class PolygonDepthUtil {

  private PolygonDepthUtil() {
  }

  public static Polygon topSource(List<Polygon> sources) {
    return Collections.max(sources, Comparator.comparingLong(Polygon::getEffectiveStartDepth));
  }

  public static Polygon bottomSource(List<Polygon> sources) {
    return Collections.min(sources, Comparator.comparingLong(Polygon::getEffectiveEndDepth));
  }
}

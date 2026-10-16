package uk.co.fivium.gisframework.operator;

import java.util.List;
import java.util.UUID;
import uk.co.fivium.gisframework.feature.Polygon;

public record EsriJsonPolygonWithSources(String esriJson, List<Polygon> sources, DepthRange depthRange) {

  public record DepthRange(long top, long bottom) {

    public static DepthRange of(List<Polygon> sources) {
      return new DepthRange(
          PolygonDepthUtil.topSource(sources).getEffectiveStartDepth(),
          PolygonDepthUtil.bottomSource(sources).getEffectiveEndDepth()
      );
    }

    public boolean meets(DepthRange other) {
      return bottom == other.top || other.bottom == top;
    }
  }

  EsriJsonPolygonWithSources(String esriJson, List<Polygon> sources) {
    this(esriJson, sources, DepthRange.of(sources));
  }

  public UUID id() {
    return sources.getFirst().getId();
  }

  public static EsriJsonPolygonWithSources combine(String esriJson, List<EsriJsonPolygonWithSources> polygons) {
    var sources = polygons.stream()
        .flatMap(polygon -> polygon.sources().stream())
        .toList();
    return new EsriJsonPolygonWithSources(esriJson, sources);
  }
}

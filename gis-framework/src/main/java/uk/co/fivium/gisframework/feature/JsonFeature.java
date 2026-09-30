package uk.co.fivium.gisframework.feature;

import java.util.Map;

public record JsonFeature(
    Map<String, Object> geometry,
    Attributes attributes
) {

  public record Attributes(
      String featureId,
      String featureName,
      String polygonId,
      Long startDepth,
      Long endDepth
  ) {
    public static Attributes from(Feature feature, Polygon polygon) {
      return new Attributes(
          feature.getId().toString(),
          feature.getFeatureName(),
          polygon.getId().toString(),
          polygon.getStartDepth(),
          polygon.getEndDepth()
      );
    }
  }
}

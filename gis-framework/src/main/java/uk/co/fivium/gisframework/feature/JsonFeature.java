package uk.co.fivium.gisframework.feature;

import java.util.Map;

public record JsonFeature(
    Map<String, Object> geometry,
    Attributes attributes
) {

  public record Attributes(
      String featureId,
      String featureName
  ) {
    public static Attributes from(Feature feature) {
      return new Attributes(feature.getId().toString(), feature.getFeatureName());
    }
  }
}

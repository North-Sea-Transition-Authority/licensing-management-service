package uk.co.fivium.gisframework.feature;

import java.util.Map;

record JsonFeature(
    Map<String, Object> geometry,
    Attributes attributes
) {

  record Attributes(
      String featureId,
      String featureName
  ) {
    static Attributes from(Feature feature) {
      return new Attributes(feature.getId().toString(), feature.getFeatureName());
    }
  }
}

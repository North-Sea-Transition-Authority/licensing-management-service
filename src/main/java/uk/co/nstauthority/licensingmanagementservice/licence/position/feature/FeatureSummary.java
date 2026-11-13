package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import java.math.BigDecimal;
import uk.co.fivium.gisframework.feature.Feature;

public record FeatureSummary(String name, BigDecimal area) {

  public static FeatureSummary from(Feature feature) {
    return new FeatureSummary(feature.getFeatureName(), feature.getFeatureArea());
  }
}

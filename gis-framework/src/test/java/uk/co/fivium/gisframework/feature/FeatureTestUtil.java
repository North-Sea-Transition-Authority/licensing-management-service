package uk.co.fivium.gisframework.feature;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import uk.co.fivium.grpc.gis.CoordinateSystem;

public class FeatureTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private Integer legacyId = 1;
    private String featureName = "Test Feature";
    private String testCase = "TC";
    private CoordinateSystem coordinateSystem = CoordinateSystem.ED50;
    private BigDecimal featureArea = BigDecimal.valueOf(100.0);
    private Feature parentFeature = null;
    private Map<String, String> attributes = Map.of();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLegacyId(Integer legacyId) {
      this.legacyId = legacyId;
      return this;
    }

    public Builder withFeatureName(String featureName) {
      this.featureName = featureName;
      return this;
    }

    public Builder withTestCase(String testCase) {
      this.testCase = testCase;
      return this;
    }

    public Builder withCoordinateSystem(CoordinateSystem coordinateSystem) {
      this.coordinateSystem = coordinateSystem;
      return this;
    }

    public Builder withFeatureArea(BigDecimal featureArea) {
      this.featureArea = featureArea;
      return this;
    }

    public Builder withParentFeature(Feature parentFeature) {
      this.parentFeature = parentFeature;
      return this;
    }

    public Builder withAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Feature build() {
      var feature = new Feature(id);
      feature.setLegacyId(legacyId);
      feature.setFeatureName(featureName);
      feature.setTestCase(testCase);
      feature.setCoordinateSystem(coordinateSystem);
      feature.setFeatureArea(featureArea);
      feature.setParentFeature(parentFeature);
      feature.setAttributes(attributes);
      return feature;
    }
  }
}



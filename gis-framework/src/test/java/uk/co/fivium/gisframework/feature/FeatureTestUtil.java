package uk.co.fivium.gisframework.feature;

import java.math.BigDecimal;
import java.util.UUID;
import uk.co.fivium.grpc.CoordinateSystem;

public class FeatureTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String featureName = "Test Feature";
    private FeatureType type = FeatureType.POLYGON;
    private CoordinateSystem coordinateSystem = CoordinateSystem.ED50;
    private BigDecimal featureArea = BigDecimal.valueOf(100.0);
    private Feature parentFeature = null;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withFeatureName(String featureName) {
      this.featureName = featureName;
      return this;
    }

    public Builder withType(FeatureType type) {
      this.type = type;
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

    public Feature build() {
      var feature = new Feature(id);
      feature.setFeatureName(featureName);
      feature.setType(type);
      feature.setCoordinateSystem(coordinateSystem);
      feature.setFeatureArea(featureArea);
      feature.setParentFeature(parentFeature);
      return feature;
    }
  }
}



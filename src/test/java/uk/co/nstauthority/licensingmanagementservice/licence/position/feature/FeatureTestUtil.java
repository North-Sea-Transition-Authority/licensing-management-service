package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.fivium.grpc.gis.CoordinateSystem;

/**
 * This util is copied from the gis-framework package, since we can't access it from this package.
 */
public class FeatureTestUtil {

  private FeatureTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static Feature blockFeature(UUID id, String quadrantNumber, int blockNumber) {
    return builder()
        .withId(id)
        .withFeatureName("SHAPE %s".formatted(blockNumber))
        .withAttributes(Map.of(
            "LAYER", Layer.BLOCKS.name(),
            "NAME", "%s/%s".formatted(quadrantNumber, blockNumber),
            "QUADRANT_NO", quadrantNumber,
            "BLOCK_NO", String.valueOf(blockNumber)))
        .build();
  }

  public static Feature subareaFeature(UUID id, String name) {
    return builder()
        .withId(id)
        .withFeatureName("SHAPE %s".formatted(name))
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name(), "NAME", name))
        .build();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String featureName = "SHAPE 1";
    private Map<String, String> attributes = Map.of("LAYER", Layer.BLOCKS.name());
    private CoordinateSystem coordinateSystem = CoordinateSystem.ED50;
    private BigDecimal featureArea = BigDecimal.valueOf(100);

    private Builder() {}

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withFeatureName(String featureName) {
      this.featureName = featureName;
      return this;
    }

    public Builder withAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
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

    public Feature build() {
      var feature = new Feature();
      ReflectionTestUtils.setField(feature, "id", id);
      feature.setFeatureName(featureName);
      feature.setAttributes(attributes);
      feature.setCoordinateSystem(coordinateSystem);
      feature.setFeatureArea(featureArea);

      return feature;
    }
  }
}

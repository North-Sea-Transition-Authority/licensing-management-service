package uk.co.fivium.gisframework.feature;

import java.util.Map;
import java.util.UUID;

public class PolygonTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private Integer legacyId = 1;
    private Feature feature = FeatureTestUtil.newBuilder().build();
    private Map<String, Object> attributes = Map.of();
    private Long startDepth = 0L;
    private Long endDepth = 100L;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLegacyId(Integer legacyId) {
      this.legacyId = legacyId;
      return this;
    }

    public Builder withFeature(Feature feature) {
      this.feature = feature;
      return this;
    }

    public Builder withAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder withStartDepth(Long startDepth) {
      this.startDepth = startDepth;
      return this;
    }

    public Builder withEndDepth(Long endDepth) {
      this.endDepth = endDepth;
      return this;
    }

    public Polygon build() {
      var polygon = new Polygon(id);
      polygon.setLegacyId(legacyId);
      polygon.setFeature(feature);
      polygon.setAttributes(attributes);
      polygon.setStartDepth(startDepth);
      polygon.setEndDepth(endDepth);
      return polygon;
    }
  }
}


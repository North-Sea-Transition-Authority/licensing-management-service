package uk.co.fivium.gisframework.feature;

import java.util.Map;
import java.util.UUID;
import uk.co.fivium.grpc.LineNavigationType;

public class LineTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private Polygon polygon = PolygonTestUtil.newBuilder().build();
    private LineNavigationType navigationType = LineNavigationType.GEODESIC;
    private Integer ringNumber = 1;
    private Integer ringConnectionOrder = 1;
    private String esriJson = """
        {"spatialReference":{"wkid":4230},"paths":[[[2.8,53.95],[2.81666666666667,53.95]]]}
        """;
    private Map<String, Object> attributes = Map.of();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withPolygon(Polygon polygon) {
      this.polygon = polygon;
      return this;
    }

    public Builder withNavigationType(LineNavigationType navigationType) {
      this.navigationType = navigationType;
      return this;
    }

    public Builder withRingNumber(Integer ringNumber) {
      this.ringNumber = ringNumber;
      return this;
    }

    public Builder withRingConnectionOrder(Integer ringConnectionOrder) {
      this.ringConnectionOrder = ringConnectionOrder;
      return this;
    }

    public Builder withEsriJson(String esriJson) {
      this.esriJson = esriJson;
      return this;
    }

    public Builder withAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Line build() {
      var line = new Line(id);
      line.setPolygon(polygon);
      line.setNavigationType(navigationType);
      line.setRingNumber(ringNumber);
      line.setRingConnectionOrder(ringConnectionOrder);
      line.setEsriJson(esriJson);
      line.setAttributes(attributes);
      return line;
    }
  }
}


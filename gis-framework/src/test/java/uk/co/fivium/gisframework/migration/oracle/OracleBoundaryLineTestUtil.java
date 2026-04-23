package uk.co.fivium.gisframework.migration.oracle;

import uk.co.fivium.grpc.LineNavigationType;

public class OracleBoundaryLineTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Long lineSidId = 1L;
    private Long boundarySidId = 1L;
    private Long connectionOrder = 1L;
    private LineNavigationType lineNavigationType = LineNavigationType.GEODESIC;
    private String lineGeojson = "{\"type\":\"LineString\"}";

    private Builder() {
    }

    public Builder withLineSidId(Long lineSidId) {
      this.lineSidId = lineSidId;
      return this;
    }

    public Builder withBoundarySidId(Long boundarySidId) {
      this.boundarySidId = boundarySidId;
      return this;
    }

    public Builder withConnectionOrder(Long connectionOrder) {
      this.connectionOrder = connectionOrder;
      return this;
    }

    public Builder withLineNavigationType(LineNavigationType lineNavigationType) {
      this.lineNavigationType = lineNavigationType;
      return this;
    }

    public Builder withLineGeojson(String lineGeojson) {
      this.lineGeojson = lineGeojson;
      return this;
    }

    public OracleBoundaryLine build() {
      var line = new OracleBoundaryLine();
      line.setLineSidId(lineSidId);
      line.setBoundarySidId(boundarySidId);
      line.setConnectionOrder(connectionOrder);
      line.setLineNavigationType(lineNavigationType);
      line.setLineGeojson(lineGeojson);
      return line;
    }
  }
}


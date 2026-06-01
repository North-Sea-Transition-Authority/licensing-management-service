package uk.co.fivium.gisframework.migration.oracle;

import uk.co.fivium.grpc.gis.LineNavigationType;

public class OracleBoundaryLineTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer lineSidId = 1;
    private Integer oraclePolygonBoundaryId = 1;
    private Integer shapeSiId = 1;
    private Long connectionOrder = 1L;
    private LineNavigationType lineNavigationType = LineNavigationType.GEODESIC;
    private String lineGeojson = "{\"type\":\"LineString\"}";

    public Builder withLineSidId(Integer lineSidId) {
      this.lineSidId = lineSidId;
      return this;
    }

    public Builder withOraclePolygonBoundaryId(Integer oraclePolygonBoundaryId) {
      this.oraclePolygonBoundaryId = oraclePolygonBoundaryId;
      return this;
    }

    public Builder withShapeSiId(Integer shapeSiId) {
      this.shapeSiId = shapeSiId;
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
      line.setOraclePolygonBoundaryId(oraclePolygonBoundaryId);
      line.setShapeSiId(shapeSiId);
      line.setConnectionOrder(connectionOrder);
      line.setLineNavigationType(lineNavigationType);
      line.setLineGeojson(lineGeojson);
      return line;
    }
  }
}


package uk.co.fivium.gisframework.migration.oracle;

public class OracleShapePolygonTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer polygonSidId = 1;
    private Integer shapeSidId = 1;
    private Integer oracleShapeId = 1;
    private Long featureOffsetLowM = 0L;
    private Long featureOffsetHighM = 100L;

    public Builder withPolygonSidId(Integer polygonSidId) {
      this.polygonSidId = polygonSidId;
      return this;
    }

    public Builder withShapeSidId(Integer shapeSidId) {
      this.shapeSidId = shapeSidId;
      return this;
    }

    public Builder withOracleShapeId(Integer oracleShapeId) {
      this.oracleShapeId = oracleShapeId;
      return this;
    }

    public Builder withFeatureOffsetLowM(Long featureOffsetLowM) {
      this.featureOffsetLowM = featureOffsetLowM;
      return this;
    }

    public Builder withFeatureOffsetHighM(Long featureOffsetHighM) {
      this.featureOffsetHighM = featureOffsetHighM;
      return this;
    }

    public OracleShapePolygon build() {
      var polygon = new OracleShapePolygon();
      polygon.setPolygonSidId(polygonSidId);
      polygon.setShapeSidId(shapeSidId);
      polygon.setOracleShapeId(oracleShapeId);
      polygon.setFeatureOffsetLowM(featureOffsetLowM);
      polygon.setFeatureOffsetHighM(featureOffsetHighM);
      return polygon;
    }
  }
}


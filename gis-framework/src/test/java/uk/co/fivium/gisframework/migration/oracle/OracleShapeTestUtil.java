package uk.co.fivium.gisframework.migration.oracle;

import java.time.LocalDate;

public class OracleShapeTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer shapeSidId = 1;
    private Integer shapeSiId = 1;
    private OracleLayer oracleLayer = OracleLayerTestUtil.newBuilder().build();
    private String shapeName = "Test Shape";
    private String shapeSrs = "ED 50";
    private Double shareAreaM2 = 100.0;
    private LocalDate shapeStartDate = LocalDate.of(2026, 1, 1);
    private LocalDate shapeEndDate = LocalDate.of(2026, 2, 2);

    public Builder withShapeSidId(Integer shapeSidId) {
      this.shapeSidId = shapeSidId;
      return this;
    }

    public Builder withShapeSiId(Integer shapeSiId) {
      this.shapeSiId = shapeSiId;
      return this;
    }

    public Builder withOracleLayer(OracleLayer oracleLayer) {
      this.oracleLayer = oracleLayer;
      return this;
    }

    public Builder withShapeName(String shapeName) {
      this.shapeName = shapeName;
      return this;
    }

    public Builder withShapeSrs(String shapeSrs) {
      this.shapeSrs = shapeSrs;
      return this;
    }

    public Builder withShareAreaM2(Double shareAreaM2) {
      this.shareAreaM2 = shareAreaM2;
      return this;
    }

    public Builder withShapeStartDate(LocalDate shapeStartDate) {
      this.shapeStartDate = shapeStartDate;
      return this;
    }

    public Builder withShapeEndDate(LocalDate shapeEndDate) {
      this.shapeEndDate = shapeEndDate;
      return this;
    }

    public OracleShape build() {
      var shape = new OracleShape();
      shape.setShapeSidId(shapeSidId);
      shape.setShapeSiId(shapeSiId);
      shape.setOracleLayer(oracleLayer);
      shape.setShapeName(shapeName);
      shape.setShapeSrs(shapeSrs);
      shape.setShareAreaM2(shareAreaM2);
      shape.setShapeStartDate(shapeStartDate);
      shape.setShapeEndDate(shapeEndDate);
      return shape;
    }
  }
}


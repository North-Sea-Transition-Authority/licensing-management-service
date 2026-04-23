package uk.co.fivium.gisframework.migration.oracle;

public class OracleShapeTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer shapeSidId = 1;
    private String testCase = "TC1";
    private String shapeName = "Test Shape";
    private String shapeSrs = "EPSG:27700";
    private Double shareAreaM2 = 100.0;
    private Integer parentShapeId = null;
    private ShapeType shapeType = ShapeType.SUBAREA;

    private Builder() {
    }

    public Builder withShapeSidId(Integer shapeSidId) {
      this.shapeSidId = shapeSidId;
      return this;
    }

    public Builder withTestCase(String testCase) {
      this.testCase = testCase;
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

    public Builder withParentShapeId(Integer parentShapeId) {
      this.parentShapeId = parentShapeId;
      return this;
    }

    public Builder withShapeType(ShapeType shapeType) {
      this.shapeType = shapeType;
      return this;
    }

    public OracleShape build() {
      var shape = new OracleShape();
      shape.setShapeSidId(shapeSidId);
      shape.setTestCase(testCase);
      shape.setShapeName(shapeName);
      shape.setShapeSrs(shapeSrs);
      shape.setShareAreaM2(shareAreaM2);
      shape.setParentShapeId(parentShapeId);
      shape.setShapeType(shapeType);
      return shape;
    }
  }
}


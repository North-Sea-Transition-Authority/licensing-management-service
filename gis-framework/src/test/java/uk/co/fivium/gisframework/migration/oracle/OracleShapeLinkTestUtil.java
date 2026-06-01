package uk.co.fivium.gisframework.migration.oracle;

public class OracleShapeLinkTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer childShapeId = 1;
    private Integer parentShapeId = 2;

    public Builder withChildShapeId(Integer childShapeId) {
      this.childShapeId = childShapeId;
      return this;
    }

    public Builder withParentShapeId(Integer parentShapeId) {
      this.parentShapeId = parentShapeId;
      return this;
    }

    public OracleShapeLink build() {
      var shapeLink = new OracleShapeLink();
      shapeLink.setChildShapeId(childShapeId);
      shapeLink.setParentShapeId(parentShapeId);
      return shapeLink;
    }
  }
}

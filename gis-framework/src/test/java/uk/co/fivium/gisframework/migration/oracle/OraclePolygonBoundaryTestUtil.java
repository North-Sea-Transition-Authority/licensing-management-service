package uk.co.fivium.gisframework.migration.oracle;

public class OraclePolygonBoundaryTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer boundarySidId = 1;
    private Integer oracleShapePolygonId = 1;
    private BoundaryType boundaryType = BoundaryType.E;
    private Integer shapeSiId = 1;

    public Builder withBoundarySidId(Integer boundarySidId) {
      this.boundarySidId = boundarySidId;
      return this;
    }

    public Builder withOracleShapePolygonId(Integer oracleShapePolygonId) {
      this.oracleShapePolygonId = oracleShapePolygonId;
      return this;
    }

    public Builder withBoundaryType(BoundaryType boundaryType) {
      this.boundaryType = boundaryType;
      return this;
    }

    public Builder withShapeSiId(Integer shapeSiId) {
      this.shapeSiId = shapeSiId;
      return this;
    }

    public OraclePolygonBoundary build() {
      var boundary = new OraclePolygonBoundary();
      boundary.setBoundarySidId(boundarySidId);
      boundary.setOracleShapePolygonId(oracleShapePolygonId);
      boundary.setBoundaryType(boundaryType);
      boundary.setShapeSiId(shapeSiId);
      return boundary;
    }
  }
}


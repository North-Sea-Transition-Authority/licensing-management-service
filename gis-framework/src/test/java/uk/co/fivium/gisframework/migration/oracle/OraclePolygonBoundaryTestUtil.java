package uk.co.fivium.gisframework.migration.oracle;

public class OraclePolygonBoundaryTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Long boundarySidId = 1L;
    private Long polygonSidId = 1L;
    private BoundaryType boundaryType = BoundaryType.E;

    private Builder() {
    }

    public Builder withBoundarySidId(Long boundarySidId) {
      this.boundarySidId = boundarySidId;
      return this;
    }

    public Builder withPolygonSidId(Long polygonSidId) {
      this.polygonSidId = polygonSidId;
      return this;
    }

    public Builder withBoundaryType(BoundaryType boundaryType) {
      this.boundaryType = boundaryType;
      return this;
    }

    public OraclePolygonBoundary build() {
      var boundary = new OraclePolygonBoundary();
      boundary.setBoundarySidId(boundarySidId);
      boundary.setPolygonSidId(polygonSidId);
      boundary.setBoundaryType(boundaryType);
      return boundary;
    }
  }
}


package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Table(name = "GIS_ALPHA_POLYGON_BOUNDARIES")
public class OraclePolygonBoundary {
  @Id
  @Column(name = "BOUNDARY_SID_ID")
  private Long boundarySidId;

  @Column(name = "POLYGON_SID_ID")
  private Long polygonSidId;

  @Column(name = "BOUNDARY_TYPE")
  @Enumerated(EnumType.STRING)
  private BoundaryType boundaryType;

  public Long getBoundarySidId() {
    return boundarySidId;
  }

  public void setBoundarySidId(Long boundarySidId) {
    this.boundarySidId = boundarySidId;
  }

  public Long getPolygonSidId() {
    return polygonSidId;
  }

  public void setPolygonSidId(Long polygonSidId) {
    this.polygonSidId = polygonSidId;
  }

  public BoundaryType getBoundaryType() {
    return boundaryType;
  }

  public void setBoundaryType(BoundaryType boundaryType) {
    this.boundaryType = boundaryType;
  }

}
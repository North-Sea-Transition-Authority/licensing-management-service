package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_POLYGON_BOUNDARIES")
public class OraclePolygonBoundary {

  @Id
  @Column(name = "BOUNDARY_SID_ID")
  private Integer boundarySidId;

  @Column(name = "POLYGON_SID_ID")
  private Integer oracleShapePolygonId;

  @Column(name = "SHAPE_SI_ID")
  private Integer shapeSiId;

  @Column(name = "BOUNDARY_TYPE")
  @Enumerated(EnumType.STRING)
  private BoundaryType boundaryType;

  public Integer getBoundarySidId() {
    return boundarySidId;
  }

  void setBoundarySidId(Integer boundarySidId) {
    this.boundarySidId = boundarySidId;
  }

  public Integer getOracleShapePolygonId() {
    return oracleShapePolygonId;
  }

  void setOracleShapePolygonId(Integer oracleShapePolygonId) {
    this.oracleShapePolygonId = oracleShapePolygonId;
  }

  public Integer getShapeSiId() {
    return shapeSiId;
  }

  void setShapeSiId(Integer shapeSiId) {
    this.shapeSiId = shapeSiId;
  }

  public BoundaryType getBoundaryType() {
    return boundaryType;
  }

  void setBoundaryType(BoundaryType boundaryType) {
    this.boundaryType = boundaryType;
  }
}

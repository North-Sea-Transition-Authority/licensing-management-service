package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_BOUNDARY_LINES")
public class OracleBoundaryLine {

  @Id
  @Column(name = "LINE_SID_ID")
  private Integer lineSidId;

  @Column(name = "BOUNDARY_SID_ID")
  private Integer oraclePolygonBoundaryId;

  @Column(name = "SHAPE_SI_ID")
  private Integer shapeSiId;

  @Column(name = "CONNECTION_ORDER")
  private Long connectionOrder;

  @Column(name = "LINE_NAVIGATION_TYPE")
  @Enumerated(EnumType.STRING)
  private LineNavigationType lineNavigationType;

  @Lob
  @Column(name = "LINE_GEOJSON")
  private String lineGeojson;

  public Integer getLineSidId() {
    return lineSidId;
  }

  void setLineSidId(Integer lineSidId) {
    this.lineSidId = lineSidId;
  }

  public Integer getOraclePolygonBoundaryId() {
    return oraclePolygonBoundaryId;
  }

  void setOraclePolygonBoundaryId(Integer oraclePolygonBoundaryId) {
    this.oraclePolygonBoundaryId = oraclePolygonBoundaryId;
  }

  public Integer getShapeSiId() {
    return shapeSiId;
  }

  void setShapeSiId(Integer shapeSiId) {
    this.shapeSiId = shapeSiId;
  }

  public Long getConnectionOrder() {
    return connectionOrder;
  }

  void setConnectionOrder(Long connectionOrder) {
    this.connectionOrder = connectionOrder;
  }

  public LineNavigationType getLineNavigationType() {
    return lineNavigationType;
  }

  void setLineNavigationType(LineNavigationType lineNavigationType) {
    this.lineNavigationType = lineNavigationType;
  }

  public String getLineGeojson() {
    return lineGeojson;
  }

  void setLineGeojson(String lineGeojson) {
    this.lineGeojson = lineGeojson;
  }
}

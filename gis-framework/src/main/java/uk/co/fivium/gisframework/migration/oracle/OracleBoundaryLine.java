package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Profile;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Profile("gis-migration")
@Entity
@Table(name = "GIS_ALPHA_BOUNDARY_LINES")
public class OracleBoundaryLine {

  @Id
  @Column(name = "LINE_SID_ID")
  private Long lineSidId;

  @Column(name = "BOUNDARY_SID_ID")
  private Long boundarySidId;

  @Column(name = "CONNECTION_ORDER")
  private Long connectionOrder;

  @Column(name = "LINE_NAVIGATION_TYPE")
  @Enumerated(EnumType.STRING)
  private LineNavigationType lineNavigationType;

  @Lob
  @Column(name = "LINE_GEOJSON")
  private String lineGeojson;

  public Long getLineSidId() {
    return lineSidId;
  }

  public void setLineSidId(Long lineSidId) {
    this.lineSidId = lineSidId;
  }

  public Long getBoundarySidId() {
    return boundarySidId;
  }

  public void setBoundarySidId(Long boundarySidId) {
    this.boundarySidId = boundarySidId;
  }

  public Long getConnectionOrder() {
    return connectionOrder;
  }

  public void setConnectionOrder(Long connectionOrder) {
    this.connectionOrder = connectionOrder;
  }

  public LineNavigationType getLineNavigationType() {
    return lineNavigationType;
  }

  public void setLineNavigationType(LineNavigationType lineNavigationType) {
    this.lineNavigationType = lineNavigationType;
  }

  public String getLineGeojson() {
    return lineGeojson;
  }

  public void setLineGeojson(String lineGeojson) {
    this.lineGeojson = lineGeojson;
  }

}
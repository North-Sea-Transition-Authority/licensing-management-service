package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Table(name = "GIS_ALPHA_SHAPE_POLYGONS")
public class OracleShapePolygon {

  @Id
  @Column(name = "POLYGON_SID_ID")
  private Integer polygonSidId;

  @Column(name = "SHAPE_SID_ID")
  private Integer shapeSidId;

  @Column(name = "FEATURE_OFFSET_LOW_M")
  private Long featureOffsetLowM;

  @Column(name = "FEATURE_OFFSET_HIGH_M")
  private Long featureOffsetHighM;

  public Integer getPolygonSidId() {
    return polygonSidId;
  }

  public void setPolygonSidId(Integer polygonSidId) {
    this.polygonSidId = polygonSidId;
  }

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  public void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public Long getFeatureOffsetLowM() {
    return featureOffsetLowM;
  }

  public void setFeatureOffsetLowM(Long featureOffsetLowM) {
    this.featureOffsetLowM = featureOffsetLowM;
  }

  public Long getFeatureOffsetHighM() {
    return featureOffsetHighM;
  }

  public void setFeatureOffsetHighM(Long featureOffsetHighM) {
    this.featureOffsetHighM = featureOffsetHighM;
  }
}
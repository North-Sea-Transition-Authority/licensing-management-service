package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_SHAPE_POLYGONS")
public class OracleShapePolygon {

  @Id
  @Column(name = "POLYGON_SID_ID")
  private Integer polygonSidId;

  @Column(name = "SHAPE_SID_ID")
  private Integer shapeSidId;

  @Column(name = "SHAPE_SI_ID")
  private Integer oracleShapeId;

  @Column(name = "FEATURE_OFFSET_LOW_M")
  private Long featureOffsetLowM;

  @Column(name = "FEATURE_OFFSET_HIGH_M")
  private Long featureOffsetHighM;

  public Integer getPolygonSidId() {
    return polygonSidId;
  }

  void setPolygonSidId(Integer polygonSidId) {
    this.polygonSidId = polygonSidId;
  }

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public Integer getOracleShapeId() {
    return oracleShapeId;
  }

  void setOracleShapeId(Integer oracleShapeId) {
    this.oracleShapeId = oracleShapeId;
  }

  public Long getFeatureOffsetLowM() {
    return featureOffsetLowM;
  }

  void setFeatureOffsetLowM(Long featureOffsetLowM) {
    this.featureOffsetLowM = featureOffsetLowM;
  }

  public Long getFeatureOffsetHighM() {
    return featureOffsetHighM;
  }

  void setFeatureOffsetHighM(Long featureOffsetHighM) {
    this.featureOffsetHighM = featureOffsetHighM;
  }
}

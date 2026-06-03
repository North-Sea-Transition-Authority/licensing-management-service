package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_SHAPES")
public class OracleShape {

  @Id
  @Column(name = "SHAPE_SI_ID")
  private Integer shapeSiId;

  @Column(name = "SHAPE_SID_ID")
  private Integer shapeSidId;

  @ManyToOne
  @JoinColumn(name = "LAYER_ID")
  private OracleLayer oracleLayer;

  @Column(name = "SHAPE_NAME")
  private String shapeName;

  @Column(name = "SHAPE_SRS")
  private String shapeSrs;

  @Column(name = "SHAPE_AREA_M2")
  private Double shareAreaM2;

  @Column(name = "SHAPE_START_DATE")
  private LocalDate shapeStartDate;

  @Column(name = "SHAPE_END_DATE")
  private LocalDate shapeEndDate;

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public Integer getShapeSiId() {
    return shapeSiId;
  }

  void setShapeSiId(Integer shapeSiId) {
    this.shapeSiId = shapeSiId;
  }

  public OracleLayer getOracleLayer() {
    return oracleLayer;
  }

  void setOracleLayer(OracleLayer oracleLayer) {
    this.oracleLayer = oracleLayer;
  }

  public String getShapeName() {
    return shapeName;
  }

  void setShapeName(String shapeName) {
    this.shapeName = shapeName;
  }

  public String getShapeSrs() {
    return shapeSrs;
  }

  void setShapeSrs(String shapeSrs) {
    this.shapeSrs = shapeSrs;
  }

  public Double getShareAreaM2() {
    return shareAreaM2;
  }

  void setShareAreaM2(Double shareAreaM2) {
    this.shareAreaM2 = shareAreaM2;
  }

  public LocalDate getShapeStartDate() {
    return shapeStartDate;
  }

  void setShapeStartDate(LocalDate shapeStartDate) {
    this.shapeStartDate = shapeStartDate;
  }

  public LocalDate getShapeEndDate() {
    return shapeEndDate;
  }

  void setShapeEndDate(LocalDate shapeEndDate) {
    this.shapeEndDate = shapeEndDate;
  }
}

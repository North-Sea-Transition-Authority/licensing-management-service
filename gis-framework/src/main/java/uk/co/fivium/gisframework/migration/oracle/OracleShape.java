package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@IdClass(OracleShapeCompositeKey.class)
@Table(name = "gis_alpha_shapes")
public class OracleShape {

  @Id
  @Column(name = "shape_sid_id")
  private Integer shapeSidId;

  @Column(name = "shape_name")
  private String shapeName;

  @Column(name = "shape_srs")
  private String shapeSrs;

  @Column(name = "shape_area_m2")
  private Double shareAreaM2;

  @Column(name = "parent_shape_id")
  private Integer parentShapeId;

  @Id
  @Column(name = "test_case")
  private String testCase;

  @Column(name = "shape_type")
  @Enumerated(EnumType.STRING)
  private ShapeType shapeType;

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  public void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public String getShapeName() {
    return shapeName;
  }

  public void setShapeName(String shapeName) {
    this.shapeName = shapeName;
  }

  public String getShapeSrs() {
    return shapeSrs;
  }

  public void setShapeSrs(String shapeSrs) {
    this.shapeSrs = shapeSrs;
  }

  public Double getShareAreaM2() {
    return shareAreaM2;
  }

  public void setShareAreaM2(Double shareAreaM2) {
    this.shareAreaM2 = shareAreaM2;
  }

  public Integer getParentShapeId() {
    return parentShapeId;
  }

  public void setParentShapeId(Integer parentShapeId) {
    this.parentShapeId = parentShapeId;
  }

  public String getTestCase() {
    return testCase;
  }

  public void setTestCase(String testCase) {
    this.testCase = testCase;
  }

  public ShapeType getShapeType() {
    return shapeType;
  }

  public void setShapeType(ShapeType shapeType) {
    this.shapeType = shapeType;
  }
}

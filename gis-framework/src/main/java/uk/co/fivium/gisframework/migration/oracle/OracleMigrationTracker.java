package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Date;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_TRACKER")
public class OracleMigrationTracker {

  @Id
  @Column(name = "MIGRATION_SHAPE_SI_ID")
  private Integer shapeSiId;

  @Column(name = "MIGRATION_SHAPE_NAME")
  private String shapeName;

  @Column(name = "MIGRATION_SHAPE_START_DATE")
  private Date shapeStartDate;

  @Column(name = "MIGRATION_SHAPE_END_DATE")
  private Date shapeEndDate;

  @Column(name = "MIGRATION_LAYER_ID")
  private Integer layerId;

  @Column(name = "MIGRATION_ORDER")
  private Integer orderNumber;

  @Column(name = "MIGRATION_START_DATETIME")
  private LocalDateTime migrationStartDateTime;

  @Column(name = "MIGRATION_END_DATETIME")
  private LocalDateTime migrationEndDateTime;

  @Column(name = "MIGRATED_FLAG")
  private String flag;

  @Column(name = "ERROR_MESSAGE")
  private String errorMessage;

  public Integer getShapeSiId() {
    return shapeSiId;
  }

  void setShapeSiId(Integer shapeSiId) {
    this.shapeSiId = shapeSiId;
  }

  public String getShapeName() {
    return shapeName;
  }

  void setShapeName(String shapeName) {
    this.shapeName = shapeName;
  }

  public Date getShapeStartDate() {
    return shapeStartDate;
  }

  void setShapeStartDate(Date shapeStartDate) {
    this.shapeStartDate = shapeStartDate;
  }

  public Date getShapeEndDate() {
    return shapeEndDate;
  }

  void setShapeEndDate(Date shapeEndDate) {
    this.shapeEndDate = shapeEndDate;
  }

  public Integer getLayerId() {
    return layerId;
  }

  void setLayerId(Integer layerId) {
    this.layerId = layerId;
  }

  public Integer getOrderNumber() {
    return orderNumber;
  }

  void setOrderNumber(Integer orderNumber) {
    this.orderNumber = orderNumber;
  }

  public LocalDateTime getMigrationStartDateTime() {
    return migrationStartDateTime;
  }

  public void setMigrationStartDateTime(LocalDateTime migrationStartDateTime) {
    this.migrationStartDateTime = migrationStartDateTime;
  }

  public LocalDateTime getMigrationEndDateTime() {
    return migrationEndDateTime;
  }

  public void setMigrationEndDateTime(LocalDateTime migrationEndDateTime) {
    this.migrationEndDateTime = migrationEndDateTime;
  }

  public String getFlag() {
    return flag;
  }

  void setFlag(String flag) {
    this.flag = flag;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}

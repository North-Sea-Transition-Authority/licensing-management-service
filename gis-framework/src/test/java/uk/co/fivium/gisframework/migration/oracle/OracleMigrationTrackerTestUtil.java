package uk.co.fivium.gisframework.migration.oracle;

import java.time.LocalDateTime;
import java.util.Date;

public class OracleMigrationTrackerTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer shapeSiId = 1;
    private String shapeName = "Test Shape";
    private Date shapeStartDate = new Date(0);
    private Date shapeEndDate = new Date(1);
    private Integer layerId = 1;
    private Integer orderNumber = 1;
    private LocalDateTime migrationStartDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
    private LocalDateTime migrationEndDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
    private String flag = "N";
    private String errorMessage = "Test error message";

    public Builder withShapeSiId(Integer shapeSiId) {
      this.shapeSiId = shapeSiId;
      return this;
    }

    public Builder withShapeName(String shapeName) {
      this.shapeName = shapeName;
      return this;
    }

    public Builder withShapeStartDate(Date shapeStartDate) {
      this.shapeStartDate = shapeStartDate;
      return this;
    }

    public Builder withShapeEndDate(Date shapeEndDate) {
      this.shapeEndDate = shapeEndDate;
      return this;
    }

    public Builder withLayerId(Integer layerId) {
      this.layerId = layerId;
      return this;
    }

    public Builder withOrderNumber(Integer orderNumber) {
      this.orderNumber = orderNumber;
      return this;
    }

    public Builder withMigrationStartDateTime(LocalDateTime migrationStartDateTime) {
      this.migrationStartDateTime = migrationStartDateTime;
      return this;
    }

    public Builder withMigrationEndDateTime(LocalDateTime migrationEndDateTime) {
      this.migrationEndDateTime = migrationEndDateTime;
      return this;
    }

    public Builder withFlag(String flag) {
      this.flag = flag;
      return this;
    }

    public Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public OracleMigrationTracker build() {
      var tracker = new OracleMigrationTracker();
      tracker.setShapeSiId(shapeSiId);
      tracker.setShapeName(shapeName);
      tracker.setShapeStartDate(shapeStartDate);
      tracker.setShapeEndDate(shapeEndDate);
      tracker.setLayerId(layerId);
      tracker.setOrderNumber(orderNumber);
      tracker.setMigrationStartDateTime(migrationStartDateTime);
      tracker.setMigrationEndDateTime(migrationEndDateTime);
      tracker.setFlag(flag);
      tracker.setErrorMessage(errorMessage);
      return tracker;
    }
  }
}

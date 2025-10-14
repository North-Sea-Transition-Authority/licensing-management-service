package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cs_start_date_migration_extract")
@MigrationEntity
public class CarbonStorageStartDateMigrationExtract {

  @Id
  private String licenceRef;

  private String startDate;

  public String getLicenceRef() {
    return licenceRef;
  }

  public void setLicenceRef(String licenceRef) {
    this.licenceRef = licenceRef;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }
}

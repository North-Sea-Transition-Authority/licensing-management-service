package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cs_licence_migration_extract")
@MigrationEntity
public class CarbonStorageLicenceMigrationExtract {

  @Id
  private String licenceRef;

  private String licenceNumber;

  private String responsibleOrgs;

  private String status;

  private String statusDate;

  public String getLicenceRef() {
    return licenceRef;
  }

  public void setLicenceRef(String licenceRef) {
    this.licenceRef = licenceRef;
  }

  public String getLicenceNumber() {
    return licenceNumber;
  }

  public void setLicenceNumber(String licenceNumber) {
    this.licenceNumber = licenceNumber;
  }

  public String getResponsibleOrgs() {
    return responsibleOrgs;
  }

  public void setResponsibleOrgs(String responsibleOrgs) {
    this.responsibleOrgs = responsibleOrgs;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatusDate() {
    return statusDate;
  }

  public void setStatusDate(String statusDate) {
    this.statusDate = statusDate;
  }
}

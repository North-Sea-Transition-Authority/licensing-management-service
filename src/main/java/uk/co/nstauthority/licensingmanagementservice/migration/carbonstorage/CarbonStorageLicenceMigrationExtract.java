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

  private String prefix;

  private String licenceNumber;

  private String responsibleOrgs;

  public String getLicenceRef() {
    return licenceRef;
  }

  public void setLicenceRef(String licenceRef) {
    this.licenceRef = licenceRef;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
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
}

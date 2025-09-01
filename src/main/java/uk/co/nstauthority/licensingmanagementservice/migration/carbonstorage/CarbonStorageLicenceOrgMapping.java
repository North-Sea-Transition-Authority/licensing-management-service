package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cs_licence_org_mapping")
@MigrationEntity
public class CarbonStorageLicenceOrgMapping {

  @Id
  private String csExtractResponsibleOrganisation;

  private Integer organisationUnitId;

  private Integer organisationGroupId;

  private String organisationGroupName;

  public String getCsExtractResponsibleOrganisation() {
    return csExtractResponsibleOrganisation;
  }

  public Integer getOrganisationUnitId() {
    return organisationUnitId;
  }

  public Integer getOrganisationGroupId() {
    return organisationGroupId;
  }

  public String getOrganisationGroupName() {
    return organisationGroupName;
  }

  public void setCsExtractResponsibleOrganisation(String csExtractResponsibleOrganisation) {
    this.csExtractResponsibleOrganisation = csExtractResponsibleOrganisation;
  }

  public void setOrganisationUnitId(Integer organisationUnitId) {
    this.organisationUnitId = organisationUnitId;
  }

  public void setOrganisationGroupId(Integer organisationGroupId) {
    this.organisationGroupId = organisationGroupId;
  }

  public void setOrganisationGroupName(String organisationGroupName) {
    this.organisationGroupName = organisationGroupName;
  }
}

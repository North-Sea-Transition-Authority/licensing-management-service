package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

@Entity
@IdClass(PearsContactsMigrationExtractCompositeKey.class)
@Table(name = "pears_contacts_migration_extract")
@MigrationEntity
public class PearsContactsMigrationExtract {

  @Id
  private Integer organisationGroupId;

  @Id
  private Integer wuaId;

  public PearsContactsMigrationExtract() {
  }

  public PearsContactsMigrationExtract(Integer organisationGroupId, Integer wuaId) {
    this.organisationGroupId = organisationGroupId;
    this.wuaId = wuaId;
  }

  public Integer getOrganisationGroupId() {
    return organisationGroupId;
  }

  public void setOrganisationGroupId(Integer organisationGroupId) {
    this.organisationGroupId = organisationGroupId;
  }

  public Integer getWuaId() {
    return wuaId;
  }

  public void setWuaId(Integer wuaId) {
    this.wuaId = wuaId;
  }
}

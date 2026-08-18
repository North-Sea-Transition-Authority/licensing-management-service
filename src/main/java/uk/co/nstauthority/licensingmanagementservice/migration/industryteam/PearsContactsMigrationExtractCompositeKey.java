package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class PearsContactsMigrationExtractCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 5416862301518963457L;

  private Integer organisationGroupId;

  private Integer wuaId;

  public PearsContactsMigrationExtractCompositeKey() {
    // Required by JPA for @IdClass
  }

  public PearsContactsMigrationExtractCompositeKey(Integer organisationGroupId, Integer wuaId) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PearsContactsMigrationExtractCompositeKey that = (PearsContactsMigrationExtractCompositeKey) o;
    return Objects.equals(organisationGroupId, that.organisationGroupId) && Objects.equals(wuaId, that.wuaId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organisationGroupId, wuaId);
  }
}

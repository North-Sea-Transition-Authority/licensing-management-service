package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import java.io.Serializable;
import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

public class LicenceOrganisationId implements Serializable {

  Licence licence;

  Integer responsibleOrganisationId;

  public LicenceOrganisationId() {
  }

  public LicenceOrganisationId(Licence licence, Integer responsibleOrganisationId) {
    this.licence = licence;
    this.responsibleOrganisationId = responsibleOrganisationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LicenceOrganisationId that = (LicenceOrganisationId) o;
    Integer licenceId = licence != null ? licence.getId() : null;
    Integer thatLicenceId = that.licence != null ? that.licence.getId() : null;
    return Objects.equals(licenceId, thatLicenceId)
        && Objects.equals(responsibleOrganisationId, that.responsibleOrganisationId);
  }

  @Override
  public int hashCode() {
    Integer licenceId = licence != null ? licence.getId() : null;
    return Objects.hash(licenceId, responsibleOrganisationId);
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import java.io.Serializable;
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
}

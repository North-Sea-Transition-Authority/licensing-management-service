package uk.co.nstauthority.licensingmanagementservice.licence;

import java.io.Serializable;

public class LicenceOrganisationId implements Serializable {

  Licence licence;

  Integer responsibleOrganisationId;

  public LicenceOrganisationId(Licence licence, Integer responsibleOrganisationId) {
    this.licence = licence;
    this.responsibleOrganisationId = responsibleOrganisationId;
  }
}

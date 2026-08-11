package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.Instant;
import java.util.UUID;

public interface LicenceApplicationDetail {
  UUID getId();

  LicenceApplication getLicenceApplication();

  Licence getLicence();

  Integer getResponsibleOrganisationUnitId();

  Instant getSubmittedDatetime();
}
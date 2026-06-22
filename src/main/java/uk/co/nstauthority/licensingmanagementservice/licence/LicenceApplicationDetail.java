package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.Instant;
import java.util.UUID;

public interface LicenceApplicationDetail {
  UUID getId();

  LicenceApplication getLicenceApplication();

  Integer getResponsibleOrganisationUnitId();

  Instant getSubmittedDatetime();
}
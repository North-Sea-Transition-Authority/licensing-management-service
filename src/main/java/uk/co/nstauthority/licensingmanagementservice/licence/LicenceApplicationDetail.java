package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;

public interface LicenceApplicationDetail {
  UUID getId();

  LicenceApplication getLicenceApplication();

  Licence getLicence();

  Integer getVersionNumber();

  ApplicationStatus getStatus();

  Integer getResponsibleOrganisationUnitId();

  Instant getSubmittedDatetime();

  Long getCreatedByWuaId();

  Long getSubmittedByWuaId();

  LocalDate getUpdateDeadlineDate();

  String getUpdateReason();

  /** True once submitted, and stays true if later withdrawn or closed. */
  default boolean hasBeenSubmitted() {
    return getSubmittedDatetime() != null;
  }
}
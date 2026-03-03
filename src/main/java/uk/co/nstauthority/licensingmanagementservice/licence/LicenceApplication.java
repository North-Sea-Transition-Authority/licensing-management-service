package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

public interface LicenceApplication {
  UUID getId();

  ApplicationType getApplicationType();
}
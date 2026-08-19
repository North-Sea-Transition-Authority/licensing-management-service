package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public final class LicenceCorrectionRoles {

  private LicenceCorrectionRoles() {
  }

  public static Optional<Role> getRequiredRoleForLicenceType(LicenceType licenceType) {
    if (LicenceType.CARBON_STORAGE == licenceType) {
      return Optional.of(Role.CARBON_STORAGE_LICENCE_CORRECTOR);
    }
    if (licenceType != null && licenceType.isProduction()) {
      return Optional.of(Role.PRODUCTION_LICENCE_CORRECTOR);
    }
    return Optional.empty();
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public final class CaseManagerRoles {

  public static final Set<Role> ROLES = Collections.unmodifiableSet(EnumSet.of(
      Role.CASE_MANAGER_OFFSHORE,
      Role.CASE_MANAGER_CARBON_STORAGE,
      Role.CASE_MANAGER_ONSHORE
  ));

  private CaseManagerRoles() {
  }

  public static Optional<Role> getRequiredRoleForLicenceType(LicenceType licenceType) {
    if (licenceType == LicenceType.CARBON_STORAGE) {
      return Optional.of(Role.CASE_MANAGER_CARBON_STORAGE);
    }
    if (licenceType == LicenceType.SEAWARD_PRODUCTION) {
      return Optional.of(Role.CASE_MANAGER_OFFSHORE);
    }
    if (licenceType == LicenceType.LANDWARD_PRODUCTION) {
      return Optional.of(Role.CASE_MANAGER_ONSHORE);
    }
    return Optional.empty();
  }
}

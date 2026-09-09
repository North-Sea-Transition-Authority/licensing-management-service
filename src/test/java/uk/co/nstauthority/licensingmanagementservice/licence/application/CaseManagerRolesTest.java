package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

class CaseManagerRolesTest {

  @Test
  void getRequiredRoleForLicenceType_whenOffshoreProduction_thenCaseManagerOffshore() {
    assertThat(CaseManagerRoles.getRequiredRoleForLicenceType(LicenceType.SEAWARD_PRODUCTION))
        .contains(Role.CASE_MANAGER_OFFSHORE);
  }

  @Test
  void getRequiredRoleForLicenceType_whenOnshoreProduction_thenCaseManagerOnshore() {
    assertThat(CaseManagerRoles.getRequiredRoleForLicenceType(LicenceType.LANDWARD_PRODUCTION))
        .contains(Role.CASE_MANAGER_ONSHORE);
  }

  @Test
  void getRequiredRoleForLicenceType_whenCarbonStorage_thenCaseManagerCarbonStorage() {
    assertThat(CaseManagerRoles.getRequiredRoleForLicenceType(LicenceType.CARBON_STORAGE))
        .contains(Role.CASE_MANAGER_CARBON_STORAGE);
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION", "CARBON_STORAGE"}
  )
  void getRequiredRoleForLicenceType_whenNoCaseManagerRole_thenEmpty(LicenceType licenceType) {
    assertThat(CaseManagerRoles.getRequiredRoleForLicenceType(licenceType)).isEmpty();
  }

  @Test
  void getRequiredRoleForLicenceType_whenNullLicenceType_thenEmpty() {
    assertThat(CaseManagerRoles.getRequiredRoleForLicenceType(null)).isEmpty();
  }
}

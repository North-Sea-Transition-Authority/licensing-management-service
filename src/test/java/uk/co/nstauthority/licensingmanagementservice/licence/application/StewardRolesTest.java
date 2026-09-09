package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

class StewardRolesTest {

  @Test
  void getRequiredRoleForLicenceType_whenOffshoreProduction_thenStewardOffshore() {
    assertThat(StewardRoles.getRequiredRoleForLicenceType(LicenceType.SEAWARD_PRODUCTION))
        .contains(Role.STEWARD_OFFSHORE);
  }

  @Test
  void getRequiredRoleForLicenceType_whenOnshoreProduction_thenStewardOnshore() {
    assertThat(StewardRoles.getRequiredRoleForLicenceType(LicenceType.LANDWARD_PRODUCTION))
        .contains(Role.STEWARD_ONSHORE);
  }

  @Test
  void getRequiredRoleForLicenceType_whenCarbonStorage_thenStewardCarbonStorage() {
    assertThat(StewardRoles.getRequiredRoleForLicenceType(LicenceType.CARBON_STORAGE))
        .contains(Role.STEWARD_CARBON_STORAGE);
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION", "CARBON_STORAGE"}
  )
  void getRequiredRoleForLicenceType_whenNoStewardRole_thenEmpty(LicenceType licenceType) {
    assertThat(StewardRoles.getRequiredRoleForLicenceType(licenceType)).isEmpty();
  }

  @Test
  void getRequiredRoleForLicenceType_whenNullLicenceType_thenEmpty() {
    assertThat(StewardRoles.getRequiredRoleForLicenceType(null)).isEmpty();
  }
}

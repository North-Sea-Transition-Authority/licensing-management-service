package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

class LicenceCorrectionRolesTest {

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"})
  void getRequiredRoleForLicenceType_whenProductionLicence_thenProductionCorrector(LicenceType licenceType) {
    assertThat(LicenceCorrectionRoles.getRequiredRoleForLicenceType(licenceType))
        .contains(Role.PRODUCTION_LICENCE_CORRECTOR);
  }

  @Test
  void getRequiredRoleForLicenceType_whenCarbonStorageLicence_thenCarbonStorageCorrector() {
    assertThat(LicenceCorrectionRoles.getRequiredRoleForLicenceType(LicenceType.CARBON_STORAGE))
        .contains(Role.CARBON_STORAGE_LICENCE_CORRECTOR);
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {
          "LANDWARD_PRODUCTION",
          "SEAWARD_PRODUCTION",
          "CARBON_STORAGE"
      }
  )
  void getRequiredRoleForLicenceType_whenNonCorrectableLicence_thenEmpty(LicenceType licenceType) {
    assertThat(LicenceCorrectionRoles.getRequiredRoleForLicenceType(licenceType)).isEmpty();
  }

  @Test
  void getRequiredRoleForLicenceType_whenNullLicenceType_thenEmpty() {
    assertThat(LicenceCorrectionRoles.getRequiredRoleForLicenceType(null)).isEmpty();
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest
  @MethodSource("correctorRoleForLicenceTypeArguments")
  void hasCorrectorRole_whenUserHasTheCorrectorRoleForTheLicenceType_thenTrue(LicenceType licenceType, Role role) {
    assertThat(LicenceCorrectionRoles.hasCorrectorRole(licenceType, Set.of(role))).isTrue();
  }

  @ParameterizedTest
  @MethodSource("correctorRoleForOtherLicenceTypeArguments")
  void hasCorrectorRole_whenUserOnlyHasTheCorrectorRoleForAnotherLicenceType_thenFalse(
      LicenceType licenceType,
      Role role
  ) {
    assertThat(LicenceCorrectionRoles.hasCorrectorRole(licenceType, Set.of(role))).isFalse();
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
  void hasCorrectorRole_whenLicenceTypeHasNoCorrectorRole_thenFalse(LicenceType licenceType) {
    var allCorrectorRoles = Set.of(Role.PRODUCTION_LICENCE_CORRECTOR, Role.CARBON_STORAGE_LICENCE_CORRECTOR);

    assertThat(LicenceCorrectionRoles.hasCorrectorRole(licenceType, allCorrectorRoles)).isFalse();
  }

  private static Stream<Arguments> correctorRoleForLicenceTypeArguments() {
    return Stream.of(
        Arguments.of(LicenceType.LANDWARD_PRODUCTION, Role.PRODUCTION_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.SEAWARD_PRODUCTION, Role.PRODUCTION_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.CARBON_STORAGE, Role.CARBON_STORAGE_LICENCE_CORRECTOR)
    );
  }

  private static Stream<Arguments> correctorRoleForOtherLicenceTypeArguments() {
    return Stream.of(
        Arguments.of(LicenceType.LANDWARD_PRODUCTION, Role.CARBON_STORAGE_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.SEAWARD_PRODUCTION, Role.CARBON_STORAGE_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.CARBON_STORAGE, Role.PRODUCTION_LICENCE_CORRECTOR)
    );
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

class LicenceTypeTest {

  @ParameterizedTest
  @ValueSource(strings = {"AL", "CE", "DL", "EXL", "ML", "PEDL", "PL"})
  void getFromPrefix_whenLandwardProductionPrefix_thenLandwardProduction(String prefix) {
    var result = LicenceType.getFromPrefix(prefix);

    assertThat(result).isEqualTo(LicenceType.LANDWARD_PRODUCTION);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"})
  void getReleaseFeature_whenAvailableLicenceType_thenLicenceType(LicenceType licenceType) {
    var result = licenceType.getReleaseFeature();

    assertThat(result).isEqualTo(ReleaseFeature.CURRENT_LICENCE_TYPE);
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      names = {"GAS_STORAGE", "LANDWARD_EXPLORATION", "METHANE_DRAINAGE", "SEAWARD_EXPLORATION"}
  )
  void getReleaseFeature_whenFutureLicenceType_thenFutureLicenceType(LicenceType licenceType) {
    var result = licenceType.getReleaseFeature();

    assertThat(result).isEqualTo(ReleaseFeature.FUTURE_LICENCE_TYPE);
  }
}

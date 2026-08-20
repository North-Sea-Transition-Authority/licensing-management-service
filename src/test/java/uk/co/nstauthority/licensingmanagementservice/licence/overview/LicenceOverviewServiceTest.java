package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

class LicenceOverviewServiceTest {

  private final LicenceOverviewService licenceOverviewService = new LicenceOverviewService();

  @Test
  void getLicenceOverviewView_whenCarbonStorageLicence_assertHeaderIncludesRegisterLink() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();

    var view = licenceOverviewService.getLicenceOverviewView(licence);

    assertThat(view).isEqualTo(new LicenceOverviewView(
        "CS001",
        LicenceType.CARBON_STORAGE.getDisplayName(),
        "https://www.nstauthority.co.uk/regulatory-information/carbon-storage/carbon-storage-public-register/?section=CS001"
    ));
  }

  @Test
  void getLicenceOverviewView_whenNotCarbonStorageLicence_assertNoRegisterLink() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.GAS_STORAGE)
        .withLicenceReference("GS001")
        .build();

    var view = licenceOverviewService.getLicenceOverviewView(licence);

    assertThat(view).isEqualTo(new LicenceOverviewView(
        "GS001",
        LicenceType.GAS_STORAGE.getDisplayName(),
        null
    ));
  }
}

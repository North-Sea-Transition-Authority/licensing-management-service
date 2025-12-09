package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LicenceTypeGroupTest {
  @Test
  void getUrlSlugList_MultipleLicenceTypes_ReturnsCommaSeparatedList() {
    var result = LicenceTypeGroup.PRODUCTION.getUrlSlugList();
    assertThat(result).isEqualTo(
        "%s,%s".formatted(LicenceType.LANDWARD_PRODUCTION.getUrlSlug(), LicenceType.SEAWARD_PRODUCTION.getUrlSlug()));
  }

  @Test
  void getUrlSlugList_SingleLicenceType_ReturnsSingleSlug() {
    var result = LicenceTypeGroup.CARBON_STORAGE.getUrlSlugList();
    assertThat(result).isEqualTo(LicenceType.CARBON_STORAGE.getUrlSlug());
  }
}
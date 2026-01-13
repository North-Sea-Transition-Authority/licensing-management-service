package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LicenceTypeUtilTest {
  @Test
  void getUrlSlugList_MultipleLicenceTypes_ReturnsCommaSeparatedList() {
    var result = LicenceTypeUtil.getUrlSlugList(List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION));
    assertThat(result).isEqualTo(
        "%s,%s".formatted(LicenceType.LANDWARD_PRODUCTION.getUrlSlug(), LicenceType.SEAWARD_PRODUCTION.getUrlSlug()));
  }

  @Test
  void getUrlSlugList_SingleLicenceType_ReturnsSingleSlug() {
    var result = LicenceTypeUtil.getUrlSlugList(List.of(LicenceType.CARBON_STORAGE));
    assertThat(result).isEqualTo(LicenceType.CARBON_STORAGE.getUrlSlug());
  }
}
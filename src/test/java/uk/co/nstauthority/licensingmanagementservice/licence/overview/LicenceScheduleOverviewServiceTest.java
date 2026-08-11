package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;

class LicenceScheduleOverviewServiceTest {

  private final LicenceScheduleOverviewService licenceScheduleOverviewService = new LicenceScheduleOverviewService();

  @Test
  void getScheduleHistoryForm_whenLicenceScheduleDetail_thenPreFilled() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var form = licenceScheduleOverviewService.getScheduleHistoryForm(licenceScheduleDetail);

    assertThat(form.getLicenceScheduleDetailId()).isEqualTo(licenceScheduleDetail.getId().toString());
  }

  @Test
  void getCsRegisterlink_whenCarbonStorageLicence_thenReturnsRegisterLink() {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS001");

    var result = licenceScheduleOverviewService.getCsRegisterlink(licence);

    assertThat(result)
        .isEqualTo("https://www.nstauthority.co.uk/regulatory-information/carbon-storage/carbon-storage-public-register/?section=CS001");
  }

  @Test
  void getCsRegisterlink_whenNotCarbonStorageLicence_thenReturnsEmptyString() {
    var licence = new Licence();
    licence.setType(LicenceType.GAS_STORAGE);
    licence.setLicenceReference("GS001");

    var result = licenceScheduleOverviewService.getCsRegisterlink(licence);

    assertThat(result).isEmpty();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;

class LicenceScheduleHistoryFormServiceTest {

  private final LicenceScheduleHistoryFormService licenceScheduleHistoryFormService = new LicenceScheduleHistoryFormService();

  @Test
  void getScheduleHistoryForm_whenLicenceScheduleDetail_thenPreFilled() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var form = licenceScheduleHistoryFormService.getScheduleHistoryForm(licenceScheduleDetail);

    assertThat(form.getLicenceScheduleDetailId()).isEqualTo(licenceScheduleDetail.getId().toString());
  }
}

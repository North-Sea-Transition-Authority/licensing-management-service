package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationScheduleDetailRequestCacheTest {

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @InjectMocks
  private LicenceContinuationScheduleDetailRequestCache licenceContinuationScheduleDetailRequestCache;

  @Test
  void getActiveScheduleDetail() {
    var licence = LicenceTestUtil.builder().build();
    var activeDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence));
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(activeDetail);

    var result = licenceContinuationScheduleDetailRequestCache.getActiveScheduleDetail(licence);

    assertThat(result).isEqualTo(activeDetail);
  }
}

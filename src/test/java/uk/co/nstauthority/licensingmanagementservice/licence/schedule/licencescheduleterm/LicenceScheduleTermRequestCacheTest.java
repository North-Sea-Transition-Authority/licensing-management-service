package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermRequestCacheTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceScheduleTermRequestCache licenceScheduleTermRequestCache;

  @Test
  void getTermsByLicenceScheduleDetail() {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(LicenceTestUtil.builder().build());
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var terms = List.of(LicenceScheduleTermTestUtil.builder().build());
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(terms);

    var result = licenceScheduleTermRequestCache.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    assertThat(result).isEqualTo(terms);
  }
}

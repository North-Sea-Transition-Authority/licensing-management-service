package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermServiceTest {

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @InjectMocks
  private LicenceScheduleTermService licenceScheduleTermService;

  @Test
  void getTermsByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleTermRepository).findByLicenceScheduleDetail(licenceScheduleDetail);
  }
}
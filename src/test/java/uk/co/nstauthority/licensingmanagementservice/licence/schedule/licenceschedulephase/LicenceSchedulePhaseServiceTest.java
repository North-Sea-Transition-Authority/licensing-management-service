package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseServiceTest {

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @InjectMocks
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Test
  void getPhasesByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceSchedulePhaseRepository).findByLicenceScheduleDetail(licenceScheduleDetail);
  }
}
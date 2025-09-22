package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionServiceTest {

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @InjectMocks
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Test
  void getPhasesByLicenceScheduleDetail() {
    var  scheduleWorkProgrammeApplicationDetail= new ScheduleWorkProgrammeApplicationDetail();
    licenceScheduleExtensionService.getExtensionRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);
    verify(licenceScheduleExtensionRepository).findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
  }
}
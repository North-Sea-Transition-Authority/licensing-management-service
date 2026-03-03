package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void getApplication_whenContinuationAndFound_returnsApplication() {
    UUID applicationId = UUID.randomUUID();
    LicenceContinuationApplication licenceContinuationApplication = new LicenceContinuationApplication();
    when(licenceContinuationService.getApplicationByIdOrThrow(applicationId)).thenReturn(licenceContinuationApplication);

    var result = applicationService.getApplication(ApplicationType.CONTINUATION_APPLICATION, applicationId);

    assertThat(result).isEqualTo(licenceContinuationApplication);
    verify(licenceContinuationService).getApplicationByIdOrThrow(applicationId);
  }

  @Test
  void getApplication_whenContinuationAndNotFound_throwsException() {
    UUID applicationId = UUID.randomUUID();

    when(licenceContinuationService.getApplicationByIdOrThrow(applicationId))
        .thenThrow(new LmsEntityNotFoundException("licence continuation application", applicationId));

    assertThatThrownBy(() -> applicationService.getApplication(ApplicationType.CONTINUATION_APPLICATION, applicationId))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessageContaining("licence continuation application");
  }

  @Test
  void getApplication_whenAmendmentAndFound_returnsApplication() {
    UUID applicationId = UUID.randomUUID();
    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    when(scheduleWorkProgrammeApplicationService.getApplicationByIdOrThrow(applicationId)).thenReturn(scheduleWorkProgrammeApplication);

    var result = applicationService.getApplication(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, applicationId);

    assertThat(result).isEqualTo(scheduleWorkProgrammeApplication);
    verify(scheduleWorkProgrammeApplicationService).getApplicationByIdOrThrow(applicationId);
  }

  @Test
  void getApplication_whenAmendmentAndNotFound_throwsException() {
    UUID applicationId = UUID.randomUUID();

    when(scheduleWorkProgrammeApplicationService.getApplicationByIdOrThrow(applicationId))
        .thenThrow(new LmsEntityNotFoundException("schedule amendment application", applicationId));

    assertThatThrownBy(() -> applicationService.getApplication(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, applicationId))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessageContaining("schedule amendment application");
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Service
public class ApplicationService {

  private final LicenceContinuationService licenceContinuationService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public ApplicationService(
      LicenceContinuationService licenceContinuationService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  public LicenceApplication getApplication(
      ApplicationType applicationType,
      UUID applicationId
  ) {
    return switch (applicationType) {
      case CONTINUATION_APPLICATION -> licenceContinuationService.getApplicationByIdOrThrow(applicationId);
      case SCHEDULE_AMENDMENT_APPLICATION -> scheduleWorkProgrammeApplicationService.getApplicationByIdOrThrow(applicationId);
    };
  }
}
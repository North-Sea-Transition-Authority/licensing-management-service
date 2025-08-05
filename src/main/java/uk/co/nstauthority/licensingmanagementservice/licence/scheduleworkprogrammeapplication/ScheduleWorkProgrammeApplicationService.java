package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;

@Service
public class ScheduleWorkProgrammeApplicationService {

  private final ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;
  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public ScheduleWorkProgrammeApplicationService(
      ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.scheduleWorkProgrammeApplicationRepository = scheduleWorkProgrammeApplicationRepository;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  @Transactional
  public ScheduleWorkProgrammeApplicationDetail createNewScheduleWorkProgrammeApplicationForLicence(
      @NotNull Licence licence,
      boolean allLicenseesPermissionConfirmed) {
    var scheduleWorkProgrammeApplication = createScheduleWorkProgrammeApplication(licence);
    var scheduleWorkProgrammeApplicationDetail = createScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplication,
        allLicenseesPermissionConfirmed
    );

    scheduleWorkProgrammeApplicationRepository.save(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetailRepository.save(scheduleWorkProgrammeApplicationDetail);

    return scheduleWorkProgrammeApplicationDetail;
  }

  private ScheduleWorkProgrammeApplication createScheduleWorkProgrammeApplication(Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.getByScheduleDetailByLicenceOrThrow(licence);

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setLicenceScheduleDetail(licenceScheduleDetail);
    return scheduleWorkProgrammeApplication;
  }

  private ScheduleWorkProgrammeApplicationDetail createScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication,
      Boolean allLicenseesPermissionConfirmed) {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);

    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(allLicenseesPermissionConfirmed);

    return scheduleWorkProgrammeApplicationDetail;
  }

  public ScheduleWorkProgrammeApplicationDetail getDetailByIdOrThrow(UUID detailId) {
    return scheduleWorkProgrammeApplicationDetailRepository.findById(detailId)
        .orElseThrow(() -> new LmsEntityNotFoundException("schedule work programme application detail", detailId));
  }
}

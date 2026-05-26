package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;

@Service
public class LicenceScheduleSupportingInformationHelperService {

  private final LicenceScheduleExtensionService licenceScheduleExtensionService;
  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public LicenceScheduleSupportingInformationHelperService(
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository
  ) {
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  public boolean isExtensionOrAmendment(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)
        ||  licenceWorkProgrammeAmendmentRepository
        .existsByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeCompletionDateChangeRequestedTrue(
            scheduleWorkProgrammeApplicationDetail);
  }

}
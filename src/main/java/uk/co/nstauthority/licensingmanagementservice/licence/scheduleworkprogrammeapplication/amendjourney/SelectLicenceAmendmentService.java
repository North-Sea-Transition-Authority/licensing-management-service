package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class SelectLicenceAmendmentService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  private final WorkProgrammeActivityService workProgrammeActivityService;


  public SelectLicenceAmendmentService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @Transactional
  public void saveAmendmentForm(UUID workProgrammeActivityId,
                                SelectLicenceAmendmentForm licenceScheduleAmendmentForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
            scheduleWorkProgrammeApplicationDetail,
            workProgrammeActivityService.findWorkProgrammeActivityByIdOrThrow(workProgrammeActivityId))
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeActivity(
        workProgrammeActivityService.findWorkProgrammeActivityByIdOrThrow(workProgrammeActivityId));
    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgrammeAmendmentRequest);
  }
}
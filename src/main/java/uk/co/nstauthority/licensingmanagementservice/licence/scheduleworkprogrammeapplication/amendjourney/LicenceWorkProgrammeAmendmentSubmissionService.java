package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentSubmissionService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public LicenceWorkProgrammeAmendmentSubmissionService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  public boolean isAmendmentSectionSubmittable(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    List<LicenceWorkProgrammeAmendmentRequest> workProgrammeApplicationDetails =
        licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail);

    return !workProgrammeApplicationDetails.isEmpty();
  }
}
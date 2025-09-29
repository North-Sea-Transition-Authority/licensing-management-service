package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public LicenceWorkProgrammeAmendmentService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  public Optional<LicenceWorkProgrammeAmendmentRequest> getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail, UUID workProgrammeActivityId) {
    return licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId);
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

/**
 * Read-only lookups over {@link LicenceWorkProgrammeAmendmentRequest}, kept free of any dependency on
 * {@link LicenceWorkProgrammeAmendmentService} so other services can query amendment requests without risking a
 * circular dependency with anything that service itself depends on.
 */
@Service
public class LicenceWorkProgrammeAmendmentRequestQueryService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public LicenceWorkProgrammeAmendmentRequestQueryService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository
  ) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  public List<LicenceWorkProgrammeAmendmentRequest> getAllByApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return licenceWorkProgrammeAmendmentRepository
        .findAllByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
  }
}

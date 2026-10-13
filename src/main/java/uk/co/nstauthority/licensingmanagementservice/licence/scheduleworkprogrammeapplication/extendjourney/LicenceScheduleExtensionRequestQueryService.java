package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

/**
 * Read-only lookups over {@link LicenceScheduleExtensionRequest}, kept free of any dependency on
 * {@link LicenceScheduleExtensionService} so other services can query extension requests without risking a
 * circular dependency with anything that service itself depends on.
 */
@Service
public class LicenceScheduleExtensionRequestQueryService {

  private final LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  public LicenceScheduleExtensionRequestQueryService(LicenceScheduleExtensionRepository licenceScheduleExtensionRepository) {
    this.licenceScheduleExtensionRepository = licenceScheduleExtensionRepository;
  }

  public List<LicenceScheduleExtensionRequest> getAllByApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return licenceScheduleExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
  }
}

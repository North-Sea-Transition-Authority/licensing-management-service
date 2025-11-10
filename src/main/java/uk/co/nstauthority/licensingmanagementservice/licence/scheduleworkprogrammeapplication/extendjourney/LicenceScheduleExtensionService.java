package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionService {

  private final LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  public LicenceScheduleExtensionService(
      LicenceScheduleExtensionRepository licenceScheduleExtensionRepository) {
    this.licenceScheduleExtensionRepository = licenceScheduleExtensionRepository;
  }

  public Optional<LicenceScheduleExtensionRequest> getExtensionRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }

  public boolean isExtensionRequested(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionRepository
        .existsLicenceScheduleExtensionRequestByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }
}
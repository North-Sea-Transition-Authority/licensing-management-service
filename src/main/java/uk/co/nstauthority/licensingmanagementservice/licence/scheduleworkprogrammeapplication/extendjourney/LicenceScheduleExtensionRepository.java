package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface LicenceScheduleExtensionRepository
    extends JpaRepository<LicenceScheduleExtensionRequest, UUID>, NotDuplicationSource {

  List<LicenceScheduleExtensionRequest> findAllByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  Optional<LicenceScheduleExtensionRequest> findByScheduleWorkProgrammeApplicationDetailsAndLicenceSchedulePhaseId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID phaseId
  );

  Optional<LicenceScheduleExtensionRequest> findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID termId);

  boolean existsLicenceScheduleExtensionRequestByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  void deleteByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);
}
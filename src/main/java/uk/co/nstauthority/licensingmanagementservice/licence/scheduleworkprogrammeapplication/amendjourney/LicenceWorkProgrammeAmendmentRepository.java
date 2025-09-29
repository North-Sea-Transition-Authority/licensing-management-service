package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface LicenceWorkProgrammeAmendmentRepository extends JpaRepository<LicenceWorkProgrammeAmendmentRequest, UUID> {
  Optional<LicenceWorkProgrammeAmendmentRequest> findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails, UUID workProgrammeActivityId);

  boolean existsByWorkProgrammeActivityId(UUID workProgrammeActivityId);
}
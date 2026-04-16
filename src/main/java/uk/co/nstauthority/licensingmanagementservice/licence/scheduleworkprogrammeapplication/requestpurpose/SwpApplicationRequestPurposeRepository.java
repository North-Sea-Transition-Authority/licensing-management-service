package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface SwpApplicationRequestPurposeRepository
    extends JpaRepository<SwpApplicationRequestPurpose, UUID>, NotDuplicationSource {
  Optional<SwpApplicationRequestPurpose> getByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);
}

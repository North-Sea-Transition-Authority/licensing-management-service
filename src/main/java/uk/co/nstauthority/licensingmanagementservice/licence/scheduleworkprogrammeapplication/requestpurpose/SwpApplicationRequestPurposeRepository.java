package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface SwpApplicationRequestPurposeRepository
    extends JpaRepository<SwpApplicationRequestPurpose, UUID>, DuplicationSource<ScheduleWorkProgrammeApplicationDetail> {
  @DuplicateThisOnUpdate
  @EntityGraph(attributePaths = "scheduleWorkProgrammeApplicationDetail")
  Optional<SwpApplicationRequestPurpose> getByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);
}

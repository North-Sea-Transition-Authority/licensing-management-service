package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface ScheduleWorkProgrammeApplicationDetailRepository
    extends JpaRepository<ScheduleWorkProgrammeApplicationDetail, UUID>, NotDuplicationSource {

  Optional<ScheduleWorkProgrammeApplicationDetail> getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication);

  int countByVersionNumberAndSubmittedDatetimeBetween(Integer versionNumber, Instant startOfYear, Instant endOfYear);

  List<ScheduleWorkProgrammeApplicationDetail> findAllByStatusIn(Set<ScheduleWorkProgrammeApplicationStatus> statuses);
}
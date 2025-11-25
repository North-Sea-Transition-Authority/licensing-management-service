package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleWorkProgrammeApplicationDetailRepository
    extends JpaRepository<ScheduleWorkProgrammeApplicationDetail, UUID> {

  Optional<ScheduleWorkProgrammeApplicationDetail> getScheduleWorkProgrammeApplicationDetailByScheduleWorkProgrammeApplication(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication);

  int countByVersionNumberAndStatusAndSubmittedDatetimeBetween(Integer versionNumber,
                                                               ScheduleWorkProgrammeApplicationStatus status,
                                                               Instant startOfYear,
                                                               Instant endOfYear);
}
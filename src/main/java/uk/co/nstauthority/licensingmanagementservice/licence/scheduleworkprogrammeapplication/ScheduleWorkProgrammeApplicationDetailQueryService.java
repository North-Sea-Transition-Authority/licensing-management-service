package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read-only lookups over {@link ScheduleWorkProgrammeApplicationDetail}, kept free of any dependency on
 * {@link ScheduleWorkProgrammeApplicationService} so other services can query application details without
 * risking a circular dependency with anything that service itself depends on.
 */
@Service
public class ScheduleWorkProgrammeApplicationDetailQueryService {

  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  public ScheduleWorkProgrammeApplicationDetailQueryService(
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository
  ) {
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
  }

  public Optional<ScheduleWorkProgrammeApplicationDetail> getLatestByApplication(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication
  ) {
    return scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(scheduleWorkProgrammeApplication);
  }

  public List<ScheduleWorkProgrammeApplicationDetail> getAllByApplicationIds(
      Collection<UUID> scheduleWorkProgrammeApplicationIds
  ) {
    return scheduleWorkProgrammeApplicationDetailRepository
        .findAllByScheduleWorkProgrammeApplication_IdIn(scheduleWorkProgrammeApplicationIds);
  }
}

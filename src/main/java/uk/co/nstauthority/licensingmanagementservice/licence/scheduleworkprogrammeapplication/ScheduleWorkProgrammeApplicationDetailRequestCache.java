package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds application details loaded by id for the lifetime of a single HTTP request, so the access rules and the
 * controller's argument resolver share one load of the detail named in the URL. Used internally by
 * {@link ScheduleWorkProgrammeApplicationService}; go through that service rather than using this directly.
 */
@Component
public class ScheduleWorkProgrammeApplicationDetailRequestCache
    extends RequestScopedCache<UUID, Optional<ScheduleWorkProgrammeApplicationDetail>> {

  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  public ScheduleWorkProgrammeApplicationDetailRequestCache(
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository
  ) {
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
  }

  public Optional<ScheduleWorkProgrammeApplicationDetail> findById(UUID detailId) {
    return get(detailId);
  }

  @Override
  protected Optional<ScheduleWorkProgrammeApplicationDetail> load(UUID detailId) {
    return scheduleWorkProgrammeApplicationDetailRepository.findById(detailId);
  }
}

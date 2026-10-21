package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds each term's phases for the lifetime of a single HTTP request, so a page that works out the same schedule state
 * in several places loads each term's phases once. Used internally by {@code LicenceScheduleStateService} when working
 * out the schedule state; anything that changes phases should use {@link LicenceSchedulePhaseService}.
 */
@Component
public class LicenceSchedulePhaseRequestCache extends RequestScopedCache<LicenceScheduleTerm, List<LicenceSchedulePhase>> {

  private final LicenceSchedulePhaseService licenceSchedulePhaseService;

  public LicenceSchedulePhaseRequestCache(LicenceSchedulePhaseService licenceSchedulePhaseService) {
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
  }

  public List<LicenceSchedulePhase> getPhasesByTerm(LicenceScheduleTerm licenceScheduleTerm) {
    return get(licenceScheduleTerm);
  }

  @Override
  protected Object cacheKey(LicenceScheduleTerm licenceScheduleTerm) {
    return licenceScheduleTerm.getId();
  }

  @Override
  protected List<LicenceSchedulePhase> load(LicenceScheduleTerm licenceScheduleTerm) {
    return licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm);
  }
}

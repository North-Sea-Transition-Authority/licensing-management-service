package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds each schedule detail's terms for the lifetime of a single HTTP request, so a page that reads the same detail's
 * terms in several places loads them once. Used for reads by {@code LicenceScheduleStateService} when working out the
 * schedule state and by {@code LicenceScheduleTimelineService} when building the timeline; anything that changes terms
 * should use {@link LicenceScheduleTermService}.
 */
@Component
public class LicenceScheduleTermRequestCache extends RequestScopedCache<LicenceScheduleDetail, List<LicenceScheduleTerm>> {

  private final LicenceScheduleTermService licenceScheduleTermService;

  public LicenceScheduleTermRequestCache(LicenceScheduleTermService licenceScheduleTermService) {
    this.licenceScheduleTermService = licenceScheduleTermService;
  }

  public List<LicenceScheduleTerm> getTermsByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    return get(licenceScheduleDetail);
  }

  @Override
  protected Object cacheKey(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleDetail.getId();
  }

  @Override
  protected List<LicenceScheduleTerm> load(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
  }
}

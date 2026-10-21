package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds each schedule detail's terms for the lifetime of a single HTTP request, so a page that works out the same
 * detail's schedule state in several places loads its terms once. Used internally by {@code LicenceScheduleStateService}
 * when working out the schedule state; anything that changes terms should use {@link LicenceScheduleTermService}.
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

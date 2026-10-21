package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds each licence's active schedule detail for the lifetime of a single HTTP request, so the access rules, the
 * controller and the task list sections of a continuation application share one lookup of it. Used internally by
 * {@link LicenceContinuationService} to find the schedule a draft continuation is assessed against; anything that
 * changes which schedule detail is active should use {@link LicenceScheduleDetailService}.
 */
@Component
public class LicenceContinuationScheduleDetailRequestCache extends RequestScopedCache<Licence, LicenceScheduleDetail> {

  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public LicenceContinuationScheduleDetailRequestCache(LicenceScheduleDetailService licenceScheduleDetailService) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  public LicenceScheduleDetail getActiveScheduleDetail(Licence licence) {
    return get(licence);
  }

  @Override
  protected Object cacheKey(Licence licence) {
    return licence.getId();
  }

  @Override
  protected LicenceScheduleDetail load(Licence licence) {
    return licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );
  }
}

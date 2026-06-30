package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@Component
@Order(10)
public class ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRule implements AccessInterceptorRule {

  private final LicenceContinuationService licenceContinuationService;
  private final LicenceScheduleService licenceScheduleService;

  @Autowired
  public ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRule(
      LicenceContinuationService licenceContinuationService,
      LicenceScheduleService licenceScheduleService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.licenceScheduleService = licenceScheduleService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return ContinuationApplicationHasWorkProgrammeActivities.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var applicationDetail = getApplicationDetailFromRequest(request);
    var scheduleDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail);

    if (licenceScheduleService.hasCurrentWorkProgrammeActivities(scheduleDetail)) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Continuation application detail %s has no work programme activities on its schedule"
            .formatted(applicationDetail.getId())
    );
  }

  private LicenceContinuationApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID
    );
    return licenceContinuationService.getDetailByIdOrThrow(applicationDetailId);
  }
}

package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;

@Component
@Order(8)
public class RequestPurposeChoiceMustBeApplicableRule implements AccessInterceptorRule {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  @Autowired
  public RequestPurposeChoiceMustBeApplicableRule(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      SwpApplicationRequestPurposeService swpApplicationRequestPurposeService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.swpApplicationRequestPurposeService = swpApplicationRequestPurposeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return RequestPurposeChoiceMustBeApplicable.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var applicationDetail = getApplicationDetailFromRequest(request);

    if (swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(applicationDetail)) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Request purpose choice is not applicable for application detail %s (no work programme activities to amend)"
            .formatted(applicationDetail.getId())
    );
  }

  private ScheduleWorkProgrammeApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID
    );
    return scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId);
  }
}

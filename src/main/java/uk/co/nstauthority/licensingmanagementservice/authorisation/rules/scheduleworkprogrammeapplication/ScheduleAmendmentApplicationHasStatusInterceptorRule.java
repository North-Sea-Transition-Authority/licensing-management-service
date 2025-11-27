package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

@Component
@Order(4)
public class ScheduleAmendmentApplicationHasStatusInterceptorRule implements AccessInterceptorRule {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Autowired
  public ScheduleAmendmentApplicationHasStatusInterceptorRule(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return ScheduleAmendmentApplicationHasStatus.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var applicationHasStatus = (ScheduleAmendmentApplicationHasStatus) annotation;

    if (applicationHasStatus.value().length == 0) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No statuses provided to security annotation");
    }

    var applicationDetail = getApplicationDetailFromRequest(request);

    for (ScheduleWorkProgrammeApplicationStatus status : applicationHasStatus.value()) {
      if (status.equals(applicationDetail.getStatus())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Application with detail id %s is not in an expected status"
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

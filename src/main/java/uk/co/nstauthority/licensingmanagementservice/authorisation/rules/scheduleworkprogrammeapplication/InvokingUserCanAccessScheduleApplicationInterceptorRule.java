package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Component
@Order(5)
public class InvokingUserCanAccessScheduleApplicationInterceptorRule implements AccessInterceptorRule {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;

  @Autowired
  public InvokingUserCanAccessScheduleApplicationInterceptorRule(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserCanAccessScheduleApplication.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

    var wuaId = userDetailService.getUserDetail().wuaId();

    var applicationDetail = getApplicationDetailFromRequest(request);
    var applicationId = applicationDetail.getScheduleWorkProgrammeApplication().getId();

    boolean hasAccess = applicationAccessService.userHasAccessToApplication(
        applicationId.toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        wuaId
    );

    if (hasAccess) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "wuaId %s does not have permission to access application %s".formatted(wuaId, applicationId)
    );
  }

  private ScheduleWorkProgrammeApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID
    );
    return scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId);
  }
}
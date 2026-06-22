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
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Component
@Order(7)
public class InvokingUserCanAccessScheduleApplicationInterceptorRule implements AccessInterceptorRule {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Autowired
  public InvokingUserCanAccessScheduleApplicationInterceptorRule(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
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
    var orgUnitToGroupMap = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(applicationDetail.getLicence());

    var hasAccess = applicationAccessService.userHasAccessToApplication(
        applicationDetail,
        orgUnitToGroupMap,
        wuaId
    );

    if (hasAccess) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "wuaId %s does not have permission to access application %s"
            .formatted(wuaId, applicationDetail.getLicenceApplication().getId())
    );
  }

  private ScheduleWorkProgrammeApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID
    );
    return scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId);
  }
}
package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Component
@Order(8)
public class InvokingUserCanAccessContinuationApplicationInterceptorRule implements AccessInterceptorRule {

  private final LicenceContinuationService licenceContinuationService;
  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Autowired
  public InvokingUserCanAccessContinuationApplicationInterceptorRule(
      LicenceContinuationService licenceContinuationService,
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserCanAccessContinuationApplication.class;
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

  private LicenceContinuationApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID
    );
    return licenceContinuationService.getDetailByIdOrThrow(applicationDetailId);
  }
}
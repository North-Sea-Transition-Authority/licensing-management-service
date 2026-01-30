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
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;

@Component
@Order(8)
public class InvokingUserCanAccessContinuationApplicationInterceptorRule implements AccessInterceptorRule {

  private final LicenceContinuationService licenceContinuationService;
  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;

  @Autowired
  public InvokingUserCanAccessContinuationApplicationInterceptorRule(
      LicenceContinuationService licenceContinuationService,
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
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
    var applicationId = applicationDetail.getId();

    boolean hasAccess = applicationAccessService.userHasAccessToApplication(
        applicationId.toString(),
        ApplicationType.CONTINUATION_APPLICATION,
        null,
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

  private LicenceContinuationApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID
    );
    return licenceContinuationService.getDetailByIdOrThrow(applicationDetailId);
  }
}
package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;

@Component
@Order(6)
public class InvokingUserCanStartApplicationInterceptorRule implements AccessInterceptorRule {

  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;

  @Autowired
  public InvokingUserCanStartApplicationInterceptorRule(
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService
  ) {
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserCanStartApplication.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

    var wuaId = userDetailService.getUserDetail().wuaId();

    boolean hasAccessToStartApplication = applicationAccessService.userHasAccessToStartApplication(wuaId);

    if (hasAccessToStartApplication) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "wuaId %s does not have permission to create application"
    );
  }
}
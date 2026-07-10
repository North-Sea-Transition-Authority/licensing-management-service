package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;

@Component
@Order(8)
public class InvokingUserCanViewCorrectionInterceptorRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;
  private final UserDetailService userDetailService;

  public InvokingUserCanViewCorrectionInterceptorRule(
      LicenceCorrectionService licenceCorrectionService,
      UserDetailService userDetailService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserCanViewCorrection.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var correctionId = getPathVariableEntityIdFromRequest(request, "correctionId");

    var user = userDetailService.getUserDetail();

    var correction = licenceCorrectionService.findByIdAndAllocatedToWuaId(correctionId, user);
    if (correction.isEmpty()) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.FORBIDDEN,
          "Licence correction %s is not assigned to wuaId %s".formatted(correctionId, user.wuaId())
      );
    }

    request.setAttribute("validatedCorrection", correction.get());
    return SecurityRuleResult.continueAsNormal();
  }
}
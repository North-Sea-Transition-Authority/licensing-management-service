package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Component
@Order(9)
public class InvokingUserCanRemoveLicencePositionInterceptorRule implements AccessInterceptorRule {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public InvokingUserCanRemoveLicencePositionInterceptorRule(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserCanRemoveLicencePosition.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var positionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");

    var correction = (LicenceCorrection) request.getAttribute("validatedCorrection");
    var position  = licencePositionService.getPositionForLicence(correction.getLicence(), positionId);
    if (!licencePositionCorrectionService.canRemovePosition(correction, position)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.FORBIDDEN,
          "Position %s is not removable".formatted(positionId));
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
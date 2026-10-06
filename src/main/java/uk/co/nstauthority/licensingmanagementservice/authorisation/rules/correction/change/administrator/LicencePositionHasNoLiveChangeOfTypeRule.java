package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Component
@Order(10)
public class LicencePositionHasNoLiveChangeOfTypeRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public LicencePositionHasNoLiveChangeOfTypeRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionHasNoLiveChangeOfType.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var operationType = ((LicencePositionHasNoLiveChangeOfType) annotation).value();
    var licencePositionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");

    var hasLiveAdminChange = licencePositionChangeService.changeExists(licencePositionId, operationType);
    if (hasLiveAdminChange) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.CONFLICT,
          "Licence position %s already has a live %s"
              .formatted(licencePositionId, operationType.getSimpleName())
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}

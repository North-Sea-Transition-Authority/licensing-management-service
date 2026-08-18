package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.equity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Component
@Order(10)
public class ValidLicencePositionEquityChangeRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public ValidLicencePositionEquityChangeRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return ValidLicencePositionEquityChange.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var changeId = getPathVariableEntityIdFromRequest(request, "changeId");

    var change = licencePositionChangeService.findById(changeId).orElse(null);

    if (change == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position change %s".formatted(changeId)
      );
    }

    if (!LicencePositionChange.containsEquityOperation(change)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Change %s is not a beneficial interest change".formatted(changeId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
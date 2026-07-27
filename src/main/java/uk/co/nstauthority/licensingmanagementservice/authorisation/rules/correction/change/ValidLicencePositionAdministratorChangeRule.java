package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Component
@Order(10)
public class ValidLicencePositionAdministratorChangeRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public ValidLicencePositionAdministratorChangeRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return ValidLicencePositionAdministratorChange.class;
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

    var isAdminChange = change.getOperations().stream().anyMatch(AdministratorOperation.class::isInstance);
    if (!isAdminChange) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Change %s is not an administrator change".formatted(changeId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
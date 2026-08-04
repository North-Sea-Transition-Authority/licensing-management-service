package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator;

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
public class LicencePositionHasNoLiveAdministratorChangeRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public LicencePositionHasNoLiveAdministratorChangeRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionHasNoLiveAdministratorChange.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");

    var hasLiveAdminChange = licencePositionChangeService.changeExists(licencePositionId, AdministratorOperation.class);
    if (hasLiveAdminChange) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.CONFLICT,
          "Licence position %s already has a live administrator change".formatted(licencePositionId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}

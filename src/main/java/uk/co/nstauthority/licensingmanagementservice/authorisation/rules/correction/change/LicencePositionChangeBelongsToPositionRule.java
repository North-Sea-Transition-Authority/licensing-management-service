package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

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
@Order(9)
public class LicencePositionChangeBelongsToPositionRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public LicencePositionChangeBelongsToPositionRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionChangeBelongsToPosition.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");
    var changeId = getPathVariableEntityIdFromRequest(request, "changeId");

    var change = licencePositionChangeService.findById(changeId).orElse(null);

    if (change == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position change %s".formatted(changeId)
      );
    }

    if (!change.getLicencePosition().getId().equals(licencePositionId)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Change %s does not belong to licence position %s".formatted(changeId, licencePositionId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}

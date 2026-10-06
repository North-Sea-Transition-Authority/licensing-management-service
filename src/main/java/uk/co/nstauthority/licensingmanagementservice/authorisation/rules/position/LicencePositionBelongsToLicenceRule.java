package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Component
@Order(9)
public class LicencePositionBelongsToLicenceRule implements AccessInterceptorRule {

  private final LicencePositionService licencePositionService;

  LicencePositionBelongsToLicenceRule(LicencePositionService licencePositionService) {
    this.licencePositionService = licencePositionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionBelongsToLicence.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionId = getPathVariableEntityIdFromRequest(request, LicencePosition.class);
    var licenceId = getPathVariableIdInteger(request, Licence.class);

    var licencePosition = licencePositionService.findById(licencePositionId).orElse(null);

    if (licencePosition == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position %s".formatted(licencePositionId)
      );
    }

    if (!licencePosition.getLicence().getId().equals(licenceId)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Licence position %s does not belong to licence %s".formatted(licencePositionId, licenceId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}

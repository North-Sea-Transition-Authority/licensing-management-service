package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Component
@Order(10)
public class LicencePositionBelongsToCorrectionLicenceRule implements AccessInterceptorRule {

  private final LicencePositionService licencePositionService;
  private final LicenceCorrectionService licenceCorrectionService;


  LicencePositionBelongsToCorrectionLicenceRule(LicencePositionService licencePositionService,
                                                LicenceCorrectionService licenceCorrectionService) {
    this.licencePositionService = licencePositionService;
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionBelongsToCorrectionLicence.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionId = getPathVariableEntityIdFromRequest(request, LicencePosition.class);
    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);

    var licencePosition = licencePositionService.findById(licencePositionId).orElse(null);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (licencePosition == null || correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position %s".formatted(licencePositionId)
      );
    }

    if (!licencePosition.getLicence().getId().equals(correction.getLicence().getId())) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Licence position %s does not belong to the licence of correction %s".formatted(
              licencePositionId,
              correction.getId())
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
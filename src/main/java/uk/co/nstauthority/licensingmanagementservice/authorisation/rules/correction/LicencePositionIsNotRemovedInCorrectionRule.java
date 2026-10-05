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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Component
@Order(10)
public class LicencePositionIsNotRemovedInCorrectionRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public LicencePositionIsNotRemovedInCorrectionRule(
      LicenceCorrectionService licenceCorrectionService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionIsNotRemovedInCorrection.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND,
          "Licence correction %s not found".formatted(correctionId)
      );
    }

    var positionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");

    if (licencePositionCorrectionService.findFirstAddedPositionCorrection(correction, positionId).isPresent()) {
      return SecurityRuleResult.continueAsNormal();
    }

    var position = licencePositionService.getPositionForLicence(correction.getLicence(), positionId);
    var positionRemovedInCorrection = licencePositionCorrectionService.isPositionRemovedInCorrection(correction, position);
    if (positionRemovedInCorrection) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.FORBIDDEN,
          "Licence position %s is removed.".formatted(positionId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
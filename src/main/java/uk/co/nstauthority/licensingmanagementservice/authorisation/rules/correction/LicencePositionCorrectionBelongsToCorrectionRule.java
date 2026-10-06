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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;

@Component
@Order(10)
public class LicencePositionCorrectionBelongsToCorrectionRule implements AccessInterceptorRule {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicenceCorrectionService licenceCorrectionService;

  LicencePositionCorrectionBelongsToCorrectionRule(LicencePositionCorrectionService licencePositionCorrectionService,
                                                   LicenceCorrectionService licenceCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionCorrectionBelongsToCorrection.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionCorrectionId = getPathVariableEntityIdFromRequest(request, LicencePositionCorrection.class);
    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);


    var licencePositionCorrection = licencePositionCorrectionService.findById(licencePositionCorrectionId).orElse(null);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (licencePositionCorrection == null || correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position correction %s".formatted(licencePositionCorrectionId)
      );
    }

    if (!licencePositionCorrection.getLicenceCorrection().getId().equals(correction.getId())) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Licence position correction %s does not belong to correction %s".formatted(
              licencePositionCorrectionId,
              correction.getId())
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
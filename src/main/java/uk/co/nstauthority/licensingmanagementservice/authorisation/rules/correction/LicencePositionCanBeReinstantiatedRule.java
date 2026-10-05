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
@Order(9)
public class LicencePositionCanBeReinstantiatedRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public LicencePositionCanBeReinstantiatedRule(
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
    return LicencePositionCanBeReinstantiated.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var positionId = getPathVariableEntityIdFromRequest(request, "licencePositionId");

    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND,
          "Licence correction %s not found".formatted(correctionId)
      );
    }

    var position = licencePositionService.getPositionForLicence(correction.getLicence(), positionId);
    if (!licencePositionCorrectionService.canReinstateDeletedPositionCorrection(correction, position)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.FORBIDDEN,
          "Licence position %s is not marked for deletion and cannot be reinstated".formatted(positionId));
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
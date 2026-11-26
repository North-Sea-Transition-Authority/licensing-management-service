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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

@Component
@Order(10)
public class LicencePositionStagedPayloadBelongsToCorrectionRule implements AccessInterceptorRule {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final LicenceCorrectionService licenceCorrectionService;

  LicencePositionStagedPayloadBelongsToCorrectionRule(LicencePositionStagedPayloadService licencePositionStagedPayloadService,
                                                   LicenceCorrectionService licenceCorrectionService) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionStagedPayloadBelongsToCorrection.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var licencePositionStagedPayloadId = getPathVariableEntityIdFromRequest(request, LicencePositionStagedPayload.class);
    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);


    var licencePositionStagedPayload = licencePositionStagedPayloadService.findById(licencePositionStagedPayloadId).orElse(null);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (licencePositionStagedPayload == null || correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position staged payload %s".formatted(licencePositionStagedPayloadId)
      );
    }

    if (!licencePositionStagedPayload.getLicenceCorrection().getId().equals(correction.getId())) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Licence position staged payload %s does not belong to correction %s".formatted(
              licencePositionStagedPayloadId,
              correction.getId())
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.util.RequestUtil;

@Component
@Order(9)
public class LicencePositionChangeBelongsToPositionRule implements AccessInterceptorRule {

  private static final String CORRECTION_ID_PATH_VARIABLE = "correctionId";

  private final LicencePositionChangeService licencePositionChangeService;
  private final LicenceCorrectionService licenceCorrectionService;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  public LicencePositionChangeBelongsToPositionRule(
      LicencePositionChangeService licencePositionChangeService,
      LicenceCorrectionService licenceCorrectionService,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.licencePositionChangeService = licencePositionChangeService;
    this.licenceCorrectionService = licenceCorrectionService;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
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

    if (!change.getLicencePosition().getId().equals(licencePositionId)
        && !isMovedToPositionInCorrection(request, licencePositionId, changeId)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Change %s does not belong to licence position %s".formatted(changeId, licencePositionId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }

  private boolean isMovedToPositionInCorrection(HttpServletRequest request, UUID licencePositionId, UUID changeId) {
    return RequestUtil.getId(request, CORRECTION_ID_PATH_VARIABLE)
        .flatMap(licenceCorrectionService::findById)
        .map(licencePositionStagedPayloadService::getPositionStagedPayloads)
        .map(LicencePositionStagedPayloadService::getUpdatedChangePositionIds)
        .map(stagedPositionIds -> licencePositionId.equals(stagedPositionIds.get(changeId.toString())))
        .orElse(false);
  }
}

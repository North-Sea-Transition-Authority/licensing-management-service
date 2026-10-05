package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;

@Component
@Order(9)
public class CorrectionHasStatusInterceptorRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;

  public CorrectionHasStatusInterceptorRule(LicenceCorrectionService licenceCorrectionService) {
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return CorrectionHasStatus.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var correctionHasStatus = (CorrectionHasStatus) annotation;

    if (correctionHasStatus.value().length == 0) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No statuses provided to security annotation"
      );
    }

    var correctionId = getPathVariableEntityIdFromRequest(request, LicenceCorrection.class);
    var correction = licenceCorrectionService.findById(correctionId).orElse(null);

    if (correction == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND,
          "Licence correction %s not found".formatted(correctionId)
      );
    }

    for (LicenceCorrectionStatus status : correctionHasStatus.value()) {
      if (status.equals(correction.getStatus())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Licence correction %s is not in an expected status".formatted(correction.getId())
    );
  }
}
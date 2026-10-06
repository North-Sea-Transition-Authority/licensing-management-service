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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;

@Component
@Order(9)
public class CorrectionLicenceIsTypeRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;

  public CorrectionLicenceIsTypeRule(LicenceCorrectionService licenceCorrectionService) {
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return CorrectionLicenceIsType.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var licenceIsType = (CorrectionLicenceIsType) annotation;

    if (licenceIsType.value().length == 0) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No licence types provided to security annotation"
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

    var licence = correction.getLicence();

    for (LicenceType type : licenceIsType.value()) {
      if (type.equals(licence.getType())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Licence %s is not of an expected type".formatted(licence.getId())
    );
  }
}
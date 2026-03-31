package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;

@Component
@Order(9)
public class ContinuationApplicationHasStatusInterceptorRule implements AccessInterceptorRule {

  private final LicenceContinuationService licenceContinuationService;

  @Autowired
  public ContinuationApplicationHasStatusInterceptorRule(
      LicenceContinuationService licenceContinuationService
  ) {
    this.licenceContinuationService = licenceContinuationService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return ContinuationApplicationHasStatus.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var applicationHasStatus = (ContinuationApplicationHasStatus) annotation;

    if (applicationHasStatus.value().length == 0) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No statuses provided to security annotation"
      );
    }

    var applicationDetail = getApplicationDetailFromRequest(request);

    for (LicenceContinuationApplicationStatus status : applicationHasStatus.value()) {
      if (status.equals(applicationDetail.getStatus())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Application with detail id %s is not in an expected status".formatted(applicationDetail.getId())
    );
  }

  private LicenceContinuationApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    Map<String, String> pathVariables = (Map<String, String>) request
        .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (pathVariables != null) {
      if (pathVariables.containsKey(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)) {
        var detailId = UUID.fromString(pathVariables.get(
            LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID
        ));
        return licenceContinuationService.getDetailByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey("applicationId")) {
        var applicationId = UUID.fromString(pathVariables.get("applicationId"));
        return licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(applicationId);
      }
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No application ID or detail ID found in path variables");

  }
}
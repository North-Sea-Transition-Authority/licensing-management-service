package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationService;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationStatus;

@Component
@Order(4)
public class XyzApplicationHasStatusInterceptorRule implements AccessInterceptorRule {

  private final XyzApplicationService xyzApplicationService;

  @Autowired
  public XyzApplicationHasStatusInterceptorRule(
      XyzApplicationService xyzApplicationService
  ) {
    this.xyzApplicationService = xyzApplicationService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return XyzApplicationHasStatus.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var xyzApplicationHasStatus = (XyzApplicationHasStatus) annotation;

    if (xyzApplicationHasStatus.value().length == 0) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No statuses provided to security annotation");
    }

    var application = getApplicationFromRequest(request);

    for (XyzApplicationStatus xyzApplicationStatus : xyzApplicationHasStatus.value()) {
      if (xyzApplicationStatus.equals(application.getStatus())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Current xyz application %s is not in an expected status"
            .formatted(application.getReference())
    );
  }

  private XyzApplication getApplicationFromRequest(HttpServletRequest request) {
    var applicationId = getPathVariableEntityIdFromRequest(request, XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME);
    return xyzApplicationService.getXyzApplicationById(applicationId);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface XyzApplicationHasStatus {
    XyzApplicationStatus[] value();
  }
}

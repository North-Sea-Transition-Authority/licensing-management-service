package uk.co.nstauthority.template.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.template.authentication.UserDetailService;
import uk.co.nstauthority.template.authorisation.SecurityRuleResult;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationService;
import uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionItem;
import uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionService;

@Component
@Order(3)
public class ActionEndPointInterceptorRule implements AccessInterceptorRule {

  private final UserDetailService userDetailService;
  private final CaseProcessingActionService caseProcessingActionService;
  private final XyzApplicationService xyzApplicationService;

  public ActionEndPointInterceptorRule(
      UserDetailService userDetailService,
      CaseProcessingActionService caseProcessingActionService,
      XyzApplicationService xyzApplicationService
  ) {
    this.userDetailService = userDetailService;
    this.caseProcessingActionService = caseProcessingActionService;
    this.xyzApplicationService = xyzApplicationService;
  }


  @Override
  public Class<? extends Annotation> supports() {
    return ActionEndPoint.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var expectedActionItems = ((ActionEndPoint) annotation).value();
    var serviceUserDetail = userDetailService.getUserDetail();

    var xyzApplication = getXyzApplicationFromRequest(request);

    var userActionItems = caseProcessingActionService.getAvailableUserActionItems(xyzApplication, serviceUserDetail);

    if (CollectionUtils.containsAny(userActionItems, Set.of(expectedActionItems))) {
      return SecurityRuleResult.continueAsNormal();
    }

    var errorMessage =
        "Attempted to use action item(s) %s on xyzApplication %s"
            .formatted(
                Arrays.stream(expectedActionItems)
                    .map(CaseProcessingActionItem::name)
                    .collect(Collectors.joining(",")),
                xyzApplication.getId()
            );

    return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.FORBIDDEN, errorMessage);
  }

  private XyzApplication getXyzApplicationFromRequest(HttpServletRequest request) {
    var applicationId = getPathVariableEntityIdFromRequest(request, XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME);
    return xyzApplicationService.getXyzApplicationById(applicationId);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface ActionEndPoint {
    CaseProcessingActionItem[] value();
  }
}

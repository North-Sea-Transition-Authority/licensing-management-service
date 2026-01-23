package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.search.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.search.action.LicenceActionService;

@Component
@Order(3)
public class LicenceActionEndPointInterceptorRule implements AccessInterceptorRule {

  private final UserDetailService userDetailService;
  private final LicenceActionService licenceActionService;
  private final LicenceService licenceService;

  public LicenceActionEndPointInterceptorRule(
      UserDetailService userDetailService,
      LicenceActionService licenceActionService,
      LicenceService licenceService
  ) {
    this.userDetailService = userDetailService;
    this.licenceActionService = licenceActionService;
    this.licenceService = licenceService;
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
    var serviceUserDetail = userDetailService.getUserDetail();

    var licence = getLicenceFromRequest(request);
    var expectedActionItems = Arrays.stream(((ActionEndPoint) annotation).value())
        .map(licenceActionItem -> licenceActionItem.toActionItemView(licence))
        .toList();

    var userActionItems = licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail);

    if (!CollectionUtils.containsAny(userActionItems, expectedActionItems)) {
      var errorMessage = "Attempted to use action item(s) %s on create licence schedule %s".formatted(
          expectedActionItems
              .stream()
              .map(ActionItemView::displayName)
              .collect(Collectors.joining(",")), licence.getId()
      );
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.FORBIDDEN, errorMessage);
    } else {
      return SecurityRuleResult.continueAsNormal();
    }
  }

  private Licence getLicenceFromRequest(HttpServletRequest request) {
    var licenceId = getPathVariableIdInteger(request, LicenceArgumentResolver.LICENCE_ID);

    return licenceService.findLicenceByIdOrThrow(licenceId);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface ActionEndPoint {
    LicenceActionItem[] value();
  }
}

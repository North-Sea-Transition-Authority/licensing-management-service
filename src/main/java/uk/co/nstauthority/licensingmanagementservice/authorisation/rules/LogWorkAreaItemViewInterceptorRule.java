package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Component
@Order(10)
public class LogWorkAreaItemViewInterceptorRule implements AccessInterceptorRule {

  private final WorkAreaItemViewService workAreaItemViewService;
  private final UserDetailService userDetailService;

  public LogWorkAreaItemViewInterceptorRule(
      WorkAreaItemViewService workAreaItemViewService,
      UserDetailService userDetailService
  ) {
    this.workAreaItemViewService = workAreaItemViewService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LogWorkAreaItemView.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var logAnnotation = (LogWorkAreaItemView) annotation;

    if (logAnnotation.disable()) {
      return SecurityRuleResult.continueAsNormal();
    }

    var itemId = getPathVariableEntityIdFromRequest(request, logAnnotation.pathVariable());
    var userId = userDetailService.getUserDetail().wuaId();
    var view = new WorkAreaItemView(itemId, logAnnotation.itemType(), userId);

    if (!workAreaItemViewService.hasUserViewedItem(view)) {
      workAreaItemViewService.logWorkAreaItemView(view);
    }

    return SecurityRuleResult.continueAsNormal();
  }
}

package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionService;

@Component
@Order(9)
public class ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule implements AccessInterceptorRule {

  private final UserDetailService userDetailService;
  private final ScheduleWorkProgrammeApplicationActionService applicationActionService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule(
      UserDetailService userDetailService,
      ScheduleWorkProgrammeApplicationActionService applicationActionService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService
  ) {
    this.userDetailService = userDetailService;
    this.applicationActionService = applicationActionService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
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
    var applicationDetail = getApplicationDetailFromRequest(request);
    var expectedActionItems = Arrays.stream(((ActionEndPoint) annotation).value())
        .map(actionItem -> actionItem.toActionItemView(applicationDetail))
        .toList();

    var userActionItems = applicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    if (!CollectionUtils.containsAny(userActionItems, expectedActionItems)) {
      var errorMessage = "Attempted to use action item(s) %s on schedule work programme application detail %s".formatted(
          expectedActionItems
              .stream()
              .map(ActionItemView::displayName)
              .collect(Collectors.joining(",")),
          applicationDetail.getId()
      );
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.FORBIDDEN, errorMessage);
    } else {
      return SecurityRuleResult.continueAsNormal();
    }
  }

  private ScheduleWorkProgrammeApplicationDetail getApplicationDetailFromRequest(HttpServletRequest request) {
    var applicationDetailId = getPathVariableEntityIdFromRequest(
        request, ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID
    );
    return scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface ActionEndPoint {
    ScheduleWorkProgrammeApplicationActionItem[] value();
  }
}

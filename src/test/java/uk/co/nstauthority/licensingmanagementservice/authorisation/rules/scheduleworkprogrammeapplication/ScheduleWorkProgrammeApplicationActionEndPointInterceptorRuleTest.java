package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionService;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationActionEndPointInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private ScheduleWorkProgrammeApplicationActionService applicationActionService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule rule;

  private ServiceUserDetail user;
  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  private interface TestEndpoints {
    @ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD)
    void getAction();
  }

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class);
  }

  @Test
  void check_matchesEndpoint_thenTrue() throws NoSuchMethodException {
    setupMocks();

    when(applicationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail)));

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_doesNotMatchAction_thenFalse() throws NoSuchMethodException {
    setupMocks();

    when(applicationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of());

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus,
        SecurityRuleResult::failureMessage
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN,
        "Attempted to use action item(s) %s on schedule work programme application detail %s"
            .formatted(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.getDisplayName(), applicationDetail.getId())
    );
  }

  @Test
  void check_missingPathVariable_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    Map<String, String> uriTemplateVariables = new HashMap<>();
    uriTemplateVariables.put(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, null);

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(uriTemplateVariables);

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"Path variable scheduleWorkProgrammeApplicationDetailId not found in request\"");
  }

  @Test
  void check_malformedInteger_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, "not a number"));

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"UUID parse error\"");
  }
  private void setupMocks() {
    user = ServiceUserDetailTestUtil.newBuilder().build();
    applicationDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, applicationDetail.getId().toString()));

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(any()))
        .thenReturn(applicationDetail);
  }
}
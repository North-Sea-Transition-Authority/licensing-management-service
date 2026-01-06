package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;

class InvokingUserCanAccessScheduleApplicationInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  @Mock
  private ApplicationAccessService applicationAccessService;
  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private InvokingUserCanAccessScheduleApplicationInterceptorRule invokingUserCanAccessScheduleApplicationInterceptorRule;

  @Test
  void supports() {
    assertThat(invokingUserCanAccessScheduleApplicationInterceptorRule.supports())
        .isEqualTo(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void check_userHasAccess_rulePass() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var applicationId = UUID.randomUUID();
    var orgUnitId = 100;
    var wuaId = 123L;

    mockUserAndApplication(detailId, applicationId, orgUnitId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(applicationId.toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, orgUnitId, wuaId))
        .thenReturn(true);

    var annotation = getAnnotation(
        InvokingUserCanAccessScheduleApplicationInterceptorRuleTest.class.getDeclaredMethod("accessControlledEndpoint"),
        InvokingUserCanAccessScheduleApplication.class
    );

    var interceptorResult = invokingUserCanAccessScheduleApplicationInterceptorRule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_userDoesNotHaveAccess_ruleFail() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var applicationId = UUID.randomUUID();
    var orgUnitId = 100;
    var wuaId = 123L;

    mockUserAndApplication(detailId, applicationId, orgUnitId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(applicationId.toString(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, orgUnitId, wuaId))
        .thenReturn(false);

    var annotation = getAnnotation(
        InvokingUserCanAccessScheduleApplicationInterceptorRuleTest.class.getDeclaredMethod("accessControlledEndpoint"),
        InvokingUserCanAccessScheduleApplication.class
    );

    var interceptorResult = invokingUserCanAccessScheduleApplicationInterceptorRule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  private void mockUserAndApplication(UUID detailId, UUID applicationId, Integer orgUnitId, Long wuaId) {
    var userDetail = ServiceUserDetailTestUtil.newBuilder()
                                              .withWuaId(wuaId)
                                              .build();
    when(userDetailService.getUserDetail()).thenReturn(userDetail);

    var application = new ScheduleWorkProgrammeApplication();
    application.setId(applicationId);

    var applicationDetail = ScheduleWorkProgrammeApplicationTestUtil.builder()
                                                                    .withId(detailId)
                                                                    .withScheduleWorkProgrammeApplication(application)
                                                                    .build();

    applicationDetail.setResponsibleOrganisationUnitId(orgUnitId);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, detailId.toString())
    );

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(detailId))
        .thenReturn(applicationDetail);
  }

  @GetMapping("access-controlled-endpoint")
  @InvokingUserCanAccessScheduleApplication
  public ResponseEntity<String> accessControlledEndpoint() {
    return ResponseEntity.ok("Access granted");
  }
}
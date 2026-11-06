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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

class InvokingUserCanAccessScheduleApplicationInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  @Mock
  private ApplicationAccessService applicationAccessService;
  @Mock
  private UserDetailService userDetailService;
  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  private static final Map<Integer, Integer> ORG_UNIT_TO_GROUP_ID = Map.of(10, 20);

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
    var wuaId = 123L;

    var applicationDetail = mockUserAndApplication(detailId, applicationId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail, ORG_UNIT_TO_GROUP_ID, wuaId))
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
    var wuaId = 123L;

    var applicationDetail = mockUserAndApplication(detailId, applicationId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail, ORG_UNIT_TO_GROUP_ID, wuaId))
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

  @Test
  void check_whenViewOnlyAndUserHasViewAccess_rulePass() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var applicationId = UUID.randomUUID();
    var wuaId = 123L;

    var applicationDetail = mockUserAndApplication(detailId, applicationId, wuaId);
    when(applicationAccessService.userHasViewAccessToApplication(
        applicationDetail, ORG_UNIT_TO_GROUP_ID, wuaId))
        .thenReturn(true);

    var annotation = getAnnotation(
        InvokingUserCanAccessScheduleApplicationInterceptorRuleTest.class.getDeclaredMethod("viewOnlyEndpoint"),
        InvokingUserCanAccessScheduleApplication.class
    );

    var interceptorResult = invokingUserCanAccessScheduleApplicationInterceptorRule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
  }

  @Test
  void check_whenViewOnlyAndUserHasNoViewAccess_ruleFail() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var applicationId = UUID.randomUUID();
    var wuaId = 123L;

    var applicationDetail = mockUserAndApplication(detailId, applicationId, wuaId);
    when(applicationAccessService.userHasViewAccessToApplication(
        applicationDetail, ORG_UNIT_TO_GROUP_ID, wuaId))
        .thenReturn(false);

    var annotation = getAnnotation(
        InvokingUserCanAccessScheduleApplicationInterceptorRuleTest.class.getDeclaredMethod("viewOnlyEndpoint"),
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

  private ScheduleWorkProgrammeApplicationDetail mockUserAndApplication(UUID detailId, UUID applicationId, Long wuaId) {
    var userDetail = ServiceUserDetailTestUtil.newBuilder()
                                              .withWuaId(wuaId)
                                              .build();
    when(userDetailService.getUserDetail()).thenReturn(userDetail);

    var licence = LicenceTestUtil.builder().withId(1).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    var application = new ScheduleWorkProgrammeApplication();
    application.setId(applicationId);
    application.setLicenceSchedule(licenceSchedule);

    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(detailId)
        .withScheduleWorkProgrammeApplication(application)
        .build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, detailId.toString())
    );

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(detailId))
        .thenReturn(applicationDetail);
    when(licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(applicationDetail.getLicence()))
        .thenReturn(ORG_UNIT_TO_GROUP_ID);

    return applicationDetail;
  }

  @GetMapping("view-only-endpoint")
  @InvokingUserCanAccessScheduleApplication(viewOnly = true)
  public ResponseEntity<String> viewOnlyEndpoint() {
    return ResponseEntity.ok("Access granted");
  }

  @GetMapping("access-controlled-endpoint")
  @InvokingUserCanAccessScheduleApplication
  public ResponseEntity<String> accessControlledEndpoint() {
    return ResponseEntity.ok("Access granted");
  }
}
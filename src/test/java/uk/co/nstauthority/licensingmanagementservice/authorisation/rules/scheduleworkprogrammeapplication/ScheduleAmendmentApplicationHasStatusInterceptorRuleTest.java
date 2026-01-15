package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

class ScheduleAmendmentApplicationHasStatusInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @InjectMocks
  private ScheduleAmendmentApplicationHasStatusInterceptorRule rule;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(ScheduleAmendmentApplicationHasStatus.class);
  }

  @Test
  void check_applicationHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    mockApplicationInStatusAsPathVariableEntity(ScheduleWorkProgrammeApplicationStatus.DRAFT);

    var annotation = getAnnotation(
        ScheduleAmendmentApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod("applicationHasStatus_oneStatus", ScheduleWorkProgrammeApplicationDetail.class),
        ScheduleAmendmentApplicationHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_applicationHasStatus_oneStatus_ruleFail() throws NoSuchMethodException {
    mockApplicationInStatusAsPathVariableEntity(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);

    var annotation = getAnnotation(
        ScheduleAmendmentApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod(
            "applicationHasStatus_oneStatus",
            ScheduleWorkProgrammeApplicationDetail.class
        ),
        ScheduleAmendmentApplicationHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"DRAFT", "SUBMITTED"})
  void check_applicationHasStatus_manyStatuses_rulePass(ScheduleWorkProgrammeApplicationStatus status) throws NoSuchMethodException {
    mockApplicationInStatusAsPathVariableEntity(status);

    var annotation = getAnnotation(
        ScheduleAmendmentApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod(
            "applicationHasStatus_manyStatuses",
            ScheduleWorkProgrammeApplicationDetail.class
        ),
        ScheduleAmendmentApplicationHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  private void mockApplicationInStatusAsPathVariableEntity(ScheduleWorkProgrammeApplicationStatus status) {
    var id = UUID.randomUUID();
    var scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder().withId(id).withStatus(status).build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, id.toString())
    );
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void check_applicationHasStatus_noStatuses_internalError() throws NoSuchMethodException {
    var annotation = getAnnotation(
        ScheduleAmendmentApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod("applicationHasStatus_noStatus", ScheduleWorkProgrammeApplicationDetail.class),
        ScheduleAmendmentApplicationHasStatus.class
    );

    assertThatThrownBy(() -> rule.check(
        annotation,
        request,
        response
    )).isInstanceOf(RuntimeException.class)
    .hasMessage("500 INTERNAL_SERVER_ERROR \"No statuses provided to security annotation\"");
  }
  
  @GetMapping("application-has-status-one-status/{applicationId}")
  @ScheduleAmendmentApplicationHasStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
  public ResponseEntity<String> applicationHasStatus_oneStatus(ScheduleWorkProgrammeApplicationDetail application) {
    return ResponseEntity.ok("application has status one status test endpoint");
  }

  @GetMapping("application-has-status-many-statuses/{applicationId}")
  @ScheduleAmendmentApplicationHasStatus({ScheduleWorkProgrammeApplicationStatus.DRAFT, ScheduleWorkProgrammeApplicationStatus.SUBMITTED})
  public ResponseEntity<String> applicationHasStatus_manyStatuses(ScheduleWorkProgrammeApplicationDetail application) {
    return ResponseEntity.ok("application has status many statuses test endpoint");
  }

  @GetMapping("application-has-status-no-status/{applicationId}")
  @ScheduleAmendmentApplicationHasStatus({})
  public ResponseEntity<String> applicationHasStatus_noStatus(ScheduleWorkProgrammeApplicationDetail application) {
    return ResponseEntity.ok("application has status no statuses test endpoint");
  }
}

package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;

class RequestPurposeChoiceMustBeApplicableRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  @InjectMocks
  private RequestPurposeChoiceMustBeApplicableRule requestPurposeChoiceMustBeApplicableRule;

  @Test
  void supports() {
    assertThat(requestPurposeChoiceMustBeApplicableRule.supports())
        .isEqualTo(RequestPurposeChoiceMustBeApplicable.class);
  }

  @Test
  void check_whenAmendableWorkProgrammeActivities_rulePasses() throws NoSuchMethodException {
    var detail = mockApplicationDetail();
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(detail)).thenReturn(true);

    var annotation = getAnnotation(
        RequestPurposeChoiceMustBeApplicableRuleTest.class.getDeclaredMethod("guardedEndpoint"),
        RequestPurposeChoiceMustBeApplicable.class
    );

    var result = requestPurposeChoiceMustBeApplicableRule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
  }

  @Test
  void check_whenNoAmendableWorkProgrammeActivities_ruleFailsForbidden() throws NoSuchMethodException {
    var detail = mockApplicationDetail();
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(detail)).thenReturn(false);

    var annotation = getAnnotation(
        RequestPurposeChoiceMustBeApplicableRuleTest.class.getDeclaredMethod("guardedEndpoint"),
        RequestPurposeChoiceMustBeApplicable.class
    );

    var result = requestPurposeChoiceMustBeApplicableRule.check(annotation, request, response);

    assertThat(result).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  private ScheduleWorkProgrammeApplicationDetail mockApplicationDetail() {
    var detailId = UUID.randomUUID();
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(detailId)
        .build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID, detailId.toString())
    );
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(detailId)).thenReturn(applicationDetail);

    return applicationDetail;
  }

  @GetMapping("guarded-endpoint")
  @RequestPurposeChoiceMustBeApplicable
  public ResponseEntity<String> guardedEndpoint() {
    return ResponseEntity.ok("ok");
  }
}

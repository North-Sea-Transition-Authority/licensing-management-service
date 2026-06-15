package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

class ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @InjectMocks
  private ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRule rule;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(ContinuationApplicationHasWorkProgrammeActivities.class);
  }

  @Test
  void check_whenScheduleHasWorkProgrammeActivities_rulePass() throws NoSuchMethodException {
    var scheduleDetail = mockApplicationAsPathVariableEntity();
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(true);

    var annotation = getAnnotation(
        ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRuleTest.class
            .getDeclaredMethod("hasWorkProgrammeActivitiesEndpoint"),
        ContinuationApplicationHasWorkProgrammeActivities.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenScheduleHasNoWorkProgrammeActivities_ruleFailsForbidden() throws NoSuchMethodException {
    var scheduleDetail = mockApplicationAsPathVariableEntity();
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(false);

    var annotation = getAnnotation(
        ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRuleTest.class
            .getDeclaredMethod("hasWorkProgrammeActivitiesEndpoint"),
        ContinuationApplicationHasWorkProgrammeActivities.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  private LicenceScheduleDetail mockApplicationAsPathVariableEntity() {
    var detailId = UUID.randomUUID();

    var licence = LicenceTestUtil
        .builder()
        .withId(1)
        .build();

    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence));

    var licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .createLicenceContinuationApplicationDetail(licenceScheduleDetail);
    licenceContinuationApplicationDetail.setId(detailId);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, detailId.toString())
    );
    when(licenceContinuationService.getDetailByIdOrThrow(detailId))
        .thenReturn(licenceContinuationApplicationDetail);
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licenceScheduleDetail);

    return licenceScheduleDetail;
  }

  @GetMapping("has-work-programme-activities-endpoint/{licenceContinuationApplicationDetailId}")
  @ContinuationApplicationHasWorkProgrammeActivities
  public ResponseEntity<String> hasWorkProgrammeActivitiesEndpoint() {
    return ResponseEntity.ok("has work programme activities test endpoint");
  }
}

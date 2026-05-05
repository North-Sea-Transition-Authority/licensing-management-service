package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail;

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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

class LicenceScheduleDetailHasStatusInterceptorRuleTest extends AbstractInterceptorRuleTest {
  
  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;
  
  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;
  
  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;
  
  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;
  
  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;
  
  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @InjectMocks
  private LicenceScheduleDetailHasStatusInterceptorRule rule;
  
  private final Licence licence = new Licence();

  private final LicenceSchedule licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
  
  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(LicenceScheduleDetailHasStatus.class);
  }

  @Test
  void check_detailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_DETAIL_ID, detail.getId().toString())
    );
    
    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("detailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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
  void check_detailHasStatus_oneStatus_ruleFail() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.ACTIVE);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_DETAIL_ID, detail.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("detailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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

  @Test
  void check_termLinkedDetailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(detail);
    
    when(licenceScheduleTermService.getTermByIdOrThrow(term.getId())).thenReturn(term);
    
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_TERM_ID, term.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("termDetailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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
  void check_phaseLinkedDetailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(detail);

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phase.getId())).thenReturn(phase);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_PHASE_ID, phase.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("phaseDetailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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
  void check_rateLinkedDetailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(detail);

    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_RATE_ID, rate.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("rateDetailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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
  void check_activityLinkedDetailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setLicenceScheduleDetail(detail);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activity.getId())).thenReturn(activity);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.WORK_PROGRAMME_ACTIVITY_ID, activity.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("activityDetailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
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
  void check_eventLinkedDetailHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus.DRAFT);

    var event = new OtherScheduleEvent();
    event.setId(UUID.randomUUID());
    event.setLicenceScheduleDetail(detail);

    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(event.getId())).thenReturn(event);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.OTHER_SCHEDULE_EVENT_ID, event.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("eventDetailHasStatus_oneStatus"),
        LicenceScheduleDetailHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }
  
  @ParameterizedTest
  @EnumSource(value = LicenceScheduleDetailStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"DRAFT", "ACTIVE"})
  void check_detailHasStatus_manyStatuses_rulePass(LicenceScheduleDetailStatus status) throws NoSuchMethodException {
    var detail = createAndMockScheduleDetailWithStatus(status);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceScheduleDetailHasStatusInterceptorRule.LICENCE_SCHEDULE_DETAIL_ID, detail.getId().toString())
    );

    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod(
            "detailHasStatus_manyStatuses"
        ),
        LicenceScheduleDetailHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }
  
  private LicenceScheduleDetail createAndMockScheduleDetailWithStatus(LicenceScheduleDetailStatus status) {
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withStatus(status)
        .build();

    when(licenceScheduleDetailService.getByIdOrThrow(detail.getId())).thenReturn(detail);
    
    return detail;
  }
  
  @Test
  void check_detailHasStatus_noStatuses_internalError() throws NoSuchMethodException {
    var annotation = getAnnotation(
        LicenceScheduleDetailHasStatusInterceptorRuleTest.class.getDeclaredMethod("detailHasStatus_noStatus"),
        LicenceScheduleDetailHasStatus.class
    );

    assertThatThrownBy(() -> rule.check(
        annotation,
        request,
        response
    )).isInstanceOf(RuntimeException.class)
        .hasMessage("500 INTERNAL_SERVER_ERROR \"No statuses provided to security annotation\"");
  }

  @GetMapping("detail-has-status-one-status/{licenceScheduleDetailId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> detailHasStatus_oneStatus() {
    return ResponseEntity.ok("detail has status one status test endpoint");
  }

  @GetMapping("term-detail-has-status-one-status/{licenceScheduleTermId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> termDetailHasStatus_oneStatus() {
    return ResponseEntity.ok("term linked detail has status one status test endpoint");
  }
  
  @GetMapping("phase-detail-has-status-one-status/{licenceSchedulePhaseId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> phaseDetailHasStatus_oneStatus() {
    return ResponseEntity.ok("phase linked detail has status one status test endpoint");
  }
  
  @GetMapping("rate-detail-has-status-one-status/{licenceScheduleRateId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> rateDetailHasStatus_oneStatus() {
    return ResponseEntity.ok("rate linked detail has status one status test endpoint");
  }

  @GetMapping("activity-detail-has-status-one-status/{workProgrammeActvityId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> activityDetailHasStatus_oneStatus() {
    return ResponseEntity.ok("activity linked detail has status one status test endpoint");
  }

  @GetMapping("event-detail-has-status-one-status/{otherScheduleEventId}")
  @LicenceScheduleDetailHasStatus(LicenceScheduleDetailStatus.DRAFT)
  public ResponseEntity<String> eventDetailHasStatus_oneStatus() {
    return ResponseEntity.ok("event linked detail has status one status test endpoint");
  }
  
  @GetMapping("application-has-status-many-statuses/{licenceScheduleDetailId}")
  @LicenceScheduleDetailHasStatus({LicenceScheduleDetailStatus.DRAFT, LicenceScheduleDetailStatus.ACTIVE})
  public ResponseEntity<String> detailHasStatus_manyStatuses() {
    return ResponseEntity.ok("detail has status many statuses test endpoint");
  }

  @GetMapping("application-has-status-no-status/{licenceScheduleDetailId}")
  @LicenceScheduleDetailHasStatus({})
  public ResponseEntity<String> detailHasStatus_noStatus() {
    return ResponseEntity.ok("detail has status no statuses test endpoint");
  }
}
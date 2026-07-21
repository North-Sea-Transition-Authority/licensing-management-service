package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;

class ContinuationApplicationHasStatusInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @InjectMocks
  private ContinuationApplicationHasStatusInterceptorRule rule;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(ContinuationApplicationHasStatus.class);
  }

  @Test
  void check_applicationHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    mockApplicationInStatusAsPathVariableEntity(ApplicationStatus.DRAFT);

    var annotation = getAnnotation(
        ContinuationApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod("applicationHasStatus_oneStatus", LicenceContinuationApplicationDetail.class),
        ContinuationApplicationHasStatus.class
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
    mockApplicationInStatusAsPathVariableEntity(ApplicationStatus.SUBMITTED);

    var annotation = getAnnotation(
        ContinuationApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod(
            "applicationHasStatus_oneStatus",
            LicenceContinuationApplicationDetail.class
        ),
        ContinuationApplicationHasStatus.class
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
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"DRAFT", "SUBMITTED"})
  void check_applicationHasStatus_manyStatuses_rulePass(ApplicationStatus status) throws NoSuchMethodException {
    mockApplicationInStatusAsPathVariableEntity(status);

    var annotation = getAnnotation(
        ContinuationApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod(
            "applicationHasStatus_manyStatuses",
            LicenceContinuationApplicationDetail.class
        ),
        ContinuationApplicationHasStatus.class
    );

    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  private void mockApplicationInStatusAsPathVariableEntity(ApplicationStatus status) {
    var id = UUID.randomUUID();
    var licence = LicenceTestUtil
        .builder()
        .withId(1)
        .build();

    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence));

    var licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .createLicenceContinuationApplicationDetail(licenceScheduleDetail);

    licenceContinuationApplicationDetail.setStatus(status);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, id.toString())
    );
    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(licenceContinuationApplicationDetail);
  }

  @Test
  void check_applicationHasStatus_noStatuses_internalError() throws NoSuchMethodException {
    var annotation = getAnnotation(
        ContinuationApplicationHasStatusInterceptorRuleTest.class.getDeclaredMethod("applicationHasStatus_noStatus", LicenceContinuationApplicationDetail.class),
        ContinuationApplicationHasStatus.class
    );

    assertThatThrownBy(() -> rule.check(
        annotation,
        request,
        response
    )).isInstanceOf(RuntimeException.class)
        .hasMessage("500 INTERNAL_SERVER_ERROR \"No statuses provided to security annotation\"");
  }

  @GetMapping("application-has-status-one-status/{applicationId}")
  @ContinuationApplicationHasStatus(ApplicationStatus.DRAFT)
  public ResponseEntity<String> applicationHasStatus_oneStatus(LicenceContinuationApplicationDetail application) {
    return ResponseEntity.ok("application has status one status test endpoint");
  }

  @GetMapping("application-has-status-many-statuses/{applicationId}")
  @ContinuationApplicationHasStatus({ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED})
  public ResponseEntity<String> applicationHasStatus_manyStatuses(LicenceContinuationApplicationDetail application) {
    return ResponseEntity.ok("application has status many statuses test endpoint");
  }


  @GetMapping("application-has-status-no-status/{applicationId}")
  @ContinuationApplicationHasStatus({})
  public ResponseEntity<String> applicationHasStatus_noStatus(LicenceContinuationApplicationDetail application) {
    return ResponseEntity.ok("application has status no statuses test endpoint");
  }
}

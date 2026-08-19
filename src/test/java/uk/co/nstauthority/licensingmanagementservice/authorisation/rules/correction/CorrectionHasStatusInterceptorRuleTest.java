package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;

class CorrectionHasStatusInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @InjectMocks
  private CorrectionHasStatusInterceptorRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(CorrectionHasStatus.class);
  }

  @Test
  void check_correctionHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .build();

    when(request.getAttribute("validatedCorrection")).thenReturn(correction);

    var annotation = getAnnotation(
        CorrectionHasStatusInterceptorRuleTest.class.getDeclaredMethod("correctionHasStatus_oneStatus"),
        CorrectionHasStatus.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_correctionHasStatus_oneStatus_ruleFail() throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    when(request.getAttribute("validatedCorrection")).thenReturn(correction);

    var annotation = getAnnotation(
        CorrectionHasStatusInterceptorRuleTest.class.getDeclaredMethod("correctionHasStatus_oneStatus"),
        CorrectionHasStatus.class
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

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"IN_PROGRESS", "COMPLETE"})
  void check_correctionHasStatus_manyStatuses_rulePass(LicenceCorrectionStatus status) throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(status)
        .build();

    when(request.getAttribute("validatedCorrection")).thenReturn(correction);

    var annotation = getAnnotation(
        CorrectionHasStatusInterceptorRuleTest.class.getDeclaredMethod("correctionHasStatus_manyStatuses"),
        CorrectionHasStatus.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_correctionHasStatus_noStatuses_internalError() throws NoSuchMethodException {
    var annotation = getAnnotation(
        CorrectionHasStatusInterceptorRuleTest.class.getDeclaredMethod("correctionHasStatus_noStatus"),
        CorrectionHasStatus.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("500 INTERNAL_SERVER_ERROR \"No statuses provided to security annotation\"");
  }

  @GetMapping("correction-has-status-one-status/{correctionId}")
  @CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
  public ResponseEntity<String> correctionHasStatus_oneStatus() {
    return ResponseEntity.ok("correction has status one status test endpoint");
  }

  @GetMapping("correction-has-status-many-statuses/{correctionId}")
  @CorrectionHasStatus({LicenceCorrectionStatus.IN_PROGRESS, LicenceCorrectionStatus.COMPLETE})
  public ResponseEntity<String> correctionHasStatus_manyStatuses() {
    return ResponseEntity.ok("correction has status many statuses test endpoint");
  }

  @GetMapping("correction-has-status-no-status/{correctionId}")
  @CorrectionHasStatus({})
  public ResponseEntity<String> correctionHasStatus_noStatus() {
    return ResponseEntity.ok("correction has status no statuses test endpoint");
  }
}
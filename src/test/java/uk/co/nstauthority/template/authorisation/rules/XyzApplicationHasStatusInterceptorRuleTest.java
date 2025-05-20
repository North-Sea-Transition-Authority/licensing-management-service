package uk.co.nstauthority.template.authorisation.rules;

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
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.template.authorisation.SecurityRuleResult;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationService;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;

class XyzApplicationHasStatusInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private XyzApplicationService xyzApplicationService;

  @InjectMocks
  private XyzApplicationHasStatusInterceptorRule rule;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class);
  }

  @Test
  void check_applicationHasStatus_oneStatus_rulePass() throws NoSuchMethodException {
    mockXyzApplicationInStatusAsPathVariableEntity(XyzApplicationStatus.APPROVED);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("xyzApplicationHasStatus_oneStatus", XyzApplication.class),
        XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class
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
  @EnumSource(value = XyzApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "APPROVED")
  void check_applicationHasStatus_oneStatus_ruleFail() throws NoSuchMethodException {
    mockXyzApplicationInStatusAsPathVariableEntity(XyzApplicationStatus.DRAFT);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("xyzApplicationHasStatus_oneStatus", XyzApplication.class),
        XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class
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
  @EnumSource(value = XyzApplicationStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"APPROVED", "SUBMITTED"})
  void check_applicationHasStatus_manyStatuses_rulePass(XyzApplicationStatus status) throws NoSuchMethodException {
    mockXyzApplicationInStatusAsPathVariableEntity(status);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("xyzApplicationHasStatus_manyStatuses", XyzApplication.class),
        XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class
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
  @EnumSource(value = XyzApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"APPROVED", "SUBMITTED"})
  void check_applicationHasStatus_manyStatuses_ruleFail(XyzApplicationStatus status) throws NoSuchMethodException {
    mockXyzApplicationInStatusAsPathVariableEntity(status);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("xyzApplicationHasStatus_manyStatuses", XyzApplication.class),
        XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class
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

  private void mockXyzApplicationInStatusAsPathVariableEntity(XyzApplicationStatus status) {
    var application = new XyzApplication(
        UUID.randomUUID(),
        null,
        null,
        status
    );

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of("applicationId", application.getId().toString())
    );
    when(xyzApplicationService.getXyzApplicationById(application.getId())).thenReturn(application);
  }

  @Test
  void check_applicationHasStatus_noStatuses_internalError() throws NoSuchMethodException {
    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("xyzApplicationHasStatus_noStatus", XyzApplication.class),
        XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus.class
    );

    assertThatThrownBy(() -> rule.check(
        annotation,
        request,
        response
    )).isInstanceOf(RuntimeException.class)
    .hasMessage("500 INTERNAL_SERVER_ERROR \"No statuses provided to security annotation\"");
  }
}

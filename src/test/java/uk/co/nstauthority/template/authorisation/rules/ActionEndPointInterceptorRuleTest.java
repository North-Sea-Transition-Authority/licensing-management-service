package uk.co.nstauthority.template.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionItem.PROGRESS_APPLICATION;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.authentication.UserDetailService;
import uk.co.nstauthority.template.authorisation.SecurityRuleResult;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationService;
import uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionService;

@ExtendWith(MockitoExtension.class)
class ActionEndPointInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private XyzApplicationService xyzApplicationService;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private CaseProcessingActionService caseProcessingActionService;

  @InjectMocks
  private ActionEndPointInterceptorRule rule;

  private ServiceUserDetail user;

  private XyzApplication xyzApplication;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(ActionEndPointInterceptorRule.ActionEndPoint.class);
  }

  @Test
  void check_matchesEndpoint_thenTrue() throws NoSuchMethodException {
    setupMocks();
    when(caseProcessingActionService.getAvailableUserActionItems(xyzApplication, user))
        .thenReturn(Set.of(PROGRESS_APPLICATION));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getAction"),
        ActionEndPointInterceptorRule.ActionEndPoint.class
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
  void check_doesNotMatchAction_thenFalse() throws NoSuchMethodException {
    setupMocks();
    when(caseProcessingActionService.getAvailableUserActionItems(xyzApplication, user)).thenReturn(Set.of());

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getAction"),
        ActionEndPointInterceptorRule.ActionEndPoint.class
    );
    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus,
        SecurityRuleResult::failureMessage
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN,
        "Attempted to use action item(s) %s on xyzApplication %s".formatted(PROGRESS_APPLICATION.name(), xyzApplication.getId())
    );
  }

  @Test
  void check_missingPathVariable_throwBadRequest() throws NoSuchMethodException {
    xyzApplication = new XyzApplication();
    xyzApplication.setId(UUID.randomUUID());
    user = ServiceUserDetailTestUtil.newBuilder().build();

    Map<String, String> uriTemplateVariables = new HashMap<>();
    uriTemplateVariables.put(XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME, null);

    when(userDetailService.getUserDetail())
        .thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(uriTemplateVariables);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getAction"),
        ActionEndPointInterceptorRule.ActionEndPoint.class
    );
    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"Path variable applicationId not found in request\"");
  }

  @Test
  void check_malformedUUID_throwNotFound() throws NoSuchMethodException {
    xyzApplication = new XyzApplication();
    xyzApplication.setId(UUID.randomUUID());
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail())
        .thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME, "not a uuid"));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getAction"),
        ActionEndPointInterceptorRule.ActionEndPoint.class
    );
    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"UUID parse error\"");
  }

  private void setupMocks() {
    xyzApplication = new XyzApplication();
    xyzApplication.setId(UUID.randomUUID());
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail())
        .thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME, xyzApplication.getId().toString()));
    when(xyzApplicationService.getXyzApplicationById(xyzApplication.getId()))
        .thenReturn(xyzApplication);
  }
}

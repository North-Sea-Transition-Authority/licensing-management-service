package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem.CREATE_LICENCE_SCHEDULE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;

@ExtendWith(MockitoExtension.class)
class LicenceActionEndPointInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private LicenceActionService licenceActionService;

  @InjectMocks
  private LicenceActionEndPointInterceptorRule rule;

  private ServiceUserDetail user;

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(LicenceActionEndPointInterceptorRule.ActionEndPoint.class);
  }

  @Test
  void check_matchesEndpoint_thenTrue() throws NoSuchMethodException {
    setupMocks();
    var licence = LicenceTestUtil.builder().withId(1).build();

    when(licenceActionService.getAvailableUserActionItems(licence, user))
        .thenReturn(List.of(CREATE_LICENCE_SCHEDULE.toActionItemView(licence)));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getLicenceAction"),
        LicenceActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_doesNotMatchAction_thenFalse() throws NoSuchMethodException {
    setupMocks();
    var licence = LicenceTestUtil.builder().withId(1).build();

    when(licenceActionService.getAvailableUserActionItems(licence, user))
        .thenReturn(List.of());

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getLicenceAction"),
        LicenceActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus,
        SecurityRuleResult::failureMessage
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN,
        "Attempted to use action item(s) %s on create licence schedule %s"
            .formatted(CREATE_LICENCE_SCHEDULE.getDisplayName(), licence.getId())
    );
  }

  @Test
  void check_missingPathVariable_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    Map<String, String> uriTemplateVariables = new HashMap<>();
    uriTemplateVariables.put(LicenceArgumentResolver.LICENCE_ID, null);

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(uriTemplateVariables);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getLicenceAction"),
        LicenceActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"Path variable licenceId not found in request\"");
  }

  @Test
  void check_malformedInteger_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(LicenceArgumentResolver.LICENCE_ID, "not a number"));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("getLicenceAction"),
        LicenceActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"Integer parse error\"");
  }

  private void setupMocks() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(LicenceArgumentResolver.LICENCE_ID, String.valueOf(licence.getId())));

    when(licenceService.findLicenceByIdOrThrow(licence.getId()))
        .thenReturn(licence);
  }
}
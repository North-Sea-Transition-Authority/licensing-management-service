package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationActionEndPointInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private LicenceContinuationActionService licenceContinuationActionService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @InjectMocks
  private LicenceContinuationActionEndPointInterceptorRule rule;

  private ServiceUserDetail user;
  private LicenceContinuationApplicationDetail applicationDetail;

  private interface TestEndpoints {
    @LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint(LicenceContinuationActionItem.CONFIRM_CONTINUATION)
    void getAction();
  }

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class);
  }

  @Test
  void check_matchesEndpoint_thenTrue() throws NoSuchMethodException {
    setupMocks();

    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of(LicenceContinuationActionItem.CONFIRM_CONTINUATION.toActionItemView(applicationDetail)));

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_doesNotMatchAction_thenFalse() throws NoSuchMethodException {
    setupMocks();

    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of());

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus,
        SecurityRuleResult::failureMessage
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN,
        "Attempted to use action item(s) %s on continuation application detail %s"
            .formatted(LicenceContinuationActionItem.CONFIRM_CONTINUATION.getDisplayName(), applicationDetail.getId())
    );
  }

  @Test
  void check_missingPathVariable_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    Map<String, String> uriTemplateVariables = new HashMap<>();
    uriTemplateVariables.put(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, null);

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(uriTemplateVariables);

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"Path variable licenceContinuationApplicationDetailId not found in request\"");
  }

  @Test
  void check_malformedInteger_throwBadRequest() throws NoSuchMethodException {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, "not a number"));

    var annotation = getAnnotation(
        TestEndpoints.class.getDeclaredMethod("getAction"),
        LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessage("400 BAD_REQUEST \"UUID parse error\"");
  }

  private void setupMocks() {
    user = ServiceUserDetailTestUtil.newBuilder().build();
    applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());

    when(userDetailService.getUserDetail()).thenReturn(user);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, applicationDetail.getId().toString()));

    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(applicationDetail);
  }
}
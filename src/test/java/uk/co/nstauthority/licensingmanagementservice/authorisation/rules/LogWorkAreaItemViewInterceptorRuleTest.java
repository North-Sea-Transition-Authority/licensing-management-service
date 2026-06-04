package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@ExtendWith(MockitoExtension.class)
class LogWorkAreaItemViewInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private WorkAreaItemViewService workAreaItemViewService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private LogWorkAreaItemViewInterceptorRule rule;

  private final ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(42L)
      .build();

  @Test
  void supports_returnsAnnotationClass() {
    assertThat(rule.supports()).isEqualTo(LogWorkAreaItemView.class);
  }

  @Test
  void check_whenDisabled_skipsLoggingAndReturnsContinue() throws NoSuchMethodException {
    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("logWorkAreaItemDisabled"),
        LogWorkAreaItemView.class
    );

    var result = rule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
    verify(workAreaItemViewService, never()).hasUserViewedItem(any());
    verify(workAreaItemViewService, never()).logWorkAreaItemView(any());
  }

  @Test
  void check_whenItemNotYetViewed_logsView() throws NoSuchMethodException {
    var itemId = UUID.randomUUID();
    stubRequestPathVariable(itemId);
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(workAreaItemViewService.hasUserViewedItem(any(WorkAreaItemView.class))).thenReturn(false);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("logWorkAreaItem"),
        LogWorkAreaItemView.class
    );

    var result = rule.check(annotation, request, response);

    var captor = ArgumentCaptor.forClass(WorkAreaItemView.class);
    verify(workAreaItemViewService).logWorkAreaItemView(captor.capture());

    var logged = captor.getValue();
    assertThat(logged.getItemId()).isEqualTo(itemId);
    assertThat(logged.getItemType()).isEqualTo(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);
    assertThat(logged.getUserId()).isEqualTo(42L);
    assertThat(result.hasRulePassed()).isTrue();
  }

  @Test
  void check_whenItemAlreadyViewed_doesNotLogAgain() throws NoSuchMethodException {
    stubRequestPathVariable(UUID.randomUUID());
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(workAreaItemViewService.hasUserViewedItem(any(WorkAreaItemView.class))).thenReturn(true);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("logWorkAreaItem"),
        LogWorkAreaItemView.class
    );

    var result = rule.check(annotation, request, response);

    verify(workAreaItemViewService, never()).logWorkAreaItemView(any());
    assertThat(result.hasRulePassed()).isTrue();
  }

  private void stubRequestPathVariable(UUID itemId) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("itemId", itemId.toString()));
  }
}

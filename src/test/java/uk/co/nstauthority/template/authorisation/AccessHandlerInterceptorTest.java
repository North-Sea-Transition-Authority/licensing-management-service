package uk.co.nstauthority.template.authorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.template.authorisation.rules.AccessInterceptorRule;

@ExtendWith(MockitoExtension.class)
class AccessHandlerInterceptorTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HandlerMethod handlerMethod;

  @Mock
  private AccessInterceptorRule securityRule1;

  private AccessInterceptorRule trueOrExceptionSecurityRule;

  private AccessInterceptorRule trueOrFalseSecurityRule;

  private AccessHandlerInterceptor handlerInterceptor;

  @BeforeEach
  void setUp() {
    trueOrExceptionSecurityRule = new ExampleTrueOrFailWithStatusAccessSecurityRule();
    trueOrFalseSecurityRule = new ExampleTrueOrFailWithoutStatusAccessSecurityRule();
  }

  @Test
  void preHandle_trueController_returnsTrue() throws NoSuchMethodException {
    handlerInterceptor = new AccessHandlerInterceptor(List.of(securityRule1, trueOrExceptionSecurityRule, trueOrFalseSecurityRule));

    var controller = TestControllers.TrueController.class;
    var method = controller.getDeclaredMethod("get", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var interceptorResult = handlerInterceptor.preHandle(request, response, handlerMethod);
    assertThat(interceptorResult).isTrue();

    verify(securityRule1).supports();
  }

  @Test
  void preHandle_falseControllerAndTrueOrExceptionSecurityRule_throwsException() throws NoSuchMethodException {
    handlerInterceptor = new AccessHandlerInterceptor(List.of(trueOrExceptionSecurityRule));

    var controller = TestControllers.FalseController.class;
    var method = controller.getDeclaredMethod("get", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    assertThatThrownBy(() -> handlerInterceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> ((ResponseStatusException) e).getStatusCode().is4xxClientError());
  }


  @Test
  void preHandle_falseControllerAndTrueOrFalseSecurityRule_throwsException() throws NoSuchMethodException {
    handlerInterceptor = new AccessHandlerInterceptor(List.of(trueOrFalseSecurityRule));

    var controller = TestControllers.FalseController.class;
    var method = controller.getDeclaredMethod("get", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    assertThatThrownBy(() -> handlerInterceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(RuntimeException.class)
        .message()
        .isEqualTo("securityRuleResult has failed without failure status");
  }
}

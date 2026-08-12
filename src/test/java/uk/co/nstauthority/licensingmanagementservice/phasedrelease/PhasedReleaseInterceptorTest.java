package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

class PhasedReleaseInterceptorTest {

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private PhasedReleaseInterceptor interceptorWithProfiles(String... activeProfiles) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(activeProfiles);
    return new PhasedReleaseInterceptor(new FeatureFlagService(environment));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private HandlerMethod handlerFor(Class<?> controllerType) {
    var handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getBeanType()).thenReturn((Class) controllerType);
    return handlerMethod;
  }

  @Test
  void preHandle_nonHandlerMethod_isAllowed() {
    assertThat(interceptorWithProfiles().preHandle(request, response, new ResourceHttpRequestHandler())).isTrue();
  }

  @Test
  void preHandle_initialController_alwaysAllowed() {
    assertThat(interceptorWithProfiles().preHandle(request, response, handlerFor(WorkAreaController.class))).isTrue();
  }

  @Test
  void preHandle_lms1Controller_blockedWhenPhaseOff() {
    var interceptor = interceptorWithProfiles();
    var handler = handlerFor(LicenceSearchController.class);
    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void preHandle_lms1Controller_allowedWhenPhaseOn() {
    assertThat(interceptorWithProfiles("enable-lms1")
        .preHandle(request, response, handlerFor(LicenceSearchController.class))).isTrue();
  }

  @Test
  void preHandle_lms2Controller_blockedUnderLms1Only() {
    var interceptor = interceptorWithProfiles("enable-lms1");
    var handler = handlerFor(LicenceCorrectionController.class);
    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void preHandle_lms2Controller_allowedWhenBothPhasesOn() {
    assertThat(interceptorWithProfiles("enable-lms1", "enable-lms2")
        .preHandle(request, response, handlerFor(LicenceCorrectionController.class))).isTrue();
  }

  @Test
  void preHandle_unclassifiedController_isBlocked() {
    var interceptor = interceptorWithProfiles("enable-lms1", "enable-lms2");
    var handler = handlerFor(String.class);
    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(ResponseStatusException.class);
  }
}

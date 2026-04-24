package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ExtendWith(MockitoExtension.class)
class DefaultErrorControllerTest {

  @Mock
  private ErrorService errorService;

  @Mock
  private HttpServletRequest request;

  private DefaultErrorController controller;

  @BeforeEach
  void setup() {
    controller = new DefaultErrorController(errorService);
  }

  @SecurityTest
  void handleError_whenStatusIsNotFound_returnsNotFoundView() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(SC_NOT_FOUND);

    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/notFound");
  }

  @Test
  void handleError_whenStatusIsMethodNotAllowed_returnsNotFoundView() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(SC_METHOD_NOT_ALLOWED);

    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/notFound");
  }

  @Test
  void handleError_whenStatusIsForbidden_returnsUnauthorisedView() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(SC_FORBIDDEN);

    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/unauthorised");
  }

  @Test
  void handleError_whenStatusIsUnauthorized_returnsUnauthorisedView() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(SC_UNAUTHORIZED);

    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/unauthorised");
  }

  @Test
  void handleError_whenStatusIsInternalServerError_returnsDefaultView() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);

    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/default");
  }

  @Test
  void handleError_whenNoStatusCode_returnsDefaultView() {
    var result = controller.handleError(request);

    assertThat(result.getViewName()).isEqualTo("lms/error/default");
  }

  @Test
  void handleError_whenStatusIs4xx_nullThrowablePassedToErrorService() {
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(SC_FORBIDDEN);

    controller.handleError(request);

    verify(errorService).addErrorAttributesToModel(any(ModelAndView.class), isNull(), eq(request));
  }

  @Test
  void handleError_whenStatusIs5xxWithDispatcherException_dispatcherExceptionPassedToErrorService() {
    var exception = new RuntimeException("dispatcher error");
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
    when(request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE)).thenReturn(exception);

    controller.handleError(request);

    verify(errorService).addErrorAttributesToModel(any(ModelAndView.class), eq(exception), eq(request));
  }

  @Test
  void handleError_whenStatusIs5xxWithServletExceptionOnly_servletExceptionPassedToErrorService() {
    var exception = new RuntimeException("servlet error");
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
    when(request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE)).thenReturn(null);
    when(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE)).thenReturn(exception);

    controller.handleError(request);

    verify(errorService).addErrorAttributesToModel(any(ModelAndView.class), eq(exception), eq(request));
  }

  @Test
  void handleError_whenStatusIs5xxWithBothExceptions_dispatcherExceptionTakesPriority() {
    var dispatcherException = new RuntimeException("dispatcher error");
    var servletException = new RuntimeException("servlet error");
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
    when(request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE)).thenReturn(dispatcherException);
    when(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE)).thenReturn(servletException);

    controller.handleError(request);

    verify(errorService).addErrorAttributesToModel(any(ModelAndView.class), eq(dispatcherException), eq(request));
  }
}

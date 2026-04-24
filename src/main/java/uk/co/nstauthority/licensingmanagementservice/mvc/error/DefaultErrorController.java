package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

@Controller
public class DefaultErrorController implements ErrorController {

  private final ErrorService errorService;

  DefaultErrorController(ErrorService errorService) {
    this.errorService = errorService;
  }

  /**
   * Handles framework-level errors (404s, authorisation failures, filter exceptions) for browser clients. Errors thrown
   * by app code (controller methods and below) are handled in DefaultExceptionResolver.
   */
  @GetMapping("error")
  public ModelAndView handleError(HttpServletRequest request) {
    int statusCode = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
        .map(Integer.class::cast)
        .orElse(0);

    var modelAndView = getModelAndViewForStatus(statusCode);

    var dispatcherException = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
    var servletException = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
    var throwable = statusCode >= 500
        ? (dispatcherException != null ? dispatcherException : servletException)
        : null;

    errorService.addErrorAttributesToModel(modelAndView, throwable, request);

    return modelAndView;
  }

  private ModelAndView getModelAndViewForStatus(int statusCode) {
    return switch (statusCode) {
      case SC_NOT_FOUND, SC_METHOD_NOT_ALLOWED -> new ModelAndView("lms/error/notFound");
      case SC_FORBIDDEN, SC_UNAUTHORIZED -> new ModelAndView("lms/error/unauthorised");
      default -> new ModelAndView("lms/error/default");
    };
  }

}

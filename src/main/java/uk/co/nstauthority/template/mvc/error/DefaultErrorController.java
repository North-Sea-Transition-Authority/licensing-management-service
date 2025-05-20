package uk.co.nstauthority.template.mvc.error;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.apache.commons.lang3.ObjectUtils;
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
    var modelAndView = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
        .map(Integer.class::cast)
        .map(this::getModelAndViewForStatus)
        .orElse(new ModelAndView(""));

    var dispatcherException = request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
    var servletException = request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
    var throwable = (Throwable) ObjectUtils.defaultIfNull(dispatcherException, servletException);

    errorService.addErrorAttributesToModel(modelAndView, throwable, request);

    return modelAndView;
  }

  private ModelAndView getModelAndViewForStatus(int statusCode) {
    return switch (statusCode) {
      case SC_NOT_FOUND, SC_METHOD_NOT_ALLOWED -> new ModelAndView("xyz/error/notFound");
      case SC_FORBIDDEN, SC_UNAUTHORIZED -> new ModelAndView("xyz/error/unauthorised");
      default -> new ModelAndView("xyz/error/default");
    };
  }

}

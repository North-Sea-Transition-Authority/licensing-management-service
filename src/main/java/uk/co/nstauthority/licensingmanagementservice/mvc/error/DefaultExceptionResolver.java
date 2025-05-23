package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import uk.co.nstauthority.licensingmanagementservice.topnavigation.TopNavigationService;

@Component
class DefaultExceptionResolver extends SimpleMappingExceptionResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionResolver.class);

  private final ErrorService errorService;
  private final TopNavigationService topNavigationService;

  DefaultExceptionResolver(
      ErrorService errorService,
      TopNavigationService topNavigationService
  ) {
    this.errorService = errorService;
    this.topNavigationService = topNavigationService;
    setDefaultErrorView("lms/error/default");
    setDefaultStatusCode(SC_INTERNAL_SERVER_ERROR);
  }

  @Override
  protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
    if (ex instanceof ClientAbortException) {
      //See https://mtyurt.net/post/spring-how-to-handle-ioexception-broken-pipe.html
      //ClientAbortException indicates a broken pipe/network error. Return null, so it can be handled by the servlet,
      //otherwise Spring attempts to write to the broken response.
      LOGGER.trace("Suppressed ClientAbortException");
      return null;
    }

    var modelAndView = super.getModelAndView(viewName, ex);
    errorService.addErrorAttributesToModel(modelAndView, ex, request);
    addTopNavigationItemModelAttributes(modelAndView, request);

    return modelAndView;
  }

  private void addTopNavigationItemModelAttributes(
      ModelAndView model,
      HttpServletRequest request
  ) {
    model.addObject("navigationItems", topNavigationService.getTopNavigationItems());
    model.addObject("currentEndPoint", request.getRequestURI());
  }
}

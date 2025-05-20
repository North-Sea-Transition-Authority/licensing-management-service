package uk.co.nstauthority.template.mvc.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.configuration.ErrorConfigurationProperties;
import uk.co.nstauthority.template.mvc.ControllerAdviceService;


@Service
public class ErrorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorService.class);
  protected static final String SAFE_CHARACTERS = "BCDFGHJKMPQRTVWXY346789";

  private final ErrorConfigurationProperties errorConfigurationProperties;
  private final ControllerAdviceService controllerAdviceService;

  ErrorService(ErrorConfigurationProperties errorConfigurationProperties,
               ControllerAdviceService controllerAdviceService) {
    this.errorConfigurationProperties = errorConfigurationProperties;
    this.controllerAdviceService = controllerAdviceService;
  }

  public ModelAndView addErrorAttributesToModel(ModelAndView modelAndView, Throwable throwable, HttpServletRequest request) {
    controllerAdviceService.addBrandingModelAttributes(modelAndView);
    controllerAdviceService.addCommonUrlModelAttributes(modelAndView);
    controllerAdviceService.addFooterLinkModelAttributes(modelAndView);

    if (Objects.isNull(throwable)) {
      return modelAndView;
    }

    addErrorReference(modelAndView, throwable);
    addStackTraceToModel(modelAndView, throwable);
    return modelAndView;
  }

  private String generateErrorReference() {
    return RandomStringUtils.random(9, SAFE_CHARACTERS.toUpperCase());
  }

  private void addStackTraceToModel(ModelAndView modelAndView, Throwable throwable) {
    if (!errorConfigurationProperties.includeStacktrace() || throwable == null) {
      return;
    }

    modelAndView.addObject("stackTrace", ExceptionUtils.getStackTrace(throwable));
  }

  private void addErrorReference(ModelAndView modelAndView, Throwable throwable) {
    if (throwable == null) {
      return;
    }

    var errorReference = generateErrorReference();
    modelAndView.addObject("errorRef", errorReference);
    LOGGER.error("Caught unhandled exception (errorRef {})", errorReference, throwable);
  }

}

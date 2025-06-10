package uk.co.nstauthority.licensingmanagementservice.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.co.nstauthority.licensingmanagementservice.configuration.AnalyticsConfiguration;
import uk.co.nstauthority.licensingmanagementservice.topnavigation.TopNavigationService;

@ControllerAdvice
public class DefaultPageControllerAdvice {

  private final ControllerAdviceService controllerAdviceService;
  private final TopNavigationService topNavigationService;
  private final HttpServletRequest request;
  private final AnalyticsConfiguration analyticsConfiguration;

  @Autowired
  DefaultPageControllerAdvice(
      ControllerAdviceService controllerAdviceService,
      TopNavigationService topNavigationService,
      HttpServletRequest request,
      AnalyticsConfiguration analyticsConfiguration) {
    this.controllerAdviceService = controllerAdviceService;
    this.topNavigationService = topNavigationService;
    this.request = request;
    this.analyticsConfiguration = analyticsConfiguration;
  }

  @ModelAttribute
  void addDefaultModelAttributes(Model model) {
    controllerAdviceService.addBrandingModelAttributes(model);
    controllerAdviceService.addCommonUrlModelAttributes(model);
    controllerAdviceService.addUserModelAttributes(model);
    controllerAdviceService.addFooterLinkModelAttributes(model);
    addTopNavigationItemModelAttributes(model, request);
    addAnalytics(model);
  }

  @InitBinder
  void initBinder(WebDataBinder binder) {
    // Trim whitespace from form fields
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  private void addTopNavigationItemModelAttributes(Model model, HttpServletRequest request) {
    model.addAttribute("navigationItems", topNavigationService.getTopNavigationItems());
    model.addAttribute("currentEndPoint", request.getRequestURI());
  }

  private void addAnalytics(Model model) {
    model.addAttribute("analytics", analyticsConfiguration.getAnalyticsConfigurationProperties());
  }
}

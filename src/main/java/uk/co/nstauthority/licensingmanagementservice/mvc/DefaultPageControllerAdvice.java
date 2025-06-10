package uk.co.nstauthority.licensingmanagementservice.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.co.nstauthority.licensingmanagementservice.topnavigation.TopNavigationService;

@ControllerAdvice
public class DefaultPageControllerAdvice {

  private final ControllerAdviceService controllerAdviceService;
  private final TopNavigationService topNavigationService;
  private final HttpServletRequest request;

  @Autowired
  DefaultPageControllerAdvice(
      ControllerAdviceService controllerAdviceService,
      TopNavigationService topNavigationService,
      HttpServletRequest request) {
    this.controllerAdviceService = controllerAdviceService;
    this.topNavigationService = topNavigationService;
    this.request = request;
  }

  @ModelAttribute
  void addDefaultModelAttributes(Model model) {
    controllerAdviceService.addBrandingModelAttributes(model);
    controllerAdviceService.addCommonUrlModelAttributes(model);
    controllerAdviceService.addUserModelAttributes(model);
    controllerAdviceService.addFooterLinkModelAttributes(model);
    addTopNavigationItemModelAttributes(model, request);
    controllerAdviceService.addAnalytics(model);
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
}

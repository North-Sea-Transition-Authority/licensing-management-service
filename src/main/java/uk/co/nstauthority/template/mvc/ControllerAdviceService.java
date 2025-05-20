package uk.co.nstauthority.template.mvc;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.authentication.InvalidAuthenticationException;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.authentication.UserDetailService;
import uk.co.nstauthority.template.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.template.branding.ServiceConfigurationProperties;
import uk.co.nstauthority.template.fds.footer.FooterController;
import uk.co.nstauthority.template.workarea.WorkAreaController;

@Service
public class ControllerAdviceService {

  private final CustomerConfigurationProperties customerConfigurationProperties;
  private final ServiceConfigurationProperties serviceConfigurationProperties;
  private final UserDetailService userDetailService;

  public ControllerAdviceService(CustomerConfigurationProperties customerConfigurationProperties,
                                 ServiceConfigurationProperties serviceConfigurationProperties,
                                 UserDetailService userDetailService) {
    this.customerConfigurationProperties = customerConfigurationProperties;
    this.serviceConfigurationProperties = serviceConfigurationProperties;
    this.userDetailService = userDetailService;
  }

  public void addBrandingModelAttributes(Object model) {
    var attributeConsumer = getAttributeConsumer(model);
    attributeConsumer.accept("serviceBranding", serviceConfigurationProperties);
    attributeConsumer.accept("customerBranding", customerConfigurationProperties);
  }

  public void addCommonUrlModelAttributes(Object model) {
    getAttributeConsumer(model).accept("serviceHomeUrl",
        ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }

  public void addFooterLinkModelAttributes(Object model) {
    getAttributeConsumer(model).accept("accessibilityStatementUrl",
        ReverseRouter.route(on(FooterController.class).accessibilityStatement()));
    getAttributeConsumer(model).accept("privacyUrl",
        "#"); // TODO xyz - privacy policy link, e.g. https://www.nstauthority.co.uk/footer/privacy-statement/ for NSTA
    getAttributeConsumer(model).accept("cookiePolicyUrl",
        ReverseRouter.route(on(FooterController.class).cookies()));
    getAttributeConsumer(model).accept("contactPageUrl",
        ReverseRouter.route(on(FooterController.class).contact()));
  }

  public void addUserModelAttributes(Object model) {
    try {
      ServiceUserDetail user = userDetailService.getUserDetail();
      getAttributeConsumer(model).accept("loggedInUser", user);
    } catch (InvalidAuthenticationException exception) {
      // public endpoints may not have a user
    }
  }

  private BiConsumer<String, Object> getAttributeConsumer(Object object) {
    if (object instanceof ModelAndView modelAndView) {
      return modelAndView::addObject;
    }

    if (object instanceof Model model) {
      return model::addAttribute;
    }

    throw new IllegalArgumentException(
        "Expected Model or ModelAndView but got %s".formatted(object.getClass().getSimpleName())
    );
  }
}

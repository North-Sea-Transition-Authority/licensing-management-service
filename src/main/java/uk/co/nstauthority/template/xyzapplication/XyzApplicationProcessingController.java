package uk.co.nstauthority.template.xyzapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.workarea.WorkAreaController;

@Controller
@RequestMapping("/application/{applicationId}")
public class XyzApplicationProcessingController {

  private final XyzApplicationContextService applicationContextService;
  private final XyzApplicationSummarySectionService xyzApplicationSummarySectionService;

  public XyzApplicationProcessingController(
      XyzApplicationContextService applicationContextService,
      XyzApplicationSummarySectionService xyzApplicationSummarySectionService
  ) {
    this.applicationContextService = applicationContextService;
    this.xyzApplicationSummarySectionService = xyzApplicationSummarySectionService;
  }

  @GetMapping("/application-processing")
  public ModelAndView getApplicationProcessing(
      XyzApplication xyzApplication,
      ServiceUserDetail userDetail
  ) {
    return getModelAndView(xyzApplication, userDetail);
  }

  private ModelAndView getModelAndView(
      XyzApplication xyzApplication,
      ServiceUserDetail userDetail
  ) {
    return new ModelAndView("xyz/application/xyzApplicationProcessing")
        .addObject("cancelUrl", ReverseRouter.route(on(WorkAreaController.class)
            .getWorkArea(null, null)))
        .addObject("application", xyzApplication)
        .addObject("applicationContext", applicationContextService.getContextForApplication(xyzApplication))
        .addObject("summarySections", xyzApplicationSummarySectionService.getSummarySections(xyzApplication, userDetail))
        .addObject("accordionId", xyzApplication.getId());
  }
}

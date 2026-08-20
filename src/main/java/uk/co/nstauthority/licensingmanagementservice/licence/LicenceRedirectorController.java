package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/redirect-to-licence/{licenceReference}")
public class LicenceRedirectorController {

  private final LicenceService licenceService;

  public LicenceRedirectorController(
      LicenceService licenceService
  ) {
    this.licenceService = licenceService;
  }

  @GetMapping("/schedule")
  public ModelAndView redirectToScheduleTimeline(@PathVariable String licenceReference) {
    var licence = licenceService.findByLicenceReferenceOrThrow(licenceReference);
    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licence.getId(), null, null, null));
  }

  @GetMapping("/work-programme")
  public ModelAndView redirectToWorkProgrammesTimeline(@PathVariable String licenceReference) {
    var licence = licenceService.findByLicenceReferenceOrThrow(licenceReference);
    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderWorkProgrammesOnlyTimeline(licence.getId(), null));
  }
}

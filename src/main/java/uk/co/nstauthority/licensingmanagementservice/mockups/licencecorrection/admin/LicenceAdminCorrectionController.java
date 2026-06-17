package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.admin;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/mockups/licence-admin-correction")
@Profile("mockups")
public class LicenceAdminCorrectionController {

  @GetMapping
  ModelAndView renderPlayback() {
    return new ModelAndView("lms/mockups/licencecorrection/changeStatePlayback")
        .addObject("editUrl", ReverseRouter.route(on(this.getClass()).renderLicenceAdminCorrection()))
        .addObject("deleteUrl", ReverseRouter.route(on(this.getClass()).renderDelete()));
  }

  @GetMapping("/edit")
  ModelAndView renderLicenceAdminCorrection() {
    return new ModelAndView("lms/mockups/licencecorrection/licenceAdminCorrection")
        .addObject("form", new LicenceAdminCorrectionForm())
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback()));
  }

  @GetMapping("/delete")
  ModelAndView renderDelete() {
    return new ModelAndView("lms/mockups/licencecorrection/deleteAdminCorrection")
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback()));

  }
}

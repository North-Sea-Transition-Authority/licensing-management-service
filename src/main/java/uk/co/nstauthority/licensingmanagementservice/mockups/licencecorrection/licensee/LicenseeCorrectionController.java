package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.licensee;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/mockups/licensee-correction")
@Profile("mockups")
public class LicenseeCorrectionController {

  private static final List<OrganisationUnitJson> JOINING_LICENSEES = List.of(
      new OrganisationUnitJson(1021007, "TOTAL E&P UK LIMITED"),
      new OrganisationUnitJson(140141, "SHELL U.K. LIMITED")
  );

  private static final List<OrganisationUnitJson> WITHDRAWING_LICENSEES = List.of(
      new OrganisationUnitJson(1013001, "BP EXPLORATION (ALPHA) LIMITED")
  );

  @GetMapping
  ModelAndView renderPlayback() {
    return new ModelAndView("lms/mockups/licencecorrection/licensee/changeStatePlayback")
        .addObject("addUrl", ReverseRouter.route(on(this.getClass()).renderAdd()))
        .addObject("correctUrl", ReverseRouter.route(on(this.getClass()).renderCorrect()))
        .addObject("removeUrl", ReverseRouter.route(on(this.getClass()).renderRemove()));
  }

  @GetMapping("/add")
  ModelAndView renderAdd() {
    return getCorrectionModelAndView(new LicenseeCorrectionForm(), List.of(), List.of());
  }

  @GetMapping("/correct")
  ModelAndView renderCorrect() {
    var form = new LicenseeCorrectionForm();
    form.setJoiningOrganisationUnitIds(
        JOINING_LICENSEES.stream().map(OrganisationUnitJson::getId).toList());
    form.setWithdrawingOrganisationUnitIds(
        WITHDRAWING_LICENSEES.stream().map(OrganisationUnitJson::getId).toList());
    return getCorrectionModelAndView(form, JOINING_LICENSEES, WITHDRAWING_LICENSEES);
  }

  @GetMapping("/remove")
  ModelAndView renderRemove() {
    return new ModelAndView("lms/mockups/licencecorrection/licensee/deleteLicenseeCorrection")
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback()));
  }

  private ModelAndView getCorrectionModelAndView(
      LicenseeCorrectionForm form,
      List<OrganisationUnitJson> preselectedJoiningOrgUnits,
      List<OrganisationUnitJson> preselectedWithdrawingOrgUnits
  ) {
    return new ModelAndView("lms/mockups/licencecorrection/licensee/licenseeCorrection")
        .addObject("form", form)
        .addObject("preselectedJoiningOrgUnits", preselectedJoiningOrgUnits)
        .addObject("preselectedWithdrawingOrgUnits", preselectedWithdrawingOrgUnits)
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback()));
  }
}

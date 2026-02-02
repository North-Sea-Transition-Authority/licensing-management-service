package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@LicenceActionEndPointInterceptorRule.ActionEndPoint(LicenceActionItem.MANAGE_RESPONSIBLE_TEAM)
@RequestMapping("licences/{licenceId}/responsible-team")
public class LicenceResponsibleTeamController {


  public static final String PAGE_TITLE = "Who is the licence allocated to?";
  private final LicenceResponsibleTeamValidator licenceResponsibleTeamValidator;
  private final LicenceResponsibleTeamService licenceResponsibleTeamService;

  public LicenceResponsibleTeamController(LicenceResponsibleTeamValidator licenceResponsibleTeamValidator,
                                          LicenceResponsibleTeamService licenceResponsibleTeamService) {
    this.licenceResponsibleTeamValidator = licenceResponsibleTeamValidator;
    this.licenceResponsibleTeamService = licenceResponsibleTeamService;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return getModelAndView(licence);
  }

  @PostMapping
  ModelAndView save(
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") LicenceResponsibleTeamForm form,
      BindingResult bindingResult
  ) {
    if (!licenceResponsibleTeamValidator.isValid(form, bindingResult)) {
      return getModelAndView(licence);
    }

    licenceResponsibleTeamService.saveLicenceResponsibleTeam(licence, form.getResponsibleTeam());

    return ReverseRouter.redirect(on(LicenceOverviewController.class).renderLicenceOverview(licenceId, null, null));
  }

  private ModelAndView getModelAndView(Licence licence) {
    var form = licenceResponsibleTeamService.getLicenceResponsibleTeamForm(licence);

    return new ModelAndView("lms/licence/manageResponsibleTeam")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("responsibleTeamOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceTeam.fromTeamType(licence.getType())))
        .addObject("backUrl",
            ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(licence.getId(), null, null)));
  }

}

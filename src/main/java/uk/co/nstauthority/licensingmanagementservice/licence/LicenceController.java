package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licences")
public class LicenceController {

  static final String NEW_LICENCE_PAGE_TITLE = "Add a new licence";

  private final LicenceFormService licenceFormService;
  private final NewLicenceValidator newLicenceValidator;
  private final ManageLicenseesValidator manageLicenseesValidator;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  public LicenceController(
      LicenceFormService licenceFormService,
      NewLicenceValidator newLicenceValidator,
      ManageLicenseesValidator manageLicenseesValidator,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {

    this.licenceFormService = licenceFormService;
    this.newLicenceValidator = newLicenceValidator;
    this.manageLicenseesValidator = manageLicenseesValidator;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
  }

  @GetMapping("/new")
  ModelAndView renderNewLicenceForm() {
    return getNewLicenceModelAndView(new NewLicenceForm());
  }

  @PostMapping("/new")
  ModelAndView saveNewLicence(
      @ModelAttribute("form") NewLicenceForm form,
      BindingResult bindingResult
  ) {
    if (newLicenceValidator.isValid(form, bindingResult)) {
      licenceFormService.saveNewLicenceFromForm(form);

      return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
    }

    return getNewLicenceModelAndView(form);
  }

  private ModelAndView getNewLicenceModelAndView(NewLicenceForm form) {
    return new ModelAndView("lms/licence/newLicence")
        .addObject("pageTitle", NEW_LICENCE_PAGE_TITLE)
        .addObject("form", form)
        .addObject("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms()))
        .addObject("preselectedOrgUnits",
            licenceFormService.getPreselectedOrganisationUnits(form.getOrganisationUnitIds()))
        .addObject("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("backUrl",
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }

  @GetMapping("/{licenceId}/manage-licensees")
  ModelAndView renderManageLicenseesPage(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    if (!licence.getType().getManagedByLms()) {
      throw new ResponseStatusException(
          HttpStatusCode.valueOf(403),
          "Licence with id: %s is not managed by LMS".formatted(licenceId)
      );
    }

    return getManageLicenseesModelAndView(
        new ManageLicenseesForm(),
        licence,
        licenceFormService.getSavedOrganisationUnits(licence)
    );
  }

  @PostMapping("/{licenceId}/manage-licensees")
  ModelAndView saveManageLicenseesPage(
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") ManageLicenseesForm form,
      BindingResult bindingResult
  ) {
    if (!licence.getType().getManagedByLms()) {
      throw new ResponseStatusException(
          HttpStatusCode.valueOf(403),
          "Licence with id: %s is not managed by LMS".formatted(licenceId)
      );
    }

    if (manageLicenseesValidator.isValid(form, bindingResult)) {
      licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, form.getOrganisationUnitIds());
      return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
    }

    return getManageLicenseesModelAndView(
        form,
        licence,
        licenceFormService.getPreselectedOrganisationUnits(form.getOrganisationUnitIds())
    );
  }

  private ModelAndView getManageLicenseesModelAndView(
      ManageLicenseesForm form,
      Licence licence,
      List<OrganisationUnitJson> organisationUnits
  ) {
    return new ModelAndView("lms/licence/manageLicensees")
        .addObject("pageTitle", licence.getLicenceReference())
        .addObject("pageCaption", "%s licence".formatted(licence.getType().getDisplayName()))
        .addObject("form", form)
        .addObject("preselectedOrgUnits", organisationUnits)
        .addObject("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("backUrl",
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }
}

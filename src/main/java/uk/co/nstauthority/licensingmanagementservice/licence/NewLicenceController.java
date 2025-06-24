package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licences/new")
public class NewLicenceController {

  static final String PAGE_TITLE = "Add a new licence";

  private final NewLicenceFormService newLicenceFormService;
  private final NewLicenceValidator newLicenceValidator;

  public NewLicenceController(
      NewLicenceFormService newLicenceFormService,
      NewLicenceValidator newLicenceValidator
  ) {

    this.newLicenceFormService = newLicenceFormService;
    this.newLicenceValidator = newLicenceValidator;
  }

  @GetMapping
  ModelAndView renderNewLicenceForm() {
    return getNewLicenceModelAndView(new NewLicenceForm());
  }

  @PostMapping
  ModelAndView saveNewLicence(
      @ModelAttribute("form") NewLicenceForm form,
      BindingResult bindingResult
  ) {
    if (newLicenceValidator.isValid(form, bindingResult)) {
      newLicenceFormService.saveNewLicenceFromForm(form);

      return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
    }

    return getNewLicenceModelAndView(form);
  }

  private ModelAndView getNewLicenceModelAndView(NewLicenceForm form) {
    return new ModelAndView("lms/licence/newLicence")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms()))
        .addObject("preselectedItems",
            newLicenceFormService.getPreselectedOrganisationUnits(form.getOrganisationUnitIds()))
        .addObject("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("backUrl",
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }

}

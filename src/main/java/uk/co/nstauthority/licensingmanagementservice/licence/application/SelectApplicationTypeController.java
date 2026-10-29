package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/start-application/{licenceTypeSlug}")
@InvokingUserCanStartApplication
public class SelectApplicationTypeController {
  public static final String PAGE_TITLE = "What are you applying to do?";

  private final SelectApplicationTypeFormValidator selectApplicationTypeFormValidator;
  private final SelectApplicationTypeService selectApplicationTypeService;

  public SelectApplicationTypeController(
      SelectApplicationTypeFormValidator selectApplicationTypeFormValidator,
      SelectApplicationTypeService selectApplicationTypeService) {
    this.selectApplicationTypeFormValidator = selectApplicationTypeFormValidator;
    this.selectApplicationTypeService = selectApplicationTypeService;
  }

  @GetMapping
  public ModelAndView render(@PathVariable String licenceTypeSlug) {
    var licenceType = selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug);
    return getModelAndView(new SelectApplicationTypeForm(), licenceType);
  }

  @PostMapping
  ModelAndView submit(
      @PathVariable String licenceTypeSlug,
      @ModelAttribute("form") SelectApplicationTypeForm form,
      BindingResult bindingResult
  ) {
    var licenceType = selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug);
    var availableApplicationTypes = selectApplicationTypeService.getAvailableApplicationTypes(licenceType);

    if (!selectApplicationTypeFormValidator.isValid(form, bindingResult, availableApplicationTypes)) {
      return getModelAndView(form, licenceType);
    }

    return selectApplicationTypeService.getJourneyStartRedirectRoute(form.getSelectedApplicationType(), licenceType);
  }

  private ModelAndView getModelAndView(SelectApplicationTypeForm form, LicenceType licenceType) {
    var applicationTypeOptions = ApplicationType.getSelectionDisplayOptions(
        selectApplicationTypeService.getAvailableApplicationTypes(licenceType));

    return new ModelAndView("lms/licence/application/selectApplicationType")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("applicationTypeOptions", applicationTypeOptions)
        .addObject("backUrl", ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).renderSelectLicenceType()));
  }
}

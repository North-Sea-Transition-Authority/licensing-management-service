package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/start-application/{licenceTypeSlug}/continuation-application/licence")
@InvokingUserCanStartApplication
public class SelectContinuationApplicationLicenceController {

  static final String PAGE_TITLE
      = "What licence do you want to create a licence continuation application for?";

  private final SelectContinuationApplicationLicenceFormValidator selectLicenceFormValidator;
  private final LicenceService licenceService;
  private final SelectApplicationTypeService selectApplicationTypeService;

  public SelectContinuationApplicationLicenceController(
      SelectContinuationApplicationLicenceFormValidator selectLicenceFormValidator,
      LicenceService licenceService,
      SelectApplicationTypeService selectApplicationTypeService
  ) {
    this.selectLicenceFormValidator = selectLicenceFormValidator;
    this.licenceService = licenceService;
    this.selectApplicationTypeService = selectApplicationTypeService;
  }

  @GetMapping
  public ModelAndView render(@PathVariable String licenceTypeSlug) {
    return getModelAndView(new SelectContinuationApplicationLicenceForm(), getLicenceTypeOrThrow(licenceTypeSlug));
  }

  @PostMapping
  ModelAndView submit(
      @PathVariable String licenceTypeSlug,
      @ModelAttribute("form") SelectContinuationApplicationLicenceForm form,
      BindingResult bindingResult
  ) {
    var licenceType = getLicenceTypeOrThrow(licenceTypeSlug);

    if (!selectLicenceFormValidator.isValid(bindingResult)) {
      return getModelAndView(form, licenceType);
    }

    var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getLicenceId()));

    if (licence.getType() != licenceType) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Licence %s is not of licence type %s".formatted(licence.getId(), licenceType)
      );
    }

    return ReverseRouter.redirect(on(LicenceContinuationLicenseeInformationController.class)
                                      .renderConfirmLicenseePermission(licence.getId(), null, null));
  }

  private LicenceType getLicenceTypeOrThrow(String licenceTypeSlug) {
    return selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug, ApplicationType.CONTINUATION_APPLICATION);
  }

  private ModelAndView getModelAndView(SelectContinuationApplicationLicenceForm form, LicenceType licenceType) {
    return new ModelAndView("lms/licence/continuation/selectLicence")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndType(licenceType.getUrlSlug(), null, null))
        ).addObject("backUrl", ReverseRouter.route(on(StartContinuationApplicationController.class)
            .render(licenceType.getUrlSlug())));
  }
}

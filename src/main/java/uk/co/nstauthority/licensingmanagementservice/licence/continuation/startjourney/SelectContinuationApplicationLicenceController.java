package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTypeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/continuation-application/licence")
@InvokingUserCanStartApplication
public class SelectContinuationApplicationLicenceController {

  static final String PAGE_TITLE
      = "What licence do you want to create a licence continuation application for?";

  private final SelectContinuationApplicationLicenceFormValidator selectLicenceFormValidator;
  private final LicenceService licenceService;

  public SelectContinuationApplicationLicenceController(
      SelectContinuationApplicationLicenceFormValidator selectLicenceFormValidator,
      LicenceService licenceService
  ) {
    this.selectLicenceFormValidator = selectLicenceFormValidator;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView render() {
    return getModelAndView(new SelectContinuationApplicationLicenceForm());
  }

  @PostMapping
  ModelAndView submit(
      @ModelAttribute("form") SelectContinuationApplicationLicenceForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceFormValidator.isValid(bindingResult)) {
      return getModelAndView(form);
    }

    var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getLicenceId()));

    return ReverseRouter.redirect(on(LicenceContinuationLicenseeInformationController.class)
                                      .renderConfirmLicenseePermission(licence.getId(), null, null));
  }

  private ModelAndView getModelAndView(SelectContinuationApplicationLicenceForm form) {
    var licenceTypeSlugList = LicenceTypeUtil.getUrlSlugList(List.of(
        LicenceType.LANDWARD_PRODUCTION,
        LicenceType.SEAWARD_PRODUCTION
    ));

    return new ModelAndView("lms/licence/continuation/selectLicence")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndTypeForContinuationApplication(licenceTypeSlugList, null, null))
        ).addObject("backUrl", ReverseRouter.route(on(StartContinuationApplicationController.class).render()));
  }
}

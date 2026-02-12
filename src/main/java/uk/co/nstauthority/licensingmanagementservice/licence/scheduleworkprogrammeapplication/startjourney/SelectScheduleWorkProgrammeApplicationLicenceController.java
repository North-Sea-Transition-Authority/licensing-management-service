package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

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
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/schedule-work-programme-application/{licenceTypeSlug}/licence")
@InvokingUserCanStartApplication
public class SelectScheduleWorkProgrammeApplicationLicenceController {

  static final String PAGE_TITLE
      = "What licence do you want to create a schedule extension or work programme amendment application for?";

  private final SelectScheduleWorkProgrammeApplicationLicenceFormValidator selectLicenceFormValidator;
  private final LicenceService licenceService;

  public SelectScheduleWorkProgrammeApplicationLicenceController(
      SelectScheduleWorkProgrammeApplicationLicenceFormValidator selectLicenceFormValidator,
      LicenceService licenceService) {
    this.selectLicenceFormValidator = selectLicenceFormValidator;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderSelectLicenceForScheduleWorkProgrammeApplication(@PathVariable String licenceTypeSlug) {
    return getSelectLicenceModelAndView(new SelectScheduleWorkProgrammeApplicationLicenceForm(), licenceTypeSlug);
  }

  @PostMapping
  ModelAndView submitSelectLicenceForScheduleWorkProgrammeApplication(
      @PathVariable String licenceTypeSlug,
      @ModelAttribute("form") SelectScheduleWorkProgrammeApplicationLicenceForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceFormValidator.isValid(bindingResult)) {
      return getSelectLicenceModelAndView(form, licenceTypeSlug);
    }

    var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getLicenceId()));

    return ReverseRouter.redirect(on(LicenseeInformationController.class)
        .renderConfirmLicenseePermission(licenceTypeSlug, licence.getId(), null, null));
  }

  private ModelAndView getSelectLicenceModelAndView(SelectScheduleWorkProgrammeApplicationLicenceForm selectLicenceForm,
                                                    String licenceTypeSlug) {
    var licenceType = LicenceType.getFromSlugOrThrow(licenceTypeSlug);

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectLicence")
        .addObject("form", selectLicenceForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndType(licenceTypeSlug, null, null))
        )
        .addObject("backUrl", ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class)
            .renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug()))
        );
  }
}
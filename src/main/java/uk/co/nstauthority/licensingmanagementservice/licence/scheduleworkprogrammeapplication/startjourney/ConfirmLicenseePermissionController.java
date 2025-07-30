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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licences/schedule-work-programme-applications/{licenceTypeSlug}/{licenceId}/confirm-licensee-permission")
public class ConfirmLicenseePermissionController {
  public static final String PAGE_TITLE = "Have you confirmed this request is made on behalf of all licensees?";

  private final ConfirmLicenseePermissionFormValidator confirmLicenseePermissionFormValidator;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceService licenceService;

  public ConfirmLicenseePermissionController(ConfirmLicenseePermissionFormValidator confirmLicenseePermissionFormValidator,
                                             ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
                                             LicenceService licenceService) {
    this.confirmLicenseePermissionFormValidator = confirmLicenseePermissionFormValidator;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceService = licenceService;
  }

  @GetMapping
  ModelAndView renderConfirmLicenseePermission(@PathVariable String licenceTypeSlug,
                                               @PathVariable Integer licenceId) {
    return gettLicenseePermissionConfirmationModelAndView(new ConfirmLicenseePermissionForm(), licenceTypeSlug);
  }

  @PostMapping
  ModelAndView submitLicenseePermissionConfirmation(
      @PathVariable String licenceTypeSlug,
      @PathVariable Integer licenceId,
      @ModelAttribute("form") ConfirmLicenseePermissionForm form,
      BindingResult bindingResult
  ) {
    var licence = licenceService.findLicenceByIdOrThrow(licenceId);

    if (!confirmLicenseePermissionFormValidator.isValid(form, bindingResult)) {
      return gettLicenseePermissionConfirmationModelAndView(form, licenceTypeSlug);
    }

    scheduleWorkProgrammeApplicationService
        .createNewScheduleWorkProgrammeApplicationForLicence(licence, form.getAllLicenseesPermissionConfirmed());

    // TODO: LMS1-130 redirect to schedule extension or work programme amendment application task list
    return ReverseRouter.redirect(on(WorkAreaController.class)

        .getWorkArea(null, null));
  }

  private ModelAndView gettLicenseePermissionConfirmationModelAndView(
      ConfirmLicenseePermissionForm selectLicenceTypeForm,
      String licenceTypeSlug) {
    var licenceType = LicenceType.getFromSlugOrThrow(licenceTypeSlug);

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/confirmLicenseePermission")
        .addObject("form", selectLicenceTypeForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("backUrl", ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class)
            .renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))
        );
  }
}

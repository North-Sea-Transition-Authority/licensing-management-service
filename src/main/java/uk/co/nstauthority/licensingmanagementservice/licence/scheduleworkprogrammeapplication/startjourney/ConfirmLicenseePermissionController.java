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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/schedule-work-programme-application/{licenceTypeSlug}/{licenceId}/confirm-licensee-permission")
public class ConfirmLicenseePermissionController {
  public static final String PAGE_TITLE = "Have you confirmed this request is made on behalf of all licensees?";

  private final ConfirmLicenseePermissionFormValidator confirmLicenseePermissionFormValidator;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public ConfirmLicenseePermissionController(ConfirmLicenseePermissionFormValidator confirmLicenseePermissionFormValidator,
                                             ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService) {
    this.confirmLicenseePermissionFormValidator = confirmLicenseePermissionFormValidator;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  @GetMapping
  ModelAndView renderConfirmLicenseePermission(@PathVariable String licenceTypeSlug,
                                               @PathVariable Integer licenceId) {
    return getLicenseePermissionConfirmationModelAndView(new ConfirmLicenseePermissionForm(), licenceTypeSlug);
  }

  @PostMapping
  ModelAndView submitLicenseePermissionConfirmation(
      @PathVariable String licenceTypeSlug,
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") ConfirmLicenseePermissionForm form,
      BindingResult bindingResult
  ) {

    if (!confirmLicenseePermissionFormValidator.isValid(form, bindingResult)) {
      return getLicenseePermissionConfirmationModelAndView(form, licenceTypeSlug);
    }

    var applicationDetail = scheduleWorkProgrammeApplicationService
        .createNewScheduleWorkProgrammeApplicationForLicence(licence, form.getAllLicenseesPermissionConfirmed());

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(applicationDetail.getId(), null, null));
  }

  private ModelAndView getLicenseePermissionConfirmationModelAndView(
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

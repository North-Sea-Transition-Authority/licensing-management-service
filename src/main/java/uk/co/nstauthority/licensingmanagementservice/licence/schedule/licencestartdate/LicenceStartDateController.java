package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.LicenceScheduleSelectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/{licenceId}/schedule/start-date")
public class LicenceStartDateController {

  static final String PAGE_TITLE = "What is the licence start date?";

  private final LicenceStartDateValidator licenceStartDateValidator;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleService licenceScheduleService;

  public LicenceStartDateController(
      LicenceStartDateValidator licenceStartDateValidator,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleService licenceScheduleService
  ) {
    this.licenceStartDateValidator = licenceStartDateValidator;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleService = licenceScheduleService;
  }

  @GetMapping("/create")
  public ModelAndView renderScheduleDetailsForm(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return getScheduleDetailsModelAndView(
        new LicenceStartDateForm(),
        licenceScheduleService.doesLicenceScheduleExistForLicence(licence)
    );
  }

  @PostMapping("/create")
  public ModelAndView submitScheduleDetailsForm(
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") LicenceStartDateForm form,
      BindingResult bindingResult
  ) {
    if (!licenceStartDateValidator.isValid(form, bindingResult)) {
      return getScheduleDetailsModelAndView(
          form,
          licenceScheduleService.doesLicenceScheduleExistForLicence(licence)
      );
    }
    var scheduleDetailId = licenceStartDateService.saveNewLicenceStartDateFromForm(form, licence)
        .getLicenceScheduleDetail().getId();

    return ReverseRouter.redirect(on(LicenceScheduleTermController.class).renderAddNewTermForm(scheduleDetailId, null));
  }

  private ModelAndView getScheduleDetailsModelAndView(LicenceStartDateForm form, boolean backToTaskList) {
    //TODO: LMS1-87 redirect to task list
    var backUrl = backToTaskList
        ? ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
        : ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule());

    return new ModelAndView("lms/licence/schedule/startDate")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("backUrl", backUrl);
  }

}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.LicenceScheduleSelectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
public class LicenceStartDateController {

  static final String PAGE_TITLE = "What is the licence start date?";

  private final LicenceStartDateValidator licenceStartDateValidator;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  public LicenceStartDateController(
      LicenceStartDateValidator licenceStartDateValidator,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.licenceStartDateValidator = licenceStartDateValidator;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
  }

  @GetMapping("/licence/{licenceId}/schedule/start-date")
  public ModelAndView renderLicenceStartDateForm(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return getScheduleDetailsModelAndView(
        new LicenceStartDateForm(),
        ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule())
    );
  }

  @PostMapping("/licence/{licenceId}/schedule/start-date")
  public ModelAndView submitLicenceStartDateForm(
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") LicenceStartDateForm form,
      BindingResult bindingResult
  ) {
    if (!licenceStartDateValidator.isValid(form, bindingResult)) {
      return getScheduleDetailsModelAndView(
          form,
          ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule())
      );
    }
    var scheduleDetailId = licenceStartDateService.saveNewLicenceStartDateFromForm(form, licence)
        .getLicenceScheduleDetail().getId();

    return ReverseRouter.redirect(on(LicenceScheduleTermController.class).renderAddNewTermForm(scheduleDetailId, null));
  }

  @GetMapping("/licence/schedule/{licenceScheduleDetailId}/start-date")
  public ModelAndView renderLicenceStartDateUpdateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleDetailsModelAndView(
        licenceStartDateService.getLicenceStartDateForm(licenceScheduleDetail),
        getTimelineUrl(licenceScheduleDetailId)
    );
  }

  @PostMapping("/licence/schedule/{licenceScheduleDetailId}/start-date")
  public ModelAndView submitLicenceStartDateUpdateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceStartDateForm form,
      BindingResult bindingResult
  ) {
    if (!licenceStartDateValidator.isValid(form, bindingResult)) {
      return getScheduleDetailsModelAndView(
          form,
          getTimelineUrl(licenceScheduleDetailId)
      );
    }

    licenceStartDateService.saveOrUpdateLicenceStartDateFromForm(form, licenceScheduleDetail);
    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    return ReverseRouter.redirect(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetailId, null));
  }

  private ModelAndView getScheduleDetailsModelAndView(LicenceStartDateForm form, String backUrl) {
    return new ModelAndView("lms/licence/schedule/startDate")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("backUrl", backUrl);
  }

  private String getTimelineUrl(UUID licenceScheduleDetailId) {
    return ReverseRouter.route(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetailId, null));
  }
}

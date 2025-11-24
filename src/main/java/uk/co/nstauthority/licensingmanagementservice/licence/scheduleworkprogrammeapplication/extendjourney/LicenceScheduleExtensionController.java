package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;


@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/extension-details")
public class LicenceScheduleExtensionController {

  public static final String PAGE_TITLE = "Extension Details";
  private final LicenceScheduleExtensionService licenceScheduleExtensionFormService;
  private final LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  public LicenceScheduleExtensionController(
      LicenceScheduleExtensionService licenceScheduleExtensionFormService,
      LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator
  ) {
    this.licenceScheduleExtensionFormService = licenceScheduleExtensionFormService;
    this.licenceScheduleExtensionFormValidator = licenceScheduleExtensionFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        licenceScheduleExtensionFormService.getlicenceScheduleExtensionForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceScheduleExtensionForm form,
      BindingResult bindingResult
  ) {
    if (!licenceScheduleExtensionFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceScheduleExtensionFormService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail, null));
  }

  private ModelAndView getModelAndView(LicenceScheduleExtensionForm form,
                                       ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplicationDetail
        .getScheduleWorkProgrammeApplication();

    var scheduleWorkProgrammeApplicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceExtension")
                .addObject("pageTitle", PAGE_TITLE)
                .addObject("form", form)
                .addObject("currentTerm", licenceScheduleExtensionFormService.getCurrentTerm(
                        scheduleWorkProgrammeApplication.getLicenceScheduleDetail()))
                .addObject("currentPhase", licenceScheduleExtensionFormService.getCurrentPhase(
                        licenceScheduleExtensionFormService.getCurrentTerm(
                            scheduleWorkProgrammeApplication.getLicenceScheduleDetail())))
                .addObject("validTermsAndPhases", licenceScheduleExtensionFormService.getExtendableTermAndPhases(
                        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceScheduleDetail()))
                .addObject("canExtendMoreThanOneOption", licenceScheduleExtensionFormService.canExtendMoreThanOneOption(
                        licenceScheduleExtensionFormService.getExtendableTermAndPhases(
                        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceScheduleDetail())))
                .addObject("cancelUrl", ReverseRouter.route(
                        on(ScheduleWorkProgrammeApplicationTaskListController.class)
                            .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null)));

    if (licenceScheduleExtensionFormService.getCurrentTerm(
        scheduleWorkProgrammeApplication.getLicenceScheduleDetail()) != null) {
      modelAndView.addObject("currentTermEndDate", DateFormatUtil.convertToDisplayText(
          licenceScheduleExtensionFormService.getCurrentTerm(
              scheduleWorkProgrammeApplication.getLicenceScheduleDetail())
                                             .getEndDate()));
    }
    if (licenceScheduleExtensionFormService.getCurrentPhase(
        licenceScheduleExtensionFormService.getCurrentTerm(
            scheduleWorkProgrammeApplication.getLicenceScheduleDetail())) != null) {
      modelAndView.addObject("currentPhaseEndDate", DateFormatUtil.convertToDisplayText(
          licenceScheduleExtensionFormService.getCurrentPhase(
              licenceScheduleExtensionFormService.getCurrentTerm(
                  scheduleWorkProgrammeApplication.getLicenceScheduleDetail()))
                                             .getEndDate()));
    }
    return modelAndView;
  }
}
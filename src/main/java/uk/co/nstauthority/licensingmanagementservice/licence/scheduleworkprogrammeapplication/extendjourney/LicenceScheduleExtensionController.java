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
  private final LicenceScheduleExtensionFormService licenceScheduleExtensionFormService;
  private final LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  public LicenceScheduleExtensionController(
      LicenceScheduleExtensionFormService licenceScheduleExtensionFormService,
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
        licenceScheduleExtensionFormService.getLicenceScheduleExtensionForm(scheduleWorkProgrammeApplicationDetail),
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
    if (!licenceScheduleExtensionFormValidator.isValid(form, bindingResult)) {
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

    var currentTerm = licenceScheduleExtensionFormService.getCurrentTerm(
        scheduleWorkProgrammeApplication.getLicenceScheduleDetail());
    var currentPhase = licenceScheduleExtensionFormService.getCurrentPhase(
        scheduleWorkProgrammeApplication.getLicenceScheduleDetail());

    var scheduleWorkProgrammeApplicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceExtension");
    modelAndView.addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("currentTerm", currentTerm)
        .addObject("currentPhase", currentPhase)
        .addObject("currentTermEndDate", DateFormatUtil.convertToDisplayText(currentTerm.getEndDate()))
        .addObject("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null
                )));
    if (currentPhase != null) {
      modelAndView.addObject("currentPhaseEndDate", DateFormatUtil.convertToDisplayText(currentPhase.getEndDate()));
    }
    return modelAndView;
  }
}
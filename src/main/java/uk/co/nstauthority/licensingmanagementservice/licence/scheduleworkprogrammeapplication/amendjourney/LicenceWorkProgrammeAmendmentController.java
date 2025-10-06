package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;


@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/" +
    "work-programme-amendment/{workProgrammeActivityId}/details")
public class LicenceWorkProgrammeAmendmentController {

  public static final String PAGE_TITLE = "Work programme amendments";
  private final LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  public LicenceWorkProgrammeAmendmentController(
      LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator
  ) {
    this.licenceWorkProgrammeAmendmentFormService = licenceWorkProgrammeAmendmentFormService;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID workProgrammeActivityId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        licenceWorkProgrammeAmendmentFormService.getLicenceWorkProgrammeActivityAmendmentForm(
            scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID workProgrammeActivityId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceWorkProgrammeAmendmentForm form,
      BindingResult bindingResult
  ) {
    if (!licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceWorkProgrammeAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail, null));
  }

  private ModelAndView getModelAndView(LicenceWorkProgrammeAmendmentForm form,
                                       ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {


    UUID scheduleWorkProgrammeApplicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("isLinkedToPhaseOrTerm", isLinkedToPhaseOrTerm())
        .addObject("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail, null)));

  }

  public boolean isLinkedToPhaseOrTerm() {
    return true;
  }
}
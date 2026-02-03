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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping
    ("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/work-programme-amendment")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
public class SelectLicenceWorkAmendmentController {

  public static final String PAGE_TITLE = "What work programme activity are you requesting to amend?";
  private final SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator;
  private final SelectLicenceAmendmentService selectLicenceAmendmentService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public SelectLicenceWorkAmendmentController(
      SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator,
      SelectLicenceAmendmentService selectLicenceAmendmentService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.selectLicenceAmendmentFormValidator = selectLicenceAmendmentFormValidator;
    this.selectLicenceAmendmentService = selectLicenceAmendmentService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @GetMapping("/create")
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(new SelectLicenceAmendmentForm(), scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping("/create")
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") SelectLicenceAmendmentForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceAmendmentFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    selectLicenceAmendmentService
        .saveAmendmentForm(form.getSelectedWorkProgrammeActivityAmendmentId(), form,
        scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(LicenceWorkProgrammeAmendmentController.class)
        .renderForm(form.selectedWorkProgrammeActivityAmendmentId, null, scheduleWorkProgrammeApplicationDetailId, null));
  }

  private ModelAndView getModelAndView(
      SelectLicenceAmendmentForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("workProgrammeAmendmentViews", workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(
                scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceScheduleDetail()
        )).addObject("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null
            )));

  }
}
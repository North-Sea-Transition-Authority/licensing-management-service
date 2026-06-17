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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.RequestPurposeChoiceMustBeApplicable;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping
    ("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/work-programme-amendment")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
@RequestPurposeChoiceMustBeApplicable
public class SelectLicenceWorkAmendmentController {

  public static final String PAGE_TITLE = "What work programme activity are you requesting to amend?";
  private final SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public SelectLicenceWorkAmendmentController(
      SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator,
      WorkProgrammeActivityService workProgrammeActivityService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService
  ) {
    this.selectLicenceAmendmentFormValidator = selectLicenceAmendmentFormValidator;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
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
    if (!selectLicenceAmendmentFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    return ReverseRouter.redirect(on(LicenceWorkProgrammeAmendmentController.class)
        .renderForm(form.getSelectedWorkProgrammeActivityAmendmentId(), null, scheduleWorkProgrammeApplicationDetailId, null));
  }

  private ModelAndView getModelAndView(
      SelectLicenceAmendmentForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("workProgrammeAmendmentViews", workProgrammeActivityService.getCurrentWorkProgrammeActivitiesViews(
            scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        ))
        .addObject("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
            scheduleWorkProgrammeApplicationDetail.getId(), null, null
        )));
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

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
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/corresponding-reduction-details")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordReductionDetailsController {

  static final String PAGE_TITLE = "Corresponding reduction details";

  private final RecordReductionDetailsService recordReductionDetailsService;
  private final RecordReductionDetailsFormValidator recordReductionDetailsFormValidator;

  public RecordReductionDetailsController(
      RecordReductionDetailsService recordReductionDetailsService,
      RecordReductionDetailsFormValidator recordReductionDetailsFormValidator
  ) {
    this.recordReductionDetailsService = recordReductionDetailsService;
    this.recordReductionDetailsFormValidator = recordReductionDetailsFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getModelAndView(
        recordReductionDetailsService.getFilledForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") RecordReductionDetailsForm form,
      BindingResult bindingResult) {
    if (!recordReductionDetailsFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    recordReductionDetailsService.saveReductionDetails(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(
      RecordReductionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var reductionDetailsViews = recordReductionDetailsService
        .getReductionDetailsViews(scheduleWorkProgrammeApplicationDetail);

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordReductionDetails")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("reductionDetailsViews", reductionDetailsViews)
        .addObject("canReduceMoreThanOneOption", reductionDetailsViews.size() > 1)
        .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}

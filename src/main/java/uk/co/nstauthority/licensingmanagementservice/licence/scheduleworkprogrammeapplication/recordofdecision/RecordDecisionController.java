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

// TODO LMS1-541: entry point comes later, status and access role here are placeholders
@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/what-is-the-decision")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordDecisionController {

  static final String PAGE_TITLE = "What is the decision?";

  private final RecordDecisionFormValidator recordDecisionFormValidator;
  private final RecordOfDecisionService recordOfDecisionService;

  public RecordDecisionController(
      RecordDecisionFormValidator recordDecisionFormValidator,
      RecordOfDecisionService recordOfDecisionService
  ) {
    this.recordDecisionFormValidator = recordDecisionFormValidator;
    this.recordOfDecisionService = recordOfDecisionService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getModelAndView(
        recordOfDecisionService.getFilledDecisionForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") RecordDecisionForm form,
      BindingResult bindingResult) {
    if (!recordDecisionFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    recordOfDecisionService.saveDecision(scheduleWorkProgrammeApplicationDetail, form);

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(
      RecordDecisionForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/whatIsTheDecision")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("decisionOptions", RecordOfDecisionResponse.getOptions())
        .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}" +
        "/duration-changes/summary")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordDurationChangesSummaryController {

  static final String PAGE_TITLE = "Review changes to term and phase durations";

  private final RecordDurationChangesService recordDurationChangesService;

  public RecordDurationChangesSummaryController(RecordDurationChangesService recordDurationChangesService) {
    this.recordDurationChangesService = recordDurationChangesService;
  }

  @GetMapping
  public ModelAndView renderSummary(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    if (!recordDurationChangesService.isComplete(scheduleWorkProgrammeApplicationDetail)) {
      return redirectToDurationChanges(scheduleWorkProgrammeApplicationDetailId);
    }

    return getModelAndView(scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView confirmSummary(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    if (!recordDurationChangesService.isComplete(scheduleWorkProgrammeApplicationDetail)) {
      return redirectToDurationChanges(scheduleWorkProgrammeApplicationDetailId);
    }

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView redirectToDurationChanges(UUID scheduleWorkProgrammeApplicationDetailId) {
    return ReverseRouter.redirect(on(RecordDurationChangesController.class)
        .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
  }

  private ModelAndView getModelAndView(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView =
        new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordDurationChangesSummary")
            .addObject("pageTitle", PAGE_TITLE)
            .addObject("durationChangeSummaryViews",
                recordDurationChangesService.getSummaryViews(scheduleWorkProgrammeApplicationDetail))
            .addObject("backUrl", ReverseRouter.route(on(RecordDurationChangesController.class)
                .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null)));

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}" +
        "/work-programme-amendment-details/{workProgrammeActivityId}/remove")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.DRAFT_DECISION)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.RESUME_DRAFT_DECISION)
public class RecordWorkProgrammeAmendmentRemoveController {

  static final String PAGE_TITLE = "Are you sure you want to remove this work programme amendment?";

  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  public RecordWorkProgrammeAmendmentRemoveController(
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService
  ) {
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      @PathVariable UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    var summaryUrl = ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
        .renderForm(scheduleWorkProgrammeApplicationDetailId, null));

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView =
        new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordWorkProgrammeAmendmentRemove")
            .addObject("pageTitle", PAGE_TITLE)
            .addObject("workProgrammeAmendment", recordWorkProgrammeAmendmentDetailsService
                .getRecordedAmendmentView(scheduleWorkProgrammeApplicationDetail, workProgrammeActivity))
            .addObject("actionUrl", ReverseRouter.route(on(RecordWorkProgrammeAmendmentRemoveController.class)
                .removeAmendmentDetails(
                    scheduleWorkProgrammeApplicationDetailId, workProgrammeActivityId, null, null, null)))
            .addObject("cancelUrl", summaryUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @PostMapping
  public ModelAndView removeAmendmentDetails(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      @PathVariable UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity,
      RedirectAttributes redirectAttributes
  ) {
    recordWorkProgrammeAmendmentDetailsService.removeAmendmentDetails(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);

    NotificationBanner.newSuccessBannerWithHeader(
        "Work programme amendment for %s has been removed".formatted(workProgrammeActivity.getDescription()),
        redirectAttributes);

    if (recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(scheduleWorkProgrammeApplicationDetail)) {
      return ReverseRouter.redirect(on(RecordWorkProgrammeAmendmentSummaryController.class)
          .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }

    return ReverseRouter.redirect(on(SelectWorkProgrammeActivityController.class)
        .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
  }
}

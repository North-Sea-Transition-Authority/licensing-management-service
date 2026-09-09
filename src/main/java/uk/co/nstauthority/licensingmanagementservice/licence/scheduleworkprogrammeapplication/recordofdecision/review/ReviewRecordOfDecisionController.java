package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionTaskListContext;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/record-of-decision/review")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class ReviewRecordOfDecisionController {

  static final String PAGE_TITLE = "Review record of decision";
  static final String SUBMITTED_BANNER = "Record of decision submitted for %s";

  private final RecordOfDecisionSummarySectionService recordOfDecisionSummarySectionService;
  private final RecordOfDecisionTaskListService recordOfDecisionTaskListService;
  private final LicenceService licenceService;

  public ReviewRecordOfDecisionController(
      RecordOfDecisionSummarySectionService recordOfDecisionSummarySectionService,
      RecordOfDecisionTaskListService recordOfDecisionTaskListService,
      LicenceService licenceService
  ) {
    this.recordOfDecisionSummarySectionService = recordOfDecisionSummarySectionService;
    this.recordOfDecisionTaskListService = recordOfDecisionTaskListService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderReview(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var context = new RecordOfDecisionTaskListContext(scheduleWorkProgrammeApplicationDetail);

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/reviewRecordOfDecision")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceService.getLicencePageCaption(
            scheduleWorkProgrammeApplicationDetail.getLicence()))
        .addObject("summarySections", recordOfDecisionSummarySectionService.getSummarySections(
            new RecordOfDecisionSummaryContext(scheduleWorkProgrammeApplicationDetail), user))
        .addObject("accordionId", scheduleWorkProgrammeApplicationDetail.getId())
        .addObject("isSubmittable", recordOfDecisionTaskListService.isSubmittable(context, user))
        .addObject("cancelUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @PostMapping
  public ModelAndView submitReview(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes) {

    var context = new RecordOfDecisionTaskListContext(scheduleWorkProgrammeApplicationDetail);

    if (!recordOfDecisionTaskListService.isSubmittable(context, user)) {
      return ReverseRouter.redirect(on(ReviewRecordOfDecisionController.class)
          .renderReview(scheduleWorkProgrammeApplicationDetailId, null, null));
    }

    var applicationReference = scheduleWorkProgrammeApplicationDetail
        .getScheduleWorkProgrammeApplication().getApplicationReference();
    NotificationBanner.newSuccessBanner()
        .withHeadingContent(SUBMITTED_BANNER.formatted(applicationReference))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }
}

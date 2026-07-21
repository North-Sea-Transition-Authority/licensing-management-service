package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.RequestPurposeChoiceMustBeApplicable;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/" +
    "work-programme-amendment/{workProgrammeActivityId}")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
@RequestPurposeChoiceMustBeApplicable
public class LicenceWorkProgrammeAmendmentDeleteController {

  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;
  private final LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  LicenceWorkProgrammeAmendmentDeleteController(
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService,
      LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService
  ) {
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
    this.licenceWorkProgrammeAmendmentSummaryService = licenceWorkProgrammeAmendmentSummaryService;
  }

  @GetMapping("/delete")
  public ModelAndView renderForm(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceWorkProgrammeAmendment =
        licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivity).orElse(null);
    return getModelAndView(workProgrammeActivityId, scheduleWorkProgrammeApplicationDetailId, licenceWorkProgrammeAmendment);
  }

  @PostMapping("/delete")
  public ModelAndView deleteLicenceWorkProgrammeAmendment(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      RedirectAttributes redirectAttributes
  ) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentService
        .getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);

    addRedirectNotification(workProgrammeActivity, redirectAttributes);

    licenceWorkProgrammeAmendmentService.deleteWorkProgrammeAmendment(licenceWorkProgrammeAmendmentRequest,
          scheduleWorkProgrammeApplicationDetail);

    boolean hasRemainingAmendments = licenceWorkProgrammeAmendmentService
        .hasAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    if (hasRemainingAmendments) {
      return ReverseRouter.redirect(
          on(LicenceWorkProgrammeAmendmentSummaryController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    } else {
      return ReverseRouter.redirect(on(SelectLicenceWorkAmendmentController.class)
          .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }
  }

  private ModelAndView getModelAndView(UUID workProgrammeActivityId,
                                       UUID scheduleWorkProgrammeApplicationDetailId,
                                       LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest
  ) {
    var taskListUrl = ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));

    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentDeleteConfirmation")
        .addObject("backToSummaryUrl", ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class)
            .renderForm(scheduleWorkProgrammeApplicationDetailId, null)))
        .addObject("actionUrl", ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class)
            .deleteLicenceWorkProgrammeAmendment(workProgrammeActivityId, null, scheduleWorkProgrammeApplicationDetailId,
                null, null)))
        .addObject("LicenceWorkProgrammeAmendmentSummaryView",
            licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(
                licenceWorkProgrammeAmendmentRequest, LicenceWorkProgrammeAmendmentSummaryMode.VIEW));

    var breadcrumbs = Breadcrumbs.builder("Are you sure you want to delete this work programme amendment?")
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }

  private void addRedirectNotification(WorkProgrammeActivity workProgrammeActivity, RedirectAttributes redirectAttributes) {
    var workProgrammeActivityCategory = workProgrammeActivity.getCategory().getDisplayName();
    NotificationBanner.newSuccessBannerWithHeader(
        String.format("Work programme amendment for %s has been deleted", workProgrammeActivityCategory), redirectAttributes
    );
  }
}
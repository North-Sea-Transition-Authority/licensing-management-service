package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/delete-application")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
public class ScheduleWorkProgrammeApplicationDeleteController {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  public ScheduleWorkProgrammeApplicationDeleteController(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
  }

  @GetMapping()
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping()
  public ModelAndView deleteScheduleWorkProgrammeApplication(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      RedirectAttributes redirectAttributes
  ) {
    scheduleWorkProgrammeApplicationService.deleteScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplicationDetail);
    addRedirectNotification(redirectAttributes);
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  private ModelAndView getModelAndView(
      UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationDeleteConfirmation")
        .addObject("backToSummaryUrl", ReverseRouter.route(on(
                ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
                scheduleWorkProgrammeApplicationDetailId,
                null,
                null)))
        .addObject("actionUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class)
                                                        .deleteScheduleWorkProgrammeApplication(
                                                            scheduleWorkProgrammeApplicationDetailId,
                                                            null,
                                                            null)))
        .addObject("summarySections", licenceScheduleSummarySectionService.getSummarySections(
                scheduleWorkProgrammeApplicationDetail,
                null))
        .addObject("accordionId", scheduleWorkProgrammeApplicationDetailId);
  }

  private void addRedirectNotification(RedirectAttributes redirectAttributes) {
    NotificationBanner.newSuccessBannerWithHeader(
        "Schedule and work programme amendment application has been deleted",
        redirectAttributes
    );
  }
}
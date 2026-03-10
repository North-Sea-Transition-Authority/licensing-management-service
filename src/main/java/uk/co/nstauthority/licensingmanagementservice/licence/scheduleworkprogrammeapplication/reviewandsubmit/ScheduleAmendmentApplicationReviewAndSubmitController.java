package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

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
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/review-and-submit")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
public class ScheduleAmendmentApplicationReviewAndSubmitController {

  private final LicenceService licenceService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;
  private final ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  public ScheduleAmendmentApplicationReviewAndSubmitController(
      LicenceService licenceService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService,
      ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService) {
    this.licenceService = licenceService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
    this.scheduleWorkProgrammeApplicationTaskListService = scheduleWorkProgrammeApplicationTaskListService;
  }

  @GetMapping
  public ModelAndView getReviewAndSubmit(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var submittable = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(
        scheduleWorkProgrammeApplicationDetail,
        user
    );

    return getReviewAndSubmitModelAndView(scheduleWorkProgrammeApplicationDetail, submittable, user);
  }

  private ModelAndView getReviewAndSubmitModelAndView(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      boolean isSubmittable,
      ServiceUserDetail user
  ) {
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/reviewAndSubmit")
        .addObject("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(applicationDetail.getId(), null, null)))
        .addObject("pageCaption", licenceService.getLicencePageCaption(
            scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail)))
        .addObject("summarySections", licenceScheduleSummarySectionService.getSummarySections(applicationDetail, null))
        .addObject("accordionId", applicationDetail.getId())
        .addObject("isSubmittable", isSubmittable)
        .addObject("userCanSubmit", scheduleWorkProgrammeApplicationService.userCanSubmitApplication(applicationDetail, user))
        .addObject("submitterRoleName", Role.APPLICATION_SUBMITTER.getName());
  }

  @PostMapping
  public ModelAndView submitApplication(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var submittable = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(
        scheduleWorkProgrammeApplicationDetail,
        user
    );

    if (!submittable) {
      return getReviewAndSubmitModelAndView(scheduleWorkProgrammeApplicationDetail, submittable, user);
    }

    var scheduleWorkProgrammeApplication
        = scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, user);

    return new ModelAndView("lms/licence/application/submissionConfirmation")
        .addObject("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null)))
        .addObject("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
        .addObject("applicationReference", scheduleWorkProgrammeApplication.getApplicationReference());
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/continuation-application/{licenceContinuationApplicationDetailId}/review-and-submit")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class ContinuationApplicationReviewAndSubmitController {

  private final LicenceService licenceService;
  private final LicenceContinuationService licenceContinuationService;
  private final ContinuationSummarySectionService continuationSummarySectionService;
  private final LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public ContinuationApplicationReviewAndSubmitController(
      LicenceService licenceService,
      LicenceContinuationService licenceContinuationService,
      ContinuationSummarySectionService continuationSummarySectionService,
      LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceService = licenceService;
    this.licenceContinuationService = licenceContinuationService;
    this.continuationSummarySectionService = continuationSummarySectionService;
    this.licenceContinuationApplicationTaskListService = licenceContinuationApplicationTaskListService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @GetMapping
  public ModelAndView getReviewAndSubmit(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var submittable = licenceContinuationApplicationTaskListService.isSubmittable(
        licenceContinuationApplicationDetail,
        user
    );

    return getReviewAndSubmitModelAndView(licenceContinuationApplicationDetail, submittable, user);
  }

  @PostMapping
  public ModelAndView submitApplication(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var submittable = licenceContinuationApplicationTaskListService.isSubmittable(
        licenceContinuationApplicationDetail,
        user
    );

    if (!submittable) {
      return getReviewAndSubmitModelAndView(licenceContinuationApplicationDetail, submittable, user);
    }

    var licenceContinuationApplication
        = licenceContinuationService.submitApplication(licenceContinuationApplicationDetail, user);

    return new ModelAndView("lms/licence/application/submissionConfirmation")
        .addObject("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null)))
        .addObject("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
        .addObject("applicationReference", licenceContinuationApplication.getApplicationReference());
  }

  private ModelAndView getReviewAndSubmitModelAndView(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      boolean isSubmittable,
      ServiceUserDetail user
  ) {

    var workProgrammeActivities = workProgrammeActivityService.getCurrentWorkProgrammeActivitiesViews(
        licenceContinuationService.getScheduleDetailFromApplicationDetail(licenceContinuationApplicationDetail)
    );

    return new ModelAndView("lms/licence/continuation/reviewAndSubmit")
        .addObject(
            "cancelUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
                licenceContinuationApplicationDetail.getId(),
                null,
                null
            )))
        .addObject(
            "pageCaption",
            licenceService.getLicencePageCaption(licenceContinuationService.getLicenceFromContinuationApplicationDetail(
                licenceContinuationApplicationDetail
            ))
        )
        .addObject(
            "summarySections",
            continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null
            )
        )
        .addObject(
            "accordionId",
            licenceContinuationApplicationDetail.getId()
        )
        .addObject(
            "isSubmittable",
            isSubmittable
        )
        .addObject(
            "userCanSubmit",
            licenceContinuationService.userCanSubmitApplication(licenceContinuationApplicationDetail, user)
        )
        .addObject(
            "submitterRoleName",
            Role.APPLICATION_SUBMITTER.getName()
        )
        .addObject(
            "workProgrammeActivities",
            workProgrammeActivities
        );
  }
}
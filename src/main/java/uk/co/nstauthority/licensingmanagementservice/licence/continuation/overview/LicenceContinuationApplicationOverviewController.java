package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/overview")
@ContinuationApplicationHasStatus(value = {
    LicenceContinuationApplicationStatus.SUBMITTED
})
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationApplicationOverviewController {

  private final LicenceContinuationService licenceContinuationService;
  private final LicenceContinuationApplicationOverviewService overviewService;
  private final ContinuationSummarySectionService continuationSummarySectionService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final LicenceContinuationActionService  licenceContinuationActionService;

  public LicenceContinuationApplicationOverviewController(
      LicenceContinuationService licenceContinuationService,
      LicenceContinuationApplicationOverviewService overviewService,
      ContinuationSummarySectionService continuationSummarySectionService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceContinuationActionService licenceContinuationActionService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.overviewService = overviewService;
    this.continuationSummarySectionService = continuationSummarySectionService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceContinuationActionService = licenceContinuationActionService;
  }

  @GetMapping
  public ModelAndView renderOverview(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    var licence = licenceContinuationService.getLicenceFromContinuationApplicationDetail(applicationDetail);
    var applicationContext = overviewService.getApplicationContext(applicationDetail, licence);
    var summarySections = continuationSummarySectionService.getSummarySections(applicationDetail, serviceUserDetail);
    var applicationActions = licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    List<WorkProgrammeActivityView> workProgrammeActivities = workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(
        applicationDetail.getLicenceContinuationApplication().getLicenceScheduleDetail()
    );

    return new ModelAndView("lms/licence/continuation/licenceContinuationApplicationOverview")
        .addObject("applicationContext", applicationContext)
        .addObject("summarySections", summarySections)
        .addObject("workProgrammeActivities", workProgrammeActivities)
        .addObject("accordionId", applicationDetail.getId())
        .addObject("showWorkProgrammeActivities", true)
        .addObject("applicationActions", applicationActions);
  }
}
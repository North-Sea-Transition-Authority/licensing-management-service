package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;

@Controller
@RequestMapping("licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/overview")
@ScheduleAmendmentApplicationHasStatus(value = {
    ScheduleWorkProgrammeApplicationStatus.SUBMITTED
})
@InvokingUserCanAccessScheduleApplication
public class ScheduleWorkProgrammeApplicationOverviewController {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final ScheduleWorkProgrammeApplicationOverviewService overviewService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;
  private final ScheduleWorkProgrammeApplicationActionService applicationActionService;

  public ScheduleWorkProgrammeApplicationOverviewController(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      ScheduleWorkProgrammeApplicationOverviewService overviewService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService,
      ScheduleWorkProgrammeApplicationActionService applicationActionService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.overviewService = overviewService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
    this.applicationActionService = applicationActionService;
  }

  @GetMapping
  public ModelAndView renderOverview(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    var licence = scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail);
    var applicationContext = overviewService.getApplicationContext(applicationDetail, licence);
    var summarySections = licenceScheduleSummarySectionService.getSummarySections(applicationDetail, serviceUserDetail);
    var applicationActions = applicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationOverview")
        .addObject("applicationContext", applicationContext)
        .addObject("summarySections", summarySections)
        .addObject("accordionId", applicationDetail.getId())
        .addObject("applicationActions", applicationActions);
  }
}

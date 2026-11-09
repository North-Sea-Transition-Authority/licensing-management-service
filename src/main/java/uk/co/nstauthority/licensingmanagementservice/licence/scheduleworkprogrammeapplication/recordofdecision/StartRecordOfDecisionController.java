package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/record-of-decision/start")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.DSP_UPLOADED)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION)
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
    pathVariable = "scheduleWorkProgrammeApplicationDetailId")
public class StartRecordOfDecisionController {

  public static final String PAGE_TITLE = "Record a decision";

  private final RecordOfDecisionTaskListService recordOfDecisionTaskListService;

  public StartRecordOfDecisionController(RecordOfDecisionTaskListService recordOfDecisionTaskListService) {
    this.recordOfDecisionTaskListService = recordOfDecisionTaskListService;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var application = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/startRecordOfDecision")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", application.getApplicationReference())
        .addObject("applicationTypeDisplayName",
            StringUtils.uncapitalize(application.getApplicationType().getDisplayName()))
        .addObject("startUrl", ReverseRouter.route(on(StartRecordOfDecisionController.class)
            .startDecision(scheduleWorkProgrammeApplicationDetailId, null, null)))
        .addObject("backUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(scheduleWorkProgrammeApplicationDetailId, null, null, null)));
  }

  @PostMapping
  public ModelAndView startDecision(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    recordOfDecisionTaskListService.startDecision(scheduleWorkProgrammeApplicationDetail, serviceUserDetail);

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }
}

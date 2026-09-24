package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp.UploadDspFileUsage;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/record-of-decision")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.DSP_UPLOADED)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION)
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
    pathVariable = "scheduleWorkProgrammeApplicationDetailId")
public class RecordOfDecisionTaskListController {

  public static final String PAGE_TITLE = "Record of decision";

  private final RecordOfDecisionTaskListService recordOfDecisionTaskListService;
  private final FileControllerHelperService fileControllerHelperService;

  public RecordOfDecisionTaskListController(
      RecordOfDecisionTaskListService recordOfDecisionTaskListService,
      FileControllerHelperService fileControllerHelperService
  ) {
    this.recordOfDecisionTaskListService = recordOfDecisionTaskListService;
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @GetMapping
  public ModelAndView getTaskList(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail serviceUserDetail) {

    var context = new RecordOfDecisionTaskListContext(scheduleWorkProgrammeApplicationDetail);
    var viewApplicationUrl = ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
        .renderOverview(scheduleWorkProgrammeApplicationDetailId, null, null));

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordOfDecisionTaskList")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("applicationContext",
            recordOfDecisionTaskListService.getApplicationContext(scheduleWorkProgrammeApplicationDetail))
        .addObject("viewApplicationUrl", viewApplicationUrl)
        .addObject("taskListSections",
            recordOfDecisionTaskListService.getTaskListSections(context, serviceUserDetail));

    recordOfDecisionTaskListService.getSignedDspSummaryItem(scheduleWorkProgrammeApplicationDetail)
        .ifPresent(summaryItem -> modelAndView.addObject("signedDspSummaryItem", summaryItem));

    var applicationReference = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication()
        .getApplicationReference();
    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addBreadcrumb(applicationReference, viewApplicationUrl)
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @GetMapping("/signed-dsp/{fileId}")
  public ResponseEntity<InputStreamResource> downloadSignedDsp(
      @PathVariable UUID fileId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail serviceUserDetail) {
    return fileControllerHelperService.download(
        fileId,
        () -> UploadDspFileUsage.fromApplication(scheduleWorkProgrammeApplicationDetail),
        serviceUserDetail);
  }
}

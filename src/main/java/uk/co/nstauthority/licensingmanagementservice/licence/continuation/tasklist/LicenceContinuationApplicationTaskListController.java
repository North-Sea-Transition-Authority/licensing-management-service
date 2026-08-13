package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDeleteController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleStateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/task-list")
@ContinuationApplicationHasStatus(value = ApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
    pathVariable = "licenceContinuationApplicationDetailId"
)
public class LicenceContinuationApplicationTaskListController {

  public static final String PAGE_TITLE = "Task list";

  private final LicenceService licenceService;
  private final LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService;
  private final LicenceContinuationService licenceContinuationService;
  private final LicenceScheduleStateService licenceScheduleStateService;

  public LicenceContinuationApplicationTaskListController(
      LicenceService licenceService,
      LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService,
      LicenceContinuationService licenceContinuationService,
      LicenceScheduleStateService licenceScheduleStateService
  ) {
    this.licenceService = licenceService;
    this.licenceContinuationApplicationTaskListService = licenceContinuationApplicationTaskListService;
    this.licenceContinuationService = licenceContinuationService;
    this.licenceScheduleStateService = licenceScheduleStateService;
  }

  @GetMapping
  public ModelAndView getTaskList(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {

    var sections = licenceContinuationApplicationTaskListService.getAllSections(
        licenceContinuationApplicationDetail,
        serviceUserDetail
    );
    var scheduleDetailFromApplicationDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(
        licenceContinuationApplicationDetail
    );

    var state = licenceScheduleStateService.getScheduleState(scheduleDetailFromApplicationDetail);

    String currentTermPhaseDisplay = licenceScheduleStateService.formatTermPhaseDisplay(
        state.currentTerm(),
        state.currentPhase()
    );
    String nextTermPhaseDisplay = licenceScheduleStateService.formatTermPhaseDisplay(
        state.nextTerm(),
        state.nextPhase()
    );

    var modelAndView = new ModelAndView("lms/licence/continuation/taskList")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("currentTermPhaseDisplay", currentTermPhaseDisplay)
        .addObject("nextTermPhaseDisplay", nextTermPhaseDisplay)
        .addObject("taskListSections", sections)
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(licenceContinuationApplicationDetail.getLicence()))
        .addObject("deleteLicenceContinuationApplicationUrl", ReverseRouter.route(on(
            LicenceContinuationApplicationDeleteController.class).renderForm(
                licenceContinuationApplicationDetailId,
                null,
                null)));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
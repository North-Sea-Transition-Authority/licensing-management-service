package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventView;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision.ContinuationDecisionSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision.ContinuationLetterFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleStateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/overview")
@ContinuationApplicationHasStatus(value = {
    ApplicationStatus.SUBMITTED,
    ApplicationStatus.DRAFT,
    ApplicationStatus.ISSUE_DECISION,
    ApplicationStatus.COMPLETE
})
@InvokingUserCanAccessContinuationApplication
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
    pathVariable = "licenceContinuationApplicationDetailId"
)
public class LicenceContinuationApplicationOverviewController {

  private static final String DEFAULT_TAB = "overview";

  private final LicenceContinuationService licenceContinuationService;
  private final LicenceContinuationApplicationOverviewService overviewService;
  private final ContinuationSummarySectionService continuationSummarySectionService;
  private final LicenceScheduleStateService licenceScheduleStateService;
  private final LicenceContinuationActionService  licenceContinuationActionService;
  private final FileControllerHelperService fileControllerHelperService;
  private final ContinuationDecisionSummarySectionService continuationIssuePdfSummarySectionService;
  private final RegulatorRoleService regulatorRoleService;
  private final CaseEventService caseEventService;
  private final LicenceContinuationApplicationTaskListService taskListService;

  public LicenceContinuationApplicationOverviewController(
      LicenceContinuationService licenceContinuationService,
      LicenceContinuationApplicationOverviewService overviewService,
      ContinuationSummarySectionService continuationSummarySectionService,
      LicenceScheduleStateService licenceScheduleStateService,
      LicenceContinuationActionService licenceContinuationActionService,
      FileControllerHelperService fileControllerHelperService,
      ContinuationDecisionSummarySectionService continuationIssuePdfSummarySectionService,
      RegulatorRoleService regulatorRoleService,
      CaseEventService caseEventService,
      LicenceContinuationApplicationTaskListService taskListService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.overviewService = overviewService;
    this.continuationSummarySectionService = continuationSummarySectionService;
    this.licenceScheduleStateService = licenceScheduleStateService;
    this.licenceContinuationActionService = licenceContinuationActionService;
    this.fileControllerHelperService = fileControllerHelperService;
    this.continuationIssuePdfSummarySectionService = continuationIssuePdfSummarySectionService;
    this.regulatorRoleService = regulatorRoleService;
    this.caseEventService = caseEventService;
    this.taskListService = taskListService;
  }

  @GetMapping
  public ModelAndView renderOverview(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail,
      ServiceUserDetail serviceUserDetail,
      @RequestParam(name = "tab", defaultValue = DEFAULT_TAB) OverviewTab tab
  ) {
    var availableTabs = regulatorRoleService.isRegulator(serviceUserDetail)
        ? List.of(OverviewTab.OVERVIEW, OverviewTab.LETTER, OverviewTab.CASE_EVENTS)
        : List.of(OverviewTab.OVERVIEW, OverviewTab.LETTER);

    if (!availableTabs.contains(tab)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "User with wuaId %d cannot view the %s tab".formatted(serviceUserDetail.wuaId(), tab.value()));
    }

    var applicationContext = overviewService.getApplicationContext(applicationDetail, applicationDetail.getLicence());
    var summarySections = continuationSummarySectionService.getSummarySections(applicationDetail, serviceUserDetail);
    var applicationActions = licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    var workProgrammeActivities = licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(
        licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail)
    );

    var isReviewer = regulatorRoleService.isContinuationReviewer(serviceUserDetail);
    var isDraft = applicationDetail.getStatus() == ApplicationStatus.DRAFT;
    var hasIncompleteSections = isDraft && !taskListService.isSubmittable(applicationDetail, serviceUserDetail);

    var letterIssueSummarySection = continuationIssuePdfSummarySectionService
        .getSummarySection(applicationDetail)
        .orElse(null);

    var modelAndView = new ModelAndView("lms/licence/continuation/licenceContinuationApplicationOverview")
        .addObject("applicationContext", applicationContext)
        .addObject("summarySections", summarySections)
        .addObject("letterIssueSummarySection", letterIssueSummarySection)
        .addObject("workProgrammeActivities", workProgrammeActivities)
        .addObject("accordionId", applicationDetail.getId())
        .addObject("isReviewer", isReviewer)
        .addObject("isDraft", isDraft)
        .addObject("hasIncompleteSections", hasIncompleteSections)
        .addObject("applicationActions", applicationActions)
        .addObject("availableTabs", availableTabs)
        .addObject("selectedTab", tab)
        .addObject("controllerUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(
                licenceContinuationApplicationDetailId,
                applicationDetail,
                serviceUserDetail,
                null
            ))
        );

    if (tab == OverviewTab.CASE_EVENTS) {
      var caseEventCards = caseEventService.getCaseEventViews(applicationDetail.getLicenceContinuationApplication())
          .stream()
          .map(CaseEventView::toSummaryCard)
          .toList();
      modelAndView.addObject("caseEventsSummaryItem", new SummaryItem(OverviewTab.CASE_EVENTS.label(), caseEventCards));
    }

    return modelAndView;
  }

  @GetMapping("/files/{fileId}")
  public ResponseEntity<InputStreamResource> downloadLetter(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail,
      @PathVariable UUID fileId,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.download(
        fileId,
        () -> ContinuationLetterFileUsages.fromApplication(applicationDetail),
        userDetail
    );
  }
}
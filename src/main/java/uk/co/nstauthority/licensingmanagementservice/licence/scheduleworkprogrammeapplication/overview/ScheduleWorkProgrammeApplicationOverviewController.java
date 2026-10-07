package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventView;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/overview")
@ScheduleAmendmentApplicationHasStatus(value = {
    ApplicationStatus.SUBMITTED, ApplicationStatus.DSP_UPLOADED, ApplicationStatus.ISSUE_DECISION
})
@InvokingUserCanAccessScheduleApplication
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
    pathVariable = "scheduleWorkProgrammeApplicationDetailId"
)
public class ScheduleWorkProgrammeApplicationOverviewController {

  private static final String DEFAULT_TAB = "overview";

  private final ScheduleWorkProgrammeApplicationOverviewService overviewService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;
  private final ScheduleWorkProgrammeApplicationActionService applicationActionService;
  private final RegulatorRoleService regulatorRoleService;
  private final CaseEventService caseEventService;

  public ScheduleWorkProgrammeApplicationOverviewController(
      ScheduleWorkProgrammeApplicationOverviewService overviewService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService,
      ScheduleWorkProgrammeApplicationActionService applicationActionService,
      RegulatorRoleService regulatorRoleService,
      CaseEventService caseEventService
  ) {
    this.overviewService = overviewService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
    this.applicationActionService = applicationActionService;
    this.regulatorRoleService = regulatorRoleService;
    this.caseEventService = caseEventService;
  }

  @GetMapping
  public ModelAndView renderOverview(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ServiceUserDetail serviceUserDetail,
      @RequestParam(name = "tab", defaultValue = DEFAULT_TAB) OverviewTab tab
  ) {
    var availableTabs = regulatorRoleService.isRegulator(serviceUserDetail)
        ? List.of(OverviewTab.OVERVIEW, OverviewTab.CASE_EVENTS)
        : List.of(OverviewTab.OVERVIEW);

    if (!availableTabs.contains(tab)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "User with wuaId %d cannot view the %s tab".formatted(serviceUserDetail.wuaId(), tab.value()));
    }

    var licence = applicationDetail.getLicence();
    var applicationContext = overviewService.getApplicationContext(applicationDetail, licence);
    var summarySections = licenceScheduleSummarySectionService.getSummarySections(applicationDetail, serviceUserDetail);
    var applicationActions = applicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationOverview")
        .addObject("applicationContext", applicationContext)
        .addObject("summarySections", summarySections)
        .addObject("accordionId", applicationDetail.getId())
        .addObject("applicationActions", applicationActions)
        .addObject("availableTabs", availableTabs)
        .addObject("selectedTab", tab)
        .addObject("controllerUrl",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class).renderOverview(
                scheduleWorkProgrammeApplicationDetailId,
                applicationDetail,
                serviceUserDetail,
                null
            ))
        );

    if (tab == OverviewTab.CASE_EVENTS) {
      var caseEventCards = caseEventService.getCaseEventViews(applicationDetail.getScheduleWorkProgrammeApplication())
          .stream()
          .map(CaseEventView::toSummaryCard)
          .toList();
      modelAndView.addObject("caseEventsSummaryItem", new SummaryItem(OverviewTab.CASE_EVENTS.label(), caseEventCards));
    }

    return modelAndView;
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;

// Renders the work area's proposed "My work"/"All work" tabs (LMS1-649), once per persona that can hold them.
@Controller
@RequestMapping(WorkAreaMockupController.PAGE_URL)
@Profile("mockups")
public class WorkAreaMockupController {

  public static final String PAGE_URL = "/mockups/work-area";

  private record Persona(String key, String label, String myWorkUrl, String allWorkUrl) {
    FdsBackendTab myWorkTab() {
      return new FdsBackendTab("My work", "my-work", myWorkUrl);
    }

    FdsBackendTab allWorkTab() {
      return new FdsBackendTab("All work", "all-work", allWorkUrl);
    }

    List<FdsBackendTab> tabs() {
      return List.of(myWorkTab(), allWorkTab());
    }
  }

  private static final Persona STEWARD =
      new Persona("steward", "Steward", PAGE_URL + "/steward", PAGE_URL + "/steward/all-work");
  private static final Persona CASE_MANAGER =
      new Persona("case-manager", "Case manager", PAGE_URL + "/case-manager", PAGE_URL + "/case-manager/all-work");
  private static final Persona CONTINUATION_REVIEWER = new Persona("continuation-reviewer", "Continuation reviewer",
      PAGE_URL + "/continuation-reviewer", PAGE_URL + "/continuation-reviewer/all-work");
  private static final Persona SCHEDULE_ADMINISTRATOR = new Persona("schedule-administrator", "Schedule administrator",
      PAGE_URL + "/schedule-administrator", PAGE_URL + "/schedule-administrator/all-work");

  private static final List<Persona> PERSONAS =
      List.of(STEWARD, CASE_MANAGER, CONTINUATION_REVIEWER, SCHEDULE_ADMINISTRATOR);

  @GetMapping("/steward")
  ModelAndView renderStewardMyWork() {
    return renderTab(STEWARD, STEWARD.myWorkTab(), WorkAreaMockupData.getStewardMyWorkItems(STEWARD.myWorkUrl()));
  }

  @GetMapping("/steward/all-work")
  ModelAndView renderStewardAllWork() {
    return renderTab(STEWARD, STEWARD.allWorkTab(), WorkAreaMockupData.getAllWorkItems(STEWARD.allWorkUrl()));
  }

  @GetMapping("/case-manager")
  ModelAndView renderCaseManagerMyWork() {
    return renderTab(
        CASE_MANAGER, CASE_MANAGER.myWorkTab(), WorkAreaMockupData.getCaseManagerMyWorkItems(CASE_MANAGER.myWorkUrl()));
  }

  @GetMapping("/case-manager/all-work")
  ModelAndView renderCaseManagerAllWork() {
    return renderTab(
        CASE_MANAGER, CASE_MANAGER.allWorkTab(), WorkAreaMockupData.getAllWorkItems(CASE_MANAGER.allWorkUrl()));
  }

  // No per-case reviewer assignment, so My work and All work show the same list.
  @GetMapping("/continuation-reviewer")
  ModelAndView renderContinuationReviewerMyWork() {
    return renderTab(CONTINUATION_REVIEWER, CONTINUATION_REVIEWER.myWorkTab(),
        WorkAreaMockupData.getContinuationReviewerItems(CONTINUATION_REVIEWER.myWorkUrl()));
  }

  @GetMapping("/continuation-reviewer/all-work")
  ModelAndView renderContinuationReviewerAllWork() {
    return renderTab(CONTINUATION_REVIEWER, CONTINUATION_REVIEWER.allWorkTab(),
        WorkAreaMockupData.getContinuationReviewerItems(CONTINUATION_REVIEWER.allWorkUrl()));
  }

  // Not licence-type scoped and no per-draft assignment, so My work and All work show the same list.
  @GetMapping("/schedule-administrator")
  ModelAndView renderScheduleAdministratorMyWork() {
    return renderTab(SCHEDULE_ADMINISTRATOR, SCHEDULE_ADMINISTRATOR.myWorkTab(),
        WorkAreaMockupData.getScheduleAdministratorItems(SCHEDULE_ADMINISTRATOR.myWorkUrl()));
  }

  @GetMapping("/schedule-administrator/all-work")
  ModelAndView renderScheduleAdministratorAllWork() {
    return renderTab(SCHEDULE_ADMINISTRATOR, SCHEDULE_ADMINISTRATOR.allWorkTab(),
        WorkAreaMockupData.getScheduleAdministratorItems(SCHEDULE_ADMINISTRATOR.allWorkUrl()));
  }

  private ModelAndView renderTab(Persona persona, FdsBackendTab currentTab, List<SearchResultItem> workAreaItems) {
    var form = new WorkAreaFilterForm();
    form.clearFilter();

    var mockupPersonas = PERSONAS.stream()
        .map(p -> Map.of("key", p.key(), "label", p.label(), "url", p.myWorkUrl()))
        .toList();

    return new ModelAndView("lms/mockups/workarea/workArea")
        .addObject("pageTitle", "Work area")
        .addObject("currentPersona", persona.key())
        .addObject("mockupPersonas", mockupPersonas)
        .addObject("tabs", persona.tabs())
        .addObject("currentTab", currentTab)
        .addObject("workAreaItems", workAreaItems)
        .addObject("form", form)
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("applicationTypes", DisplayableEnumOptionUtil.getDisplayableOptions(ApplicationType.class))
        .addObject("applicationStatuses",
            DisplayableEnumOptionUtil.getDisplayableOptions(ApplicationStatus.getSearchableStatuses()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Map.of())
        .addObject("clearFilterUrl", currentTab.url());
  }
}

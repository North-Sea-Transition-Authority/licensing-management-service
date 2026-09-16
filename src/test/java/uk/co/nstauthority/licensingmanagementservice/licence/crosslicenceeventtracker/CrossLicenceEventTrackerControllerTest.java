package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = CrossLicenceEventTrackerController.class)
class CrossLicenceEventTrackerControllerTest extends AbstractControllerTest {

  private static final String RENDER_EVENT_TRACKER_ROUTE =
      ReverseRouter.route(on(CrossLicenceEventTrackerController.class).renderEventTracker(null, null));

  private static final String CLEAR_FILTERS_ROUTE =
      ReverseRouter.route(on(CrossLicenceEventTrackerController.class).clearEventTrackerFilters(null, null));

  private static final String FILTER_EVENT_TRACKER_ROUTE =
      ReverseRouter.route(on(CrossLicenceEventTrackerController.class).filterEventTracker(null, null, null, null));

  @MockitoBean
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @MockitoBean
  private RegulatorRoleService regulatorRoleService;

  @MockitoBean
  private EventTrackerFormValidator eventTrackerFormValidator;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  private SortableTableView eventTrackerTable;

  @BeforeEach
  void setUp() {
    eventTrackerTable = SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Steward")
        .build();
    when(crossLicenceEventTrackerService.getEventTrackerTable(any(), any())).thenReturn(eventTrackerTable);
  }

  @Test
  void renderEventTracker_whenRegulator_rendersExpectedViewAndModel() throws Exception {
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    mockMvc.perform(
            get(RENDER_EVENT_TRACKER_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/crosslicenceeventtracker/eventTracker"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("form", instanceOf(EventTrackerForm.class)))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))))
        .andExpect(model().attribute("preSelectedLicenseeOrgUnit", Collections.emptyMap()))
        .andExpect(model().attribute("licenseeGroupOrgUnitUrl", SearchSelectorService.route(
            on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null))))
        .andExpect(model().attribute("preSelectedLicenseeGroupOrgUnit", Collections.emptyMap()))
        .andExpect(model().attribute("isRegulatorUser", true))
        .andExpect(model().attribute("requestTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class)))
        .andExpect(model().attribute("eventStatuses",
            DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class)))
        .andExpect(model().attribute("eventTrackerTableJson", eventTrackerTable.toString()))
        .andExpect(model().attribute("clearFilterUrl", CLEAR_FILTERS_ROUTE));

    verify(crossLicenceEventTrackerService).getEventTrackerTable(any(), eq(regulatorUser));
  }

  @Test
  void renderEventTracker_whenIndustryUser_thenLicenseeGroupFilterIsNotShown() throws Exception {
    var industryUser = ServiceUserDetailTestUtil.newBuilder().withWuaId(300L).build();
    when(regulatorRoleService.isRegulator(industryUser)).thenReturn(false);

    mockMvc.perform(
            get(RENDER_EVENT_TRACKER_ROUTE)
                .with(user(industryUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isRegulatorUser", false));

    verify(crossLicenceEventTrackerService).getEventTrackerTable(any(), eq(industryUser));
  }

  @Test
  void renderEventTracker_whenLicenseeSelected_thenPreselectedLicenseeOrgUnitIncluded() throws Exception {
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    var form = new EventTrackerForm();
    form.setLicenseeOrgUnitId(700);
    var filterSession = new EventTrackerFilterSession(form);

    when(organisationUnitQueryService.getOrganisationUnitSelectOption("700"))
        .thenReturn(Map.of("700", "Licensee Ltd"));

    mockMvc.perform(
            get(RENDER_EVENT_TRACKER_ROUTE)
                .flashAttr("eventTrackerFilterSession", filterSession)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("preSelectedLicenseeOrgUnit", Map.of("700", "Licensee Ltd")));
  }

  @Test
  void filterEventTracker_updatesSessionAndRedirects() throws Exception {
    var filterSession = new EventTrackerFilterSession(new EventTrackerForm());

    mockMvc.perform(
            post(FILTER_EVENT_TRACKER_ROUTE)
                .param("licenceTypes", LicenceType.CARBON_STORAGE.name())
                .flashAttr("eventTrackerFilterSession", filterSession)
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_EVENT_TRACKER_ROUTE));

    assertThat(filterSession.getFilterForm().getLicenceTypes()).containsExactly(LicenceType.CARBON_STORAGE.name());
  }

  @Test
  void filterEventTracker_whenValidationFails_thenReRendersWithErrorsAndDoesNotUpdateSession() throws Exception {
    var existingForm = new EventTrackerForm();
    var filterSession = new EventTrackerFilterSession(existingForm);

    Answer<Void> rejectFromDate = invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("fromDate", "fromDate.invalid", "Event from must be a real date in the format dd/mm/yyyy");
      return null;
    };
    doAnswer(rejectFromDate).when(eventTrackerFormValidator).isValid(any(), any());

    mockMvc.perform(
            post(FILTER_EVENT_TRACKER_ROUTE)
                .param("fromDate", "not-a-date")
                .flashAttr("eventTrackerFilterSession", filterSession)
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/crosslicenceeventtracker/eventTracker"))
        .andExpect(model().attributeHasFieldErrors("form", "fromDate"));

    assertThat(filterSession.getFilterForm()).isSameAs(existingForm);
  }

  @Test
  void clearEventTrackerFilters_redirectsToRenderEventTracker() throws Exception {
    mockMvc.perform(
            get(CLEAR_FILTERS_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_EVENT_TRACKER_ROUTE));
  }
}

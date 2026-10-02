package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionController.FILTER_SESSION_ATTRIBUTE;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceSummaryCardView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterSession;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = {
    LicencePositionController.class,
    TabbedLicencePageService.class,
    LicenceOverviewService.class,
    LicenceTimelinePositionTab.class
})
@ActiveProfiles("test")
class LicencePositionControllerTest extends AbstractControllerTest {

  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("REF-1")
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Integer ORGANISATION_ID = 10;
  private static final Integer OTHER_ORGANISATION_ID = 11;
  private static final LicenceTimelineFilterOptions FILTER_OPTIONS = new LicenceTimelineFilterOptions(
      Map.of(LicenceOperation.SET_EQUITY, "Set equity"),
      Map.of(String.valueOf(ORGANISATION_ID), "Shell plc", String.valueOf(OTHER_ORGANISATION_ID), "BP plc")
  );
  private static final LicenceTimelineFilter SET_EQUITY_FILTER =
      new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of());

  @Autowired
  private LicenceOverviewService licenceOverviewService;

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
    when(licenceSummaryCardService.getLicenceSummaryCardView(LICENCE))
        .thenReturn(new LicenceSummaryCardView("Extant", List.of(), false, null));
  }

  @Test
  void renderLicencePositionTimeline_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(timelineUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLicencePositionTimeline_whenFilterInSession_thenFilteredLatestPositionIsRendered() throws Exception {
    var filter = new LicenceTimelineFilter(
        Set.of(LicenceOperation.SET_EQUITY),
        Set.of(ORGANISATION_ID, OTHER_ORGANISATION_ID)
    );
    var pageView = LicencePositionPageView.noMatchingPositions(LICENCE.getType(), FILTER_OPTIONS);

    when(licencePositionViewService.getLatestPositionPageView(LICENCE, filter)).thenReturn(pageView);

    mockMvc.perform(get(timelineUrl())
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(LICENCE.getLicenceReference(), filter))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositions"),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("form", allOf(
                hasProperty("changeTypes", contains(LicenceOperation.SET_EQUITY)),
                hasProperty("organisationIds", containsInAnyOrder(ORGANISATION_ID, OTHER_ORGANISATION_ID))
            )),
            content().string(allOf(
                containsString("<option value=\"10\" selected>Shell plc</option>"),
                containsString("<option value=\"11\" selected>BP plc</option>")
            )),
            model().attribute("filterUrl", timelineUrl()),
            model().attribute("clearFilterUrl", clearFiltersUrl())
        );
  }

  @Test
  void renderLicencePositionTimeline_whenFilterIsForAnotherLicence_thenUnfilteredLatestPositionIsRendered()
      throws Exception {
    var pageView = LicencePositionPageView.empty();

    when(licencePositionViewService.getLatestPositionPageView(LICENCE, LicenceTimelineFilter.empty()))
        .thenReturn(pageView);

    mockMvc.perform(get(timelineUrl())
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession("OTHER-REF", SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositions"),
            model().attribute("licenceOverviewView", licenceOverviewService.getLicenceOverviewView(LICENCE)),
            model().attribute("licencePositionPageView", pageView)
        );
  }

  @Test
  void renderLicencePositionTimeline_whenNotificationBannerIsFlashed_thenItIsRenderedOnTheLatestPosition()
      throws Exception {
    var banner = NotificationBanner.newSuccessBanner().withHeadingContent("Correction COR-1 applied").build();

    when(licencePositionViewService.getLatestPositionPageView(LICENCE, LicenceTimelineFilter.empty()))
        .thenReturn(LicencePositionPageView.empty());

    mockMvc.perform(get(timelineUrl())
            .flashAttr("notificationBanner", banner)
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("notificationBanner", banner)
        );
  }

  @Test
  void filterLicencePositionTimeline_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(timelineUrl()).with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void filterLicencePositionTimeline_whenFiltersSubmitted_thenOnlyThoseOnTheLicenceAreHeldForTheLicence()
      throws Exception {
    var filterSession = new LicenceTimelineFilterSession();

    when(licencePositionViewService.getFilterOptions(LICENCE)).thenReturn(FILTER_OPTIONS);

    mockMvc.perform(post(timelineUrl())
            .param("changeTypes", LicenceOperation.SET_EQUITY, "not-a-change-type")
            .param("organisationIds", String.valueOf(ORGANISATION_ID), "999")
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(timelineUrl()));

    assertThat(filterSession.getFilter(LICENCE.getLicenceReference()))
        .isEqualTo(new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of(ORGANISATION_ID)));
  }

  @Test
  void filterLicencePositionTimeline_whenNothingOnTheLicenceSubmitted_thenLicenceFilterIsRemoved() throws Exception {
    var filterSession = filterSession(LICENCE.getLicenceReference(), SET_EQUITY_FILTER);

    when(licencePositionViewService.getFilterOptions(LICENCE)).thenReturn(FILTER_OPTIONS);

    mockMvc.perform(post(timelineUrl())
            .param("organisationIds", "999")
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(timelineUrl()));

    assertThat(filterSession.isEmpty()).isTrue();
  }

  @Test
  void clearLicencePositionTimelineFilters_whenOtherLicencesFiltered_thenOnlyThisLicenceFilterIsCleared()
      throws Exception {
    var otherLicenceFilter = new LicenceTimelineFilter(Set.of(), Set.of(ORGANISATION_ID));
    var filterSession = filterSession(LICENCE.getLicenceReference(), SET_EQUITY_FILTER);
    filterSession.update("OTHER-REF", otherLicenceFilter);

    mockMvc.perform(get(clearFiltersUrl())
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(user(regulatorUser)))
        .andExpectAll(
            redirectedUrl(timelineUrl()),
            request().sessionAttribute(FILTER_SESSION_ATTRIBUTE, filterSession)
        );

    assertThat(filterSession.getFilter(LICENCE.getLicenceReference())).isEqualTo(LicenceTimelineFilter.empty());
    assertThat(filterSession.getFilter("OTHER-REF")).isEqualTo(otherLicenceFilter);
  }

  @Test
  void clearLicencePositionTimelineFilters_whenNoOtherLicencesFiltered_thenFilterIsRemovedFromSession()
      throws Exception {
    mockMvc.perform(get(clearFiltersUrl())
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(LICENCE.getLicenceReference(), SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpectAll(
            redirectedUrl(timelineUrl()),
            request().sessionAttributeDoesNotExist(FILTER_SESSION_ATTRIBUTE)
        );
  }

  @Test
  void renderLicencePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(LICENCE, POSITION_ID, null, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLicencePosition() throws Exception {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var pageView = new LicencePositionPageView(
        List.of(new LicencePositionTimelineView(
            POSITION_ID, "url1", "REF-2", "5 June 2026", List.of("licenseeNames"), false, null, false, null, null, false, null, null, false)),
        position.getFormattedPositionDate(),
        position.getLicence().getLicenceReference(),
        List.of(),
        new LicencePositionStateView(
            new AdministratorStateView("admin organisation"),
            new LicenseeStateView(List.of("licenseeNames")),
            List.of(),
            List.of()
        ),
        false,
        POSITION_ID,
        false,
        LicencePositionPageView.Actions.none(),
        LICENCE.getType(),
        List.of(),
        FILTER_OPTIONS,
        true
    );

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionViewService.getPositionPageView(position, SET_EQUITY_FILTER)).thenReturn(pageView);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(LICENCE, POSITION_ID, null, null)))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(LICENCE.getLicenceReference(), SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositions"),
            // the timeline tab shows the same header as every other licence tab
            model().attribute("licenceOverviewView", licenceOverviewService.getLicenceOverviewView(LICENCE)),
            model().attribute("licencePositionPageView", pageView)
        );
  }

  private static String timelineUrl() {
    return ReverseRouter.route(on(LicencePositionController.class).renderLicencePositionTimeline(LICENCE, null, null));
  }

  private static String clearFiltersUrl() {
    return ReverseRouter.route(on(LicencePositionController.class)
        .clearLicencePositionTimelineFilters(LICENCE, null, null));
  }

  private static LicenceTimelineFilterSession filterSession(String licenceReference, LicenceTimelineFilter filter) {
    var filterSession = new LicenceTimelineFilterSession();
    filterSession.update(licenceReference, filter);
    return filterSession;
  }
}

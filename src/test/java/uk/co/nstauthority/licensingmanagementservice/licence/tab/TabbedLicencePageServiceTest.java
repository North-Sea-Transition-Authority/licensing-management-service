package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewView;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

@ExtendWith(MockitoExtension.class)
class TabbedLicencePageServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicenceReference("CS001")
      .build();

  private static final LicenceOverviewService LICENCE_OVERVIEW_SERVICE = new LicenceOverviewService();

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  // display orders mirror the real tabs, so the timeline precedes the schedule
  private static final LicenceTab TIMELINE_TAB =
      new TestLicenceTab("Timeline", 1, ReleaseFeature.VIEW_LICENCE_TIMELINE);
  private static final LicenceTab SCHEDULE_TAB =
      new TestLicenceTab("Schedule", 2, ReleaseFeature.VIEW_LICENCE_SCHEDULE);

  @Mock
  private LicenceActionService licenceActionService;

  @Test
  void hydrateModel_assertTabsSortedByDisplayOrder() {
    // display order deliberately opposes alphabetical order of the display names
    var firstTab = new TestLicenceTab("Zebra", 1, ReleaseFeature.VIEW_LICENCE_SCHEDULE);
    var secondTab = new TestLicenceTab("Aardvark", 2, ReleaseFeature.VIEW_LICENCE_SCHEDULE);

    var tabbedLicencePageService = serviceWithTabs(List.of(secondTab, firstTab));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, firstTab, USER);

    var context = new LicenceTabContext(LICENCE);

    assertThat(modelAndView.getModel()).extractingByKey("tabs")
        .isEqualTo(List.of(
            FdsBackendTab.from(firstTab, context),
            FdsBackendTab.from(secondTab, context)
        ));
  }

  @Test
  void hydrateModel_whenTabsReleasePhaseIsNotEnabled_assertTabExcluded() {
    var tabbedLicencePageService = serviceWithTabsAndProfiles(List.of(SCHEDULE_TAB, TIMELINE_TAB), "enable-lms1");

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    // the timeline tab's feature belongs to LMS2, which is not switched on
    assertThat(modelAndView.getModel()).extractingByKey("tabs")
        .isEqualTo(List.of(FdsBackendTab.from(SCHEDULE_TAB, new LicenceTabContext(LICENCE))));
  }

  @Test
  void hydrateModel_whenNoReleasePhasesEnabled_assertNoTabs() {
    var tabbedLicencePageService = serviceWithTabsAndProfiles(List.of(SCHEDULE_TAB, TIMELINE_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    assertThat(modelAndView.getModel()).containsEntry("tabs", List.of());
  }

  @Test
  void hydrateModel_assertCurrentTab() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB, TIMELINE_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, TIMELINE_TAB, USER);

    assertThat(modelAndView.getModel()).extractingByKey("currentTab")
        .isEqualTo(FdsBackendTab.from(TIMELINE_TAB, new LicenceTabContext(LICENCE)));
  }

  @Test
  void hydrateModel_assertTabsBuiltWithContextForGivenLicence() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    assertThat(modelAndView.getModel()).extractingByKey("tabs")
        .isEqualTo(List.of(new FdsBackendTab("Schedule", "schedule", "/tab/schedule/1")));
  }

  @Test
  void hydrateModel_assertLicenceActions() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB));

    var topLevelLicenceActions = List.of(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(LICENCE));
    var currentTabLicenceActions = List.of(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(LICENCE));

    when(licenceActionService.getTopLevelLicenceActionItems(LICENCE, USER)).thenReturn(topLevelLicenceActions);
    when(licenceActionService.getLicenceActionItemsForTab(LICENCE, USER, SCHEDULE_TAB)).thenReturn(currentTabLicenceActions);

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    assertThat(modelAndView.getModel())
        .containsEntry("topLevelLicenceActions", topLevelLicenceActions)
        .containsEntry("currentTabLicenceActions", currentTabLicenceActions);
  }

  @Test
  void hydrateModel_assertLicenceOverviewView() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB, TIMELINE_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, TIMELINE_TAB, USER);

    // every tab renders the same header, so it does not vary with the current tab
    assertThat(modelAndView.getModel()).extractingByKey("licenceOverviewView")
        .isEqualTo(LICENCE_OVERVIEW_SERVICE.getLicenceOverviewView(LICENCE))
        .isEqualTo(new LicenceOverviewView(
            "CS001",
            LicenceType.CARBON_STORAGE.getDisplayName(),
            "https://www.nstauthority.co.uk/regulatory-information/carbon-storage/carbon-storage-public-register/?section=CS001"
        ));
  }

  @Test
  void hydrateModel_assertActionsRetrievedForCurrentTabOnly() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB, TIMELINE_TAB));

    tabbedLicencePageService.hydrateModel(new ModelAndView("some/view"), LICENCE, TIMELINE_TAB, USER);

    verify(licenceActionService).getLicenceActionItemsForTab(LICENCE, USER, TIMELINE_TAB);
    verify(licenceActionService).getTopLevelLicenceActionItems(LICENCE, USER);
  }

  @Test
  void hydrateModel_whenNoTabsRegistered_assertNoTabs() {
    var tabbedLicencePageService = serviceWithTabs(List.of());

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    assertThat(modelAndView.getModel()).containsEntry("tabs", List.of());
  }

  @Test
  void hydrateModel_whenCurrentTabIsNotARegisteredTab_assertCurrentTabStillAdded() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, TIMELINE_TAB, USER);

    var context = new LicenceTabContext(LICENCE);

    assertThat(modelAndView.getModel())
        .containsEntry("tabs", List.of(FdsBackendTab.from(SCHEDULE_TAB, context)))
        .containsEntry("currentTab", FdsBackendTab.from(TIMELINE_TAB, context));
  }

  @Test
  void hydrateModel_assertExistingModelAttributesRetained() {
    var tabbedLicencePageService = serviceWithTabs(List.of(SCHEDULE_TAB));

    var modelAndView = new ModelAndView("some/view").addObject("existingAttribute", "existingValue");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, SCHEDULE_TAB, USER);

    assertThat(modelAndView.getViewName()).isEqualTo("some/view");
    assertThat(modelAndView.getModel())
        .containsEntry("existingAttribute", "existingValue")
        .containsKeys("licenceOverviewView", "tabs", "currentTab", "topLevelLicenceActions", "currentTabLicenceActions");
  }

  @Test
  void getDefaultTabUrl_whenOnlyLms1Enabled_assertScheduleTabUrl() {
    var tabbedLicencePageService = serviceWithTabsAndProfiles(List.of(TIMELINE_TAB, SCHEDULE_TAB), "enable-lms1");

    assertThat(tabbedLicencePageService.getDefaultTabUrl(LICENCE))
        .isEqualTo(SCHEDULE_TAB.url(new LicenceTabContext(LICENCE)));
  }

  @Test
  void getDefaultTabUrl_whenLms2Enabled_assertTimelineTabUrl() {
    var tabbedLicencePageService = serviceWithTabs(List.of(TIMELINE_TAB, SCHEDULE_TAB));

    assertThat(tabbedLicencePageService.getDefaultTabUrl(LICENCE))
        .isEqualTo(TIMELINE_TAB.url(new LicenceTabContext(LICENCE)));
  }

  @Test
  void getDefaultTabUrl_whenNoTabsEnabled_assertThrows() {
    var tabbedLicencePageService = serviceWithTabsAndProfiles(List.of(TIMELINE_TAB, SCHEDULE_TAB));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> tabbedLicencePageService.getDefaultTabUrl(LICENCE))
        .withMessageContaining("No licence tabs are enabled");
  }

  /** All release phases switched on, so tabs are filtered only by what the test explicitly registers. */
  private TabbedLicencePageService serviceWithTabs(List<LicenceTab> licenceTabs) {
    return serviceWithTabsAndProfiles(licenceTabs, "enable-lms1", "enable-lms2");
  }

  private TabbedLicencePageService serviceWithTabsAndProfiles(List<LicenceTab> licenceTabs, String... activeProfiles) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(activeProfiles);

    return new TabbedLicencePageService(
        licenceActionService,
        LICENCE_OVERVIEW_SERVICE,
        new FeatureFlagService(environment),
        licenceTabs
    );
  }

  private record TestLicenceTab(String displayName, int displayOrder, ReleaseFeature releaseFeature)
      implements LicenceTab {

    @Override
    public ReleaseFeature getReleaseFeature() {
      return releaseFeature;
    }

    @Override
    public String url(LicenceTabContext context) {
      return "/tab/%s/%d".formatted(anchor(), context.licence().getId());
    }
  }
}

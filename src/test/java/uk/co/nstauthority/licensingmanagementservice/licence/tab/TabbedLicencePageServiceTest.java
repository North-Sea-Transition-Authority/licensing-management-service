package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;

@ExtendWith(MockitoExtension.class)
class TabbedLicencePageServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .build();

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  private static final LicenceTab OVERVIEW_TAB = new TestLicenceTab("Overview");
  private static final LicenceTab TIMELINE_TAB = new TestLicenceTab("Timeline");

  @Mock
  private LicenceActionService licenceActionService;

  @Test
  void hydrateModel_assertTabsSortedByDisplayName() {
    var tabbedLicencePageService = new TabbedLicencePageService(
        licenceActionService,
        List.of(TIMELINE_TAB, OVERVIEW_TAB)
    );

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, OVERVIEW_TAB, USER);

    var context = new LicenceTabContext(LICENCE);

    assertThat(modelAndView.getModel()).extractingByKey("tabs")
        .isEqualTo(List.of(
            FdsBackendTab.from(OVERVIEW_TAB, context),
            FdsBackendTab.from(TIMELINE_TAB, context)
        ));
  }

  @Test
  void hydrateModel_assertCurrentTab() {
    var tabbedLicencePageService = new TabbedLicencePageService(
        licenceActionService,
        List.of(OVERVIEW_TAB, TIMELINE_TAB)
    );

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, TIMELINE_TAB, USER);

    assertThat(modelAndView.getModel()).extractingByKey("currentTab")
        .isEqualTo(FdsBackendTab.from(TIMELINE_TAB, new LicenceTabContext(LICENCE)));
  }

  @Test
  void hydrateModel_assertTabsBuiltWithContextForGivenLicence() {
    var tabbedLicencePageService = new TabbedLicencePageService(licenceActionService, List.of(OVERVIEW_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, OVERVIEW_TAB, USER);

    assertThat(modelAndView.getModel()).extractingByKey("tabs")
        .isEqualTo(List.of(new FdsBackendTab("Overview", "overview", "/tab/overview/1")));
  }

  @Test
  void hydrateModel_assertLicenceActions() {
    var tabbedLicencePageService = new TabbedLicencePageService(licenceActionService, List.of(OVERVIEW_TAB));

    var topLevelLicenceActions = List.of(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(LICENCE));
    var currentTabLicenceActions = List.of(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(LICENCE));

    when(licenceActionService.getTopLevelLicenceActionItems(LICENCE, USER)).thenReturn(topLevelLicenceActions);
    when(licenceActionService.getLicenceActionItemsForTab(LICENCE, USER, OVERVIEW_TAB)).thenReturn(currentTabLicenceActions);

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, OVERVIEW_TAB, USER);

    assertThat(modelAndView.getModel())
        .containsEntry("topLevelLicenceActions", topLevelLicenceActions)
        .containsEntry("currentTabLicenceActions", currentTabLicenceActions);
  }

  @Test
  void hydrateModel_assertActionsRetrievedForCurrentTabOnly() {
    var tabbedLicencePageService = new TabbedLicencePageService(
        licenceActionService,
        List.of(OVERVIEW_TAB, TIMELINE_TAB)
    );

    tabbedLicencePageService.hydrateModel(new ModelAndView("some/view"), LICENCE, TIMELINE_TAB, USER);

    verify(licenceActionService).getLicenceActionItemsForTab(LICENCE, USER, TIMELINE_TAB);
    verify(licenceActionService).getTopLevelLicenceActionItems(LICENCE, USER);
  }

  @Test
  void hydrateModel_whenNoTabsRegistered_assertNoTabs() {
    var tabbedLicencePageService = new TabbedLicencePageService(licenceActionService, List.of());

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, OVERVIEW_TAB, USER);

    assertThat(modelAndView.getModel()).containsEntry("tabs", List.of());
  }

  @Test
  void hydrateModel_whenCurrentTabIsNotARegisteredTab_assertCurrentTabStillAdded() {
    var tabbedLicencePageService = new TabbedLicencePageService(licenceActionService, List.of(OVERVIEW_TAB));

    var modelAndView = new ModelAndView("some/view");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, TIMELINE_TAB, USER);

    var context = new LicenceTabContext(LICENCE);

    assertThat(modelAndView.getModel())
        .containsEntry("tabs", List.of(FdsBackendTab.from(OVERVIEW_TAB, context)))
        .containsEntry("currentTab", FdsBackendTab.from(TIMELINE_TAB, context));
  }

  @Test
  void hydrateModel_assertExistingModelAttributesRetained() {
    var tabbedLicencePageService = new TabbedLicencePageService(licenceActionService, List.of(OVERVIEW_TAB));

    var modelAndView = new ModelAndView("some/view").addObject("existingAttribute", "existingValue");

    tabbedLicencePageService.hydrateModel(modelAndView, LICENCE, OVERVIEW_TAB, USER);

    assertThat(modelAndView.getViewName()).isEqualTo("some/view");
    assertThat(modelAndView.getModel())
        .containsEntry("existingAttribute", "existingValue")
        .containsKeys("tabs", "currentTab", "topLevelLicenceActions", "currentTabLicenceActions");
  }

  private record TestLicenceTab(String displayName) implements LicenceTab {

    @Override
    public String url(LicenceTabContext context) {
      return "/tab/%s/%d".formatted(anchor(), context.licence().getId());
    }
  }
}

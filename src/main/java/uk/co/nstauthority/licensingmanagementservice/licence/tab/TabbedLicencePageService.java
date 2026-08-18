package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;

@Service
public class TabbedLicencePageService {

  private final LicenceActionService licenceActionService;
  private final List<LicenceTab> enabledLicenceTabs;

  TabbedLicencePageService(
      LicenceActionService licenceActionService,
      FeatureFlagService featureFlagService,
      List<LicenceTab> licenceTabs
  ) {
    this.licenceActionService = licenceActionService;
    this.enabledLicenceTabs = featureFlagService.filterEnabled(licenceTabs).stream()
        .sorted(Comparator.comparingInt(LicenceTab::displayOrder))
        .toList();
  }

  public void hydrateModel(
      ModelAndView modelAndView,
      Licence licence,
      LicenceTab currentTab,
      ServiceUserDetail user
  ) {
    var context = new LicenceTabContext(licence);
    var topLevelLicenceActions = licenceActionService.getTopLevelLicenceActionItems(licence, user);
    var tabs = enabledLicenceTabs.stream()
        .map(licenceTab -> FdsBackendTab.from(licenceTab, context))
        .toList();
    var licenceActionsForTab = licenceActionService.getLicenceActionItemsForTab(licence, user, currentTab);

    modelAndView
        .addObject("topLevelLicenceActions", topLevelLicenceActions)
        .addObject("tabs", tabs)
        .addObject("currentTab", FdsBackendTab.from(currentTab, context))
        .addObject("currentTabLicenceActions", licenceActionsForTab);
  }

  /**
   * The URL of the licence page's default tab — the first enabled tab in display order. That makes the timeline the
   * default once LMS2 is switched on, and the schedule while only LMS1 is. Use this for links to the licence page which
   * aren't tied to a particular tab.
   */
  public String getDefaultTabUrl(Licence licence) {
    return enabledLicenceTabs.stream()
        .findFirst()
        .map(licenceTab -> licenceTab.url(new LicenceTabContext(licence)))
        .orElseThrow(() -> new IllegalStateException(
            "No licence tabs are enabled so there is no default tab for licence with id: %d".formatted(licence.getId())));
  }

}

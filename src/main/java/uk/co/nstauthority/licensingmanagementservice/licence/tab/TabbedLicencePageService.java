package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;

@Service
public class TabbedLicencePageService {

  private final LicenceActionService licenceActionService;
  private final List<LicenceTab> licenceTabs;

  TabbedLicencePageService(LicenceActionService licenceActionService, List<LicenceTab> licenceTabs) {
    this.licenceActionService = licenceActionService;
    this.licenceTabs = licenceTabs.stream().sorted(Comparator.comparing(LicenceTab::displayName)).toList();
  }

  public void hydrateModel(
      ModelAndView modelAndView,
      Licence licence,
      LicenceTab currentTab,
      ServiceUserDetail user
  ) {
    var context = new LicenceTabContext(licence);
    var topLevelLicenceActions = licenceActionService.getTopLevelLicenceActionItems(licence, user);
    var tabs = licenceTabs.stream().map(licenceTab -> FdsBackendTab.from(licenceTab, context)).toList();
    var licenceActionsForTab = licenceActionService.getLicenceActionItemsForTab(licence, user, currentTab);

    modelAndView
        .addObject("topLevelLicenceActions", topLevelLicenceActions)
        .addObject("tabs", tabs)
        .addObject("currentTab", FdsBackendTab.from(currentTab, context))
        .addObject("currentTabLicenceActions", licenceActionsForTab);
  }

}

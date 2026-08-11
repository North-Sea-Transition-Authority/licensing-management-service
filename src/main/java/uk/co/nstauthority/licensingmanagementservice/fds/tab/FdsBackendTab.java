package uk.co.nstauthority.licensingmanagementservice.fds.tab;

import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;

public record FdsBackendTab(String label, String anchor, String url) {

  public static FdsBackendTab from(LicenceTab licenceTab, LicenceTabContext licenceTabContext) {
    return new FdsBackendTab(licenceTab.displayName(), licenceTab.anchor(), licenceTab.url(licenceTabContext));
  }

}

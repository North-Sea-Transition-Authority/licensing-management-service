package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import java.util.Collection;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;

/**
 * A tab on the licence page. Extends {@link PhaseGated} so each tab declares the release feature (hence phase) it
 * belongs to — {@code TabbedLicencePageService} filters the tabs accordingly.
 */
public interface LicenceTab extends PhaseGated {

  String displayName();

  /** Tabs are displayed in ascending order, and the first enabled tab is the licence page's default tab. */
  int displayOrder();

  String url(LicenceTabContext context);

  default Collection<LicenceActionItem> actions(Licence licence, ServiceUserDetail userDetail) {
    return List.of();
  }

  default String anchor() {
    return displayName().toLowerCase().replace(" ", "-");
  }

}

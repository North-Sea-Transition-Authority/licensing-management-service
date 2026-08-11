package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import java.util.Collection;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;

public interface LicenceTab {

  String displayName();

  String url(LicenceTabContext context);

  default Collection<LicenceActionItem> actions(Licence licence, ServiceUserDetail userDetail) {
    return List.of();
  }

  default String anchor() {
    return displayName().toLowerCase().replace(" ", "-");
  }

}

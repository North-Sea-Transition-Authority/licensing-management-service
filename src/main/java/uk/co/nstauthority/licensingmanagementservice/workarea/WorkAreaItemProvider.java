package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

public interface WorkAreaItemProvider {

  List<SearchResultItem> getWorkAreaItems(WorkAreaFilterForm workAreaFilterForm, ServiceUserDetail serviceUserDetail);
}

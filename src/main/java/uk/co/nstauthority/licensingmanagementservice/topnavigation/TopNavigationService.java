package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;

@Service
public class TopNavigationService {

  private final UserDetailService userDetailService;

  public TopNavigationService(
      UserDetailService userDetailService
  ) {
    this.userDetailService = userDetailService;
  }

  public List<TopNavigationItem> getTopNavigationItems() {
    if (!userDetailService.isUserLoggedIn()) {
      return Collections.emptyList();
    }
    var navigationItems = new ArrayList<TopNavigationItem>();
    navigationItems.add(TopNavigationItem.WORK_AREA);
    navigationItems.add(TopNavigationItem.LICENCE_SEARCH);
    navigationItems.add(TopNavigationItem.CREATE_LICENCE);
    navigationItems.add(TopNavigationItem.TEAMS);

    return navigationItems;
  }
}

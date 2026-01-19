package uk.co.nstauthority.licensingmanagementservice.query;

import uk.co.nstauthority.licensingmanagementservice.fds.TabView;
import uk.co.nstauthority.licensingmanagementservice.mvc.PageView;

public record SearchTabItem(PageView<SearchResultItem> searchResultsForTab, TabView tabView) {
}

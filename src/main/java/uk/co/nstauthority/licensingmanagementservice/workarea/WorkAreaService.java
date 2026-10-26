package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@Service
public class WorkAreaService {

  private final List<WorkAreaItemProvider> workAreaItemProviders;
  private final FeatureFlagService featureFlagService;
  private final WorkAreaContextService workAreaContextService;

  WorkAreaService(
      List<WorkAreaItemProvider> workAreaItemProviders,
      FeatureFlagService featureFlagService,
      WorkAreaContextService workAreaContextService
  ) {
    this.workAreaItemProviders = workAreaItemProviders;
    this.featureFlagService = featureFlagService;
    this.workAreaContextService = workAreaContextService;
  }

  public List<SearchResultItem> getMyWorkResults(WorkAreaFilterForm filter, ServiceUserDetail serviceUserDetail) {
    return getResults(filter, serviceUserDetail, WorkAreaScope.MY_WORK);
  }

  public List<SearchResultItem> getAllWorkResults(WorkAreaFilterForm filter, ServiceUserDetail serviceUserDetail) {
    return getResults(filter, serviceUserDetail, WorkAreaScope.ALL_WORK);
  }

  private List<SearchResultItem> getResults(
      WorkAreaFilterForm filter,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  ) {
    // Only include categories whose release phase is switched on
    var sections = featureFlagService.filterEnabled(workAreaItemProviders).stream()
        .map(workAreaItemProvider -> workAreaItemProvider.loadSection(filter, serviceUserDetail, workAreaScope))
        .toList();

    // Look up what the sections share once for all of them, rather than once per section
    var context = workAreaContextService.buildContext(sections, filter, serviceUserDetail);

    return sections.stream()
        .flatMap(section -> section.buildItems(context).stream())
        .sorted(Comparator.comparing(SearchResultItem::transactionDatetime).reversed())
        .toList();
  }
}

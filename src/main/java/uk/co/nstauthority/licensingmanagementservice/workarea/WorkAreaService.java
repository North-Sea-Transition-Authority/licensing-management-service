package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@Service
public class WorkAreaService {

  private final List<WorkAreaItemProvider> workAreaItemProviders;
  private final FeatureFlagService featureFlagService;

  public WorkAreaService(List<WorkAreaItemProvider> workAreaItemProviders, FeatureFlagService featureFlagService) {
    this.workAreaItemProviders = workAreaItemProviders;
    this.featureFlagService = featureFlagService;
  }

  public List<SearchResultItem> getMyWorkResults(WorkAreaFilterForm filter, ServiceUserDetail serviceUserDetail) {
    return getResults(provider -> provider.getMyWorkItems(filter, serviceUserDetail));
  }

  public List<SearchResultItem> getAllWorkResults(WorkAreaFilterForm filter, ServiceUserDetail serviceUserDetail) {
    return getResults(provider -> provider.getAllWorkItems(filter, serviceUserDetail));
  }

  private List<SearchResultItem> getResults(Function<WorkAreaItemProvider, List<SearchResultItem>> itemSupplier) {
    // Only include categories whose release phase is switched on
    return featureFlagService.filterEnabled(workAreaItemProviders).stream()
        .flatMap(workAreaItemProvider -> itemSupplier.apply(workAreaItemProvider).stream())
        .sorted(Comparator.comparing(SearchResultItem::transactionDatetime).reversed())
        .toList();
  }
}

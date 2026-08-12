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

  public WorkAreaService(List<WorkAreaItemProvider> workAreaItemProviders, FeatureFlagService featureFlagService) {
    this.workAreaItemProviders = workAreaItemProviders;
    this.featureFlagService = featureFlagService;
  }

  public List<SearchResultItem> getWorkAreaResults(WorkAreaFilterForm filter,
                                                   ServiceUserDetail serviceUserDetail) {
    // Only include categories whose release phase is switched on
    return featureFlagService.filterEnabled(workAreaItemProviders).stream()
        .flatMap(workAreaItemProvider -> workAreaItemProvider
            .getWorkAreaItems(filter, serviceUserDetail).stream())
        .sorted(Comparator.comparing(SearchResultItem::transactionDatetime).reversed())
        .toList();
  }
}

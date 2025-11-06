package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@Service
public class WorkAreaService {

  private final List<WorkAreaItemProvider> workAreaItemProviders;

  public WorkAreaService(List<WorkAreaItemProvider> workAreaItemProviders) {
    this.workAreaItemProviders = workAreaItemProviders;
  }

  public List<SearchResultItem> getWorkAreaResults(WorkAreaFilterForm filter,
                                                   ServiceUserDetail serviceUserDetail) {
    return workAreaItemProviders.stream()
        .flatMap(workAreaItemProvider -> workAreaItemProvider
            .getWorkAreaItems(filter, serviceUserDetail).stream())
        .sorted(Comparator.comparing(SearchResultItem::transactionDatetime).reversed())
        .toList();
  }
}

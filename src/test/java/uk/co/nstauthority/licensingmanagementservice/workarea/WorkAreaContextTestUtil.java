package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;

public class WorkAreaContextTestUtil {

  private WorkAreaContextTestUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Map<Licence, List<OrganisationUnit>> licenseesByLicence = Map.of();
    private Map<Integer, Integer> organisationGroupIdsByUnitId = Map.of();
    private List<Integer> licenseeGroupOrganisationUnitIds = null;
    private List<WorkAreaItemView> viewedItems = List.of();

    public Builder withLicenseesByLicence(Map<Licence, List<OrganisationUnit>> licenseesByLicence) {
      this.licenseesByLicence = licenseesByLicence;
      return this;
    }

    public Builder withOrganisationGroupIdsByUnitId(Map<Integer, Integer> organisationGroupIdsByUnitId) {
      this.organisationGroupIdsByUnitId = organisationGroupIdsByUnitId;
      return this;
    }

    public Builder withLicenseeGroupOrganisationUnitIds(List<Integer> licenseeGroupOrganisationUnitIds) {
      this.licenseeGroupOrganisationUnitIds = licenseeGroupOrganisationUnitIds;
      return this;
    }

    public Builder withViewedItems(List<WorkAreaItemView> viewedItems) {
      this.viewedItems = viewedItems;
      return this;
    }

    public WorkAreaContext build() {
      return new WorkAreaContext(
          licenseesByLicence,
          organisationGroupIdsByUnitId,
          licenseeGroupOrganisationUnitIds,
          viewedItems.stream().collect(Collectors.groupingBy(
              WorkAreaItemView::getItemType,
              Collectors.mapping(WorkAreaItemView::getItemId, Collectors.toSet())
          ))
      );
    }
  }
}

package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

/**
 * The data every work area section needs, looked up once for all of them by {@link WorkAreaContextService}.
 *
 * @param licenseesByLicence the licensees of every licence any section shows
 * @param organisationGroupIdsByUnitId the organisation group of each licensee, for the sections that need it
 * @param licenseeGroupOrganisationUnitIds the organisation units in the licensee group being filtered by, or null when
 *                                         the work area is not filtered by licensee group
 * @param viewedItemIdsByType the ids of the items the user has already viewed, by item type
 */
public record WorkAreaContext(
    Map<Licence, List<OrganisationUnit>> licenseesByLicence,
    Map<Integer, Integer> organisationGroupIdsByUnitId,
    List<Integer> licenseeGroupOrganisationUnitIds,
    Map<WorkAreaDataItemType, Set<UUID>> viewedItemIdsByType
) {

  public List<OrganisationUnit> getLicensees(Licence licence) {
    return licenseesByLicence.getOrDefault(licence, List.of());
  }

  public List<Integer> getLicenseeOrganisationUnitIds(Licence licence) {
    return getLicensees(licence).stream()
        .map(OrganisationUnit::organisationUnitId)
        .toList();
  }

  public Map<Integer, Integer> getOrganisationGroupIdsByUnitId(Collection<Integer> organisationUnitIds) {
    return organisationUnitIds.stream()
        .filter(organisationGroupIdsByUnitId::containsKey)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), organisationGroupIdsByUnitId::get));
  }

  public boolean matchesLicenseeFilters(Licence licence, WorkAreaFilterForm filterForm) {
    var licenseeOrganisationUnitIds = getLicenseeOrganisationUnitIds(licence);
    return FilterUtil.matchesIdList(licenseeOrganisationUnitIds, filterForm.getLicenseeOrgUnitId())
        && FilterUtil.listMatchesIdList(licenseeOrganisationUnitIds, licenseeGroupOrganisationUnitIds);
  }

  public boolean isNewItem(WorkAreaDataItemType itemType, UUID itemId) {
    return !viewedItemIdsByType.getOrDefault(itemType, Set.of()).contains(itemId);
  }
}

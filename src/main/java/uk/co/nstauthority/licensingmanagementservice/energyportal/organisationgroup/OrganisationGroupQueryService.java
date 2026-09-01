package uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.starter.configuration.WellKnownOrganisationGroupsConfigurationProperties;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.client.OrganisationGroupProjectionRoot;
import uk.co.fivium.energyportalapi.generated.client.OrganisationGroupsProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;

@Service
public class OrganisationGroupQueryService {
  public static final OrganisationGroupsProjectionRoot ORGANISATION_GROUPS_PROJECTION_ROOT =
      new OrganisationGroupsProjectionRoot()
          .organisationGroupId()
          .name();
  public static final OrganisationGroupProjectionRoot ORGANISATION_GROUP_PROJECTION_ROOT =
      new OrganisationGroupProjectionRoot()
          .organisationGroupId()
          .name()
          .emailDomains()
          .domain()
          .root();
  public static final OrganisationGroupsProjectionRoot ORGANISATION_GROUPS_UNITS_PROJECTION_ROOT =
      ORGANISATION_GROUPS_PROJECTION_ROOT
          .organisationUnits()
          .organisationUnitId()
          .name()
          .registeredNumber()
          .isDuplicate()
          .root();

  private final OrganisationApi organisationApi;
  private final WellKnownOrganisationGroupsConfigurationProperties wellKnownOrganisationGroups;

  OrganisationGroupQueryService(OrganisationApi organisationApi,
                                WellKnownOrganisationGroupsConfigurationProperties wellKnownOrganisationGroups
  ) {
    this.organisationApi = organisationApi;
    this.wellKnownOrganisationGroups = wellKnownOrganisationGroups;
  }

  public List<OrganisationGroupDto> getOrganisationGroupsByName(String name) {
    return organisationApi.searchOrganisationGroups(
            name,
            ORGANISATION_GROUPS_PROJECTION_ROOT,
            new RequestPurpose("getOrganisationGroupsByName")
        )
        .stream()
        .map(OrganisationGroupDto::from)
        .toList();
  }

  public List<OrganisationGroup> getOrganisationGroupsByIds(List<Integer> organisationGroups) {
    return organisationApi.getAllOrganisationGroupsByIds(
        organisationGroups,
        ORGANISATION_GROUPS_UNITS_PROJECTION_ROOT,
        new RequestPurpose("getOrganisationGroupsByIds")
    );
  }

  public List<OrganisationUnitJson> getOrganisationUnitsByOrganisationGroupIds(List<Integer> organisationGroupIds) {
    return getOrganisationGroupsByIds(organisationGroupIds)
        .stream()
        .filter(orgGroup -> orgGroup.getOrganisationUnits() != null && !orgGroup.getOrganisationUnits().isEmpty())
        .flatMap(orgGroup -> orgGroup.getOrganisationUnits().stream())
        .map(OrganisationUnitJson::from)
        .toList();
  }

  public Optional<OrganisationGroupDto> getOrganisationGroupById(Integer id) {
    return organisationApi.findOrganisationGroup(
            id,
            ORGANISATION_GROUP_PROJECTION_ROOT,
            new RequestPurpose("getOrganisationGroupById")
        )
        .map(OrganisationGroupDto::from);
  }

  public Optional<OrganisationGroupDto> getRegulatorOrganisationGroup() {
    return getOrganisationGroupById(wellKnownOrganisationGroups.nsta().idAsInteger());
  }

  /**
   * Resolves the member organisation unit ids of a licensee group filter, for use with
   * {@link uk.co.nstauthority.licensingmanagementservice.util.FilterUtil#listMatchesIdList(List, List)}.
   * Returns {@code null} (meaning "no group filter applied") when no group id is given.
   */
  public List<Integer> getOrganisationUnitIdsByOrganisationGroupId(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return Collections.emptyList();
    }

    return getOrganisationUnitsByOrganisationGroupIds(List.of(organisationGroupId)).stream()
        .map(OrganisationUnitJson::organisationUnitId)
        .toList();
  }

  public Map<String, String> getOrganisationGroupSelectOption(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return Map.of();
    }

    return getOrganisationGroupById(organisationGroupId)
        .map(group -> Map.of(group.getOrganisationGroupId().toString(), group.getOrganisationGroupName()))
        .orElse(Map.of());
  }
}

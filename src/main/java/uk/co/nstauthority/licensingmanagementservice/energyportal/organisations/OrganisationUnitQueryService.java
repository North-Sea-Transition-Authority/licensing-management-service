package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.client.OrganisationUnitProjectionRoot;
import uk.co.fivium.energyportalapi.generated.client.OrganisationUnitsProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;

@Service
public class OrganisationUnitQueryService {

  public static final OrganisationUnitsProjectionRoot ORGANISATION_UNITS_PROJECTION_ROOT
      = new OrganisationUnitsProjectionRoot().organisationUnitId().name().registeredNumber();

  public static final OrganisationUnitProjectionRoot ORGANISATION_UNIT_GROUPS_PROJECTION_ROOT
      = new OrganisationUnitProjectionRoot().isDuplicate().organisationUnitId().organisationGroups().organisationGroupId().root();

  public static final OrganisationUnitsProjectionRoot ORGANISATION_UNITS_GROUPS_PROJECTION_ROOT
      = new OrganisationUnitsProjectionRoot().organisationUnitId().name().organisationGroups().organisationGroupId().root();

  private final OrganisationApi organisationApi;

  public OrganisationUnitQueryService(OrganisationApi organisationApi) {
    this.organisationApi = organisationApi;
  }

  public List<OrganisationUnitJson> getOrganisationUnitsByIds(List<Integer> organisationUnitIds) {
    return getOrganisationUnitsByIdsFromEpa(organisationUnitIds)
        .stream()
        .map(OrganisationUnitJson::from)
        .toList();
  }

  public Map<Integer, String> getOrganisationUnitNamesByIds(List<Integer> responsibleOrganisationIds) {
    return getOrganisationUnitsByIdsFromEpa(responsibleOrganisationIds).stream()
        .collect(Collectors.toMap(OrganisationUnit::getOrganisationUnitId, OrganisationUnit::getName));
  }

  public Optional<String> getOrganisationUnitNameById(Integer responsibleOrganisationUnitId) {
    return getOrganisationUnit(responsibleOrganisationUnitId)
        .map(OrganisationUnit::getName);
  }

  public Optional<Address> getOrganisationUnitAddressById(Integer responsibleOrganisationUnitId) {
    return getOrganisationUnit(responsibleOrganisationUnitId)
        .map(OrganisationUnit::getRegisteredAddress);
  }

  public Optional<OrganisationUnit> getOrganisationUnit(Integer responsibleOrganisationUnitId) {
    return getOrganisationUnitsByIdsFromEpa(List.of(responsibleOrganisationUnitId))
        .stream()
        .findFirst();
  }

  private List<OrganisationUnit> getOrganisationUnitsByIdsFromEpa(List<Integer> organisationUnitIds) {
    return organisationApi.getOrganisationUnitsByIds(
        organisationUnitIds,
        ORGANISATION_UNITS_PROJECTION_ROOT,
        new RequestPurpose("Get organisation units by ids"),
        CorrelationIdUtil.getLogCorrelationId()
    );
  }

  public Optional<Integer> findOrganisationGroupIdByUnitId(Integer organisationUnitId) {
    return organisationApi.findOrganisationUnit(
                              organisationUnitId,
                              ORGANISATION_UNIT_GROUPS_PROJECTION_ROOT,
                              new RequestPurpose("Find organisation unit by ID"),
                              CorrelationIdUtil.getLogCorrelationId())
                          .flatMap(this::organisationGroupIdFromOrganisationUnit);
  }

  public List<Integer> findOrganisationGroupIdsByUnitIds(List<Integer> organisationUnitIds) {
    return organisationApi.getOrganisationUnitsByIds(
            organisationUnitIds,
            ORGANISATION_UNITS_GROUPS_PROJECTION_ROOT,
            new RequestPurpose("Get organisation units and org groups by org unit ids"),
            CorrelationIdUtil.getLogCorrelationId()
        ).stream()
        .map(this::organisationGroupIdFromOrganisationUnit)
        .flatMap(Optional::stream)
        .toList();
  }

  public Map<Integer, Integer> findOrganisationGroupIdMapByUnitIds(List<Integer> organisationUnitIds) {
    return organisationApi.getOrganisationUnitsByIds(
            organisationUnitIds,
            ORGANISATION_UNITS_GROUPS_PROJECTION_ROOT,
            new RequestPurpose("Get organisation units and org groups by org unit ids"),
            CorrelationIdUtil.getLogCorrelationId()
        ).stream()
        .filter(unit -> organisationGroupIdFromOrganisationUnit(unit).isPresent())
        .collect(Collectors.toMap(
            OrganisationUnit::getOrganisationUnitId,
            unit -> organisationGroupIdFromOrganisationUnit(unit).get()
        ));
  }

  private Optional<Integer> organisationGroupIdFromOrganisationUnit(OrganisationUnit organisationUnit) {
    if (organisationUnit.getOrganisationGroups() == null) {
      return Optional.empty();
    }

    return organisationUnit.getOrganisationGroups()
                           // EPA returns a list, but there should only be a maximum of one organisation group per unit
                           .stream()
                           .map(OrganisationGroup::getOrganisationGroupId)
                           .filter(Objects::nonNull)
                           .findFirst();
  }

  public List<OrganisationUnitJson> searchOrganisationUnitsWithName(String organisationName) {
    return organisationApi.searchOrganisationUnits(
        organisationName,
        ORGANISATION_UNITS_PROJECTION_ROOT,
        new RequestPurpose("Search organisation units by name"),
        CorrelationIdUtil.getLogCorrelationId()
    )
        .stream()
        .filter(unit -> !BooleanUtils.isTrue(unit.getIsDuplicate()))
        .map(OrganisationUnitJson::from)
        .sorted(Comparator.comparing(OrganisationUnitJson::name))
        .toList();
  }
}
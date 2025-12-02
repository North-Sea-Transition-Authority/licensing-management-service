package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.client.OrganisationUnitsProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;

@Service
public class OrganisationUnitQueryService {

  public static final OrganisationUnitsProjectionRoot ORGANISATION_UNITS_PROJECTION_ROOT
      = new OrganisationUnitsProjectionRoot().organisationUnitId().name().registeredNumber();

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
    return getOrganisationUnitsByIdsFromEpa(List.of(responsibleOrganisationUnitId))
        .stream()
        .findFirst()
        .map(OrganisationUnit::getName);
  }

  private List<OrganisationUnit> getOrganisationUnitsByIdsFromEpa(List<Integer> organisationUnitIds) {
    return organisationApi.getOrganisationUnitsByIds(
        organisationUnitIds,
        ORGANISATION_UNITS_PROJECTION_ROOT,
        new RequestPurpose("Get organisation units by ids"),
        CorrelationIdUtil.getLogCorrelationId()
    );
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

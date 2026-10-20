package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds organisation units fetched from the Energy Portal by id for the lifetime of a single HTTP request, so the
 * access rules, services and page share one lookup per organisation unit. Ids the Energy Portal does not return are
 * remembered too, so they are not asked for again. Used internally by {@link OrganisationUnitQueryService}; go through
 * that service rather than using this directly.
 */
@Component
public class OrganisationUnitRequestCache extends RequestScopedCache<Integer, Optional<OrganisationUnit>> {

  static final RequestPurpose GET_ORGANISATION_UNITS_BY_IDS_PURPOSE = new RequestPurpose("Get organisation units by ids");

  private final OrganisationApi organisationApi;

  public OrganisationUnitRequestCache(OrganisationApi organisationApi) {
    this.organisationApi = organisationApi;
  }

  public List<OrganisationUnit> getOrganisationUnitsByIds(Collection<Integer> organisationUnitIds) {
    return getAll(organisationUnitIds).values().stream()
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  protected Optional<OrganisationUnit> load(Integer organisationUnitId) {
    return loadAll(List.of(organisationUnitId)).get(organisationUnitId);
  }

  @Override
  protected Map<Integer, Optional<OrganisationUnit>> loadAll(Collection<Integer> organisationUnitIds) {
    var organisationUnitsById = organisationApi.getOrganisationUnitsByIds(
            List.copyOf(organisationUnitIds),
            OrganisationUnitQueryService.ORGANISATION_UNITS_PROJECTION_ROOT,
            GET_ORGANISATION_UNITS_BY_IDS_PURPOSE,
            CorrelationIdUtil.getLogCorrelationId()
        )
        .stream()
        .collect(Collectors.toMap(
            OrganisationUnit::getOrganisationUnitId,
            Function.identity(),
            (first, duplicate) -> first
        ));

    var result = new LinkedHashMap<Integer, Optional<OrganisationUnit>>();
    organisationUnitIds.forEach(id -> result.put(id, Optional.ofNullable(organisationUnitsById.get(id))));
    return result;
  }
}

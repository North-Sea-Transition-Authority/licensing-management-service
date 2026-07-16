package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceReferenceComparator;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class LicenceSearchService {

  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public LicenceSearchService(LicenceService licenceService,
                              LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
                              OrganisationUnitQueryService organisationUnitQueryService,
                              OrganisationGroupQueryService organisationGroupQueryService) {
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  public List<SearchResultItem> getSearchResultItems(LicenceSearchFilterForm filterForm) {
    // get all licenses and apply simple filtering
    var filteredLicenses = licenceService.getAllLicences().stream()
        .filter(licence -> FilterUtil.matchesTextInput(licence.getLicenceReference(), filterForm.getLicenceReference()))
        .filter(licence -> FilterUtil.matchesEnum(LicenceType.class, licence.getType(), filterForm.getLicenceTypes()))
        .toList();

    var licenceResponsibleOrganisations = licenceResponsibleOrganisationService.getAllByLicenceIn(filteredLicenses);

    // apply batch filtering
    var responsibleOrganisationIds = getResponsibleOrganisationIds(licenceResponsibleOrganisations);
    var groupOrgUnitIds = getOrgUnitIdsForGroup(filterForm.getLicenseeOrgGroupId());
    var batchFilteredLicences = filteredLicenses.stream()
        .filter(licence -> {
          var orgUnitIds = responsibleOrganisationIds.getOrDefault(licence, List.of());
          return FilterUtil.matchesIdList(orgUnitIds, filterForm.getLicenseeOrgUnitId())
              && FilterUtil.listMatchesIdList(orgUnitIds, groupOrgUnitIds);
        });

    var responsibleOrganisationNames = getResponsibleOrganisationNamesByLicences(licenceResponsibleOrganisations);
    return batchFilteredLicences
        .map(licence -> toSearchResultItem(licence, responsibleOrganisationNames.getOrDefault(licence, List.of())))
        .sorted(Comparator.comparing(SearchResultItem::linkHeadingText, new LicenceReferenceComparator()))
        .toList();
  }

  public Map<Licence, List<String>> getLicenceToResponsibleOrganisationNameMap(List<Licence> licences) {
    var responsibleOrganisations = licenceResponsibleOrganisationService.getAllByLicenceIn(licences);

    return getResponsibleOrganisationNamesByLicences(responsibleOrganisations);
  }

  Map<Licence, List<Integer>> getResponsibleOrganisationIds(
      List<LicenceResponsibleOrganisation> licenceResponsibleOrganisations) {
    return licenceResponsibleOrganisations.stream()
        .collect(Collectors.groupingBy(
            LicenceResponsibleOrganisation::getLicence,
            Collectors.mapping(
                LicenceResponsibleOrganisation::getResponsibleOrganisationId,
                Collectors.toList()
            )
        ));
  }

  Map<Licence, List<String>> getResponsibleOrganisationNamesByLicences(
      List<LicenceResponsibleOrganisation> licenceResponsibleOrganisations) {

    var responsibleOrganisationIds = licenceResponsibleOrganisations.stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .distinct()
        .toList();

    var organisationUnitNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(responsibleOrganisationIds);

    return licenceResponsibleOrganisations.stream()
        .collect(Collectors.groupingBy(
            LicenceResponsibleOrganisation::getLicence,
            Collectors.mapping(
                lro -> organisationUnitNames.get(lro.getResponsibleOrganisationId()),
                Collectors.toList()
            )
        ));
  }

  Map<String, String> getPreselectedOrganisationUnit(Integer organisationUnitId) {
    if (organisationUnitId == null) {
      return Collections.emptyMap();
    }

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(Collections.singletonList(organisationUnitId)).entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().toString(),
            Map.Entry::getValue
        ));
  }

  Map<String, String> getPreselectedOrganisationGroup(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return Collections.emptyMap();
    }

    return organisationGroupQueryService.getOrganisationGroupById(organisationGroupId)
        .map(g -> Map.of(g.getOrganisationGroupId().toString(), g.getOrganisationGroupName()))
        .orElse(Collections.emptyMap());
  }

  private List<Integer> getOrgUnitIdsForGroup(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return null;
    }

    return organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(organisationGroupId))
        .stream()
        .map(OrganisationUnitJson::organisationUnitId)
        .toList();
  }

  private SearchResultItem toSearchResultItem(Licence licence, List<String> licensees) {
    var mappedLicensees = licensees.stream().filter(Objects::nonNull).toList();
    return SearchResultItem.newBuilder()
        .withId(licence.getId().toString())
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceOverviewController.class)
          .renderLicenceOverview(licence.getId(), null, null, null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licensee(s)", String.join(", ", mappedLicensees))
            .addStringValue("Status", licence.getStatus().getDisplayName())
            .build())
        .build();
  }
}

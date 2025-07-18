package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class LicenceSearchService {

  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicenceSearchService(LicenceService licenceService,
                              LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
                              OrganisationUnitQueryService organisationUnitQueryService) {
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public List<SearchResultItem> getSearchResultItems(LicenceSearchFilterForm filterForm) {
    // get all licenses and apply simple filtering
    var filteredLicenses = licenceService.getAllLicences().stream()
        .filter(licence -> FilterUtil.filterTextInput(licence.getLicenceReference(), filterForm.getReference()))
        .filter(licence -> FilterUtil.filterEnum(LicenceType.class, licence.getType(), filterForm.getLicenceTypes()))
        .toList();

    var licenceResponsibleOrganisations = licenceResponsibleOrganisationService.getAllByLicenceIn(filteredLicenses);

    // apply batch filtering
    var responsibleOrganisationIds = getResponsibleOrganisationIds(licenceResponsibleOrganisations);
    var batchFilteredLicences = filteredLicenses.stream()
        .filter(licence -> {
          var orgUnitIds = responsibleOrganisationIds.getOrDefault(licence, List.of());
          return FilterUtil.filterIdList(orgUnitIds, filterForm.getLicenseeOrgUnitId());
        });

    var responsibleOrganisationNames = getResponsibleOrganisationNamesByLicences(licenceResponsibleOrganisations);
    return batchFilteredLicences
        .map(licence -> toSearchResultItem(licence, responsibleOrganisationNames.getOrDefault(licence, List.of())))
        .sorted(Comparator.comparing(SearchResultItem::linkHeadingText))
        .toList();
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

  private SearchResultItem toSearchResultItem(Licence licence, List<String> licensees) {
    return SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence.getId(), null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .withDataItemRow(SummaryDataView.newStringKeyValue("Licensee(s)", String.join(", ", licensees)))
        .build();
  }
}

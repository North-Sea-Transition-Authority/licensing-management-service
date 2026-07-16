package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class LicenceSearchServiceTest {

  private static final int ORG_UNIT_ID_ALPHA = 100;
  private static final int ORG_UNIT_ID_BETA = 101;
  private static final String ORG_UNIT_NAME_ALPHA = "Org Alpha";
  private static final String ORG_UNIT_NAME_BETA = "Org Beta";
  private static final int ORG_GROUP_ID = 5;

  private static final Licence SEAWARD_PRODUCTION_LICENCE = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1", LicenceStatus.EXTANT);
  private static final Licence CARBON_STORAGE_LICENCE = buildLicence(2, LicenceType.CARBON_STORAGE, "CS1", LicenceStatus.EXTANT);
  private static final Licence CARBON_STORAGE_LICENCE_2 = buildLicence(5, LicenceType.CARBON_STORAGE, "CS2", LicenceStatus.EXTANT);
  private static final Licence GAS_STORAGE_LICENCE = buildLicence(3, LicenceType.GAS_STORAGE, "GS1", LicenceStatus.EXPIRED);
  private static final Licence LANDWARD_PRODUCTION_LICENCE = buildLicence(4, LicenceType.LANDWARD_PRODUCTION, "PEDL1", LicenceStatus.SURRENDERED);

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private LicenceSearchService licenceSearchService;

  @Test
  void getSearchResultItems_NoFilters_ReturnsAllResults() {
    when(licenceService.getAllLicences()).thenReturn(List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE));
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE)))
        .thenReturn(List.of());

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm());

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByReference_ReturnsFilteredResults() {
    when(licenceService.getAllLicences())
        .thenReturn(List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, CARBON_STORAGE_LICENCE_2));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceReference("s");

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE_2, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceType_ReturnsFilteredResults() {
    when(licenceService.getAllLicences())
        .thenReturn(List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, CARBON_STORAGE_LICENCE_2, LANDWARD_PRODUCTION_LICENCE));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name(), LicenceType.LANDWARD_PRODUCTION.name()));

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE_2, List.of()),
            buildSearchResultItem(LANDWARD_PRODUCTION_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicensee_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, GAS_STORAGE_LICENCE, LANDWARD_PRODUCTION_LICENCE);

    when(licenceService.getAllLicences()).thenReturn(licences);
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro3 = buildLicenceResponsibleOrganisation(GAS_STORAGE_LICENCE, ORG_UNIT_ID_BETA);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of(lro1, lro3));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA, ORG_UNIT_ID_BETA, ORG_UNIT_NAME_BETA));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenseeOrgUnitId(ORG_UNIT_ID_ALPHA);

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of(ORG_UNIT_NAME_ALPHA))
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenseeGroup_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, GAS_STORAGE_LICENCE);

    when(licenceService.getAllLicences()).thenReturn(licences);
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro2 = buildLicenceResponsibleOrganisation(CARBON_STORAGE_LICENCE, ORG_UNIT_ID_BETA);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of(lro1, lro2));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA, ORG_UNIT_ID_BETA, ORG_UNIT_NAME_BETA));
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(ORG_GROUP_ID)))
        .thenReturn(List.of(new OrganisationUnitJson(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA)));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenseeOrgGroupId(ORG_GROUP_ID);

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of(ORG_UNIT_NAME_ALPHA))
        ));
  }

  @Test
  void getPreselectedOrganisationUnit_whenIdProvided_returnsNameMap() {
    var orgUnitJson = new OrganisationUnitJson(1, "org name");

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(anyList()))
        .thenReturn(Map.of(orgUnitJson.organisationUnitId(), orgUnitJson.getName()));

    var result = licenceSearchService.getPreselectedOrganisationUnit(1);

    assertThat(result).isEqualTo(Map.of(orgUnitJson.getId(), orgUnitJson.getName()));
  }

  @Test
  void getPreselectedOrganisationUnit_whenNullId_returnsEmptyMap() {
    var result = licenceSearchService.getPreselectedOrganisationUnit(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationUnitQueryService);
  }

  @Test
  void getPreselectedOrganisationGroup_whenGroupExists() {
    var groupDto = new OrganisationGroupDto();
    groupDto.setOrganisationGroupId(ORG_GROUP_ID);
    groupDto.setOrganisationGroupName("Group Alpha");

    when(organisationGroupQueryService.getOrganisationGroupById(ORG_GROUP_ID))
        .thenReturn(Optional.of(groupDto));

    var result = licenceSearchService.getPreselectedOrganisationGroup(ORG_GROUP_ID);

    assertThat(result).isEqualTo(Map.of(String.valueOf(ORG_GROUP_ID), "Group Alpha"));
  }

  @Test
  void getPreselectedOrganisationGroup_whenNoneSelected() {
    var result = licenceSearchService.getPreselectedOrganisationGroup(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationGroupQueryService);
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_ReturnsCorrectMapping() {
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro2 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_BETA);
    var lro3 = buildLicenceResponsibleOrganisation(CARBON_STORAGE_LICENCE, ORG_UNIT_ID_ALPHA);
    var lroList = List.of(lro1, lro2, lro3);

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, "Org 100", ORG_UNIT_ID_BETA, "Org 101"));

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(lroList);

    assertThat(result).containsOnlyKeys(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE);
    assertThat(result.get(SEAWARD_PRODUCTION_LICENCE)).containsExactlyInAnyOrder("Org 100", "Org 101");
    assertThat(result.get(CARBON_STORAGE_LICENCE)).containsExactly("Org 100");
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_whenEmptyInput_returnsEmptyMap() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of()))
        .thenReturn(Map.of());

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(List.of());

    assertThat(result).isEmpty();
  }

  private static Licence buildLicence(Integer id, LicenceType licenceType, String ref, LicenceStatus status) {
    return LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(licenceType)
        .withLicenceReference(ref)
        .withStatus(status)
        .build();
  }

  private static LicenceResponsibleOrganisation buildLicenceResponsibleOrganisation(Licence licence,
                                                                                     int responsibleOrganisationId) {
    var lro = new LicenceResponsibleOrganisation();
    lro.setLicence(licence);
    lro.setResponsibleOrganisationId(responsibleOrganisationId);
    return lro;
  }

  private static SearchResultItem buildSearchResultItem(Licence licence, List<String> licensees) {
    return SearchResultItem.newBuilder()
        .withId(licence.getId().toString())
        .withLinkHeadingUrl(ReverseRouter.route(
            on(LicenceOverviewController.class).renderLicenceOverview(licence.getId(), null, null, null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licensee(s)", String.join(", ", licensees))
            .addStringValue("Status", licence.getStatus().getDisplayName())
            .build())
        .build();
  }
}

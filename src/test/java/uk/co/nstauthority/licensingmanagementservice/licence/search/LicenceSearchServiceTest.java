package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class LicenceSearchServiceTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Spy
  @InjectMocks
  private LicenceSearchService licenceSearchService;

  @Test
  void getSearchResultItems_NoFilters_ReturnsAllResults() {
    var licence = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(2, LicenceType.CARBON_STORAGE, "CS2");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence, licence2));
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence, licence2)))
        .thenReturn(Collections.emptyList());

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm());

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(of(
            buildSearchResultItem(licence, List.of()),
            buildSearchResultItem(licence2, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByReference_ReturnsFilteredResults() {
    var licence = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(2, LicenceType.CARBON_STORAGE, "CS2");
    var licence3 = buildLicence(3, LicenceType.CARBON_STORAGE, "CS3");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence, licence2, licence3));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setReference("s");

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(of(
            buildSearchResultItem(licence2, List.of()),
            buildSearchResultItem(licence3, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceType_ReturnsFilteredResults() {
    var licence1 = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(2, LicenceType.CARBON_STORAGE, "CS2");
    var licence3 = buildLicence(3, LicenceType.CARBON_STORAGE, "CS3");
    var licence4 = buildLicence(4, LicenceType.LANDWARD_PRODUCTION, "P2");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence1, licence2, licence3, licence4));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name(), LicenceType.LANDWARD_PRODUCTION.name()));

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(of(
            buildSearchResultItem(licence2, List.of()),
            buildSearchResultItem(licence3, List.of()),
            buildSearchResultItem(licence4, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicensee_ReturnsFilteredResults() {
    var licence1 = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(2, LicenceType.CARBON_STORAGE, "CS1");
    var licence3 = buildLicence(3, LicenceType.GAS_STORAGE, "GS1");
    var licence4 = buildLicence(4, LicenceType.LANDWARD_PRODUCTION, "P2");
    var licences = List.of(licence1, licence2, licence3, licence4);

    when(licenceService.getAllLicences()).thenReturn(licences);
    var lro1 = buildLicenceResponsibleOrganisation(licence1, 100);
    var lro3 = buildLicenceResponsibleOrganisation(licence3, 101);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(of(lro1, lro3));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(of(100, 101)))
        .thenReturn(Map.of(100, "Org Alpha", 101, "Org Beta"));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenseeOrgUnitId(100);

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(of(
            buildSearchResultItem(licence1, of("Org Alpha"))
        ));
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_ReturnsCorrectMapping() {
    var licence1 = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(2, LicenceType.CARBON_STORAGE, "CS2");

    var lro1 = buildLicenceResponsibleOrganisation(licence1, 100);
    var lro2 = buildLicenceResponsibleOrganisation(licence1, 101);
    var lro3 = buildLicenceResponsibleOrganisation(licence2, 100);
    var lroList = of(lro1, lro2, lro3);

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(of(100, 101)))
        .thenReturn(Map.of(100, "Org 100", 101, "Org 101"));

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(lroList);

    assertThat(result).containsOnlyKeys(licence1, licence2);
    assertThat(result.get(licence1)).containsExactlyInAnyOrder("Org 100", "Org 101");
    assertThat(result.get(licence2)).containsExactly("Org 100");
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_EmptyInput_ReturnsEmptyMap() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(Collections.emptyList()))
        .thenReturn(Collections.emptyMap());

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(Collections.emptyList());
    assertThat(result).isEmpty();
  }

  @Test
  void getPreselectedOrganisationUnit() {
    var orgUnitJson = new OrganisationUnitJson(
        1,
        "org name"
    );

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(anyList())).thenReturn(Map.of(Integer.valueOf(orgUnitJson.getId()), orgUnitJson.getName()));

    var result = licenceSearchService.getPreselectedOrganisationUnit(1);

    assertThat(result).isEqualTo(Map.of(orgUnitJson.getId(), orgUnitJson.getName()));
  }

  @Test
  void getPreselectedOrganisationUnit_noneSelected() {
    var result = licenceSearchService.getPreselectedOrganisationUnit(null);

    assertThat(result).isEmpty();

    verifyNoInteractions(organisationUnitQueryService);
  }

  private Licence buildLicence(Integer id, LicenceType licenceType, String ref) {
    return LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(licenceType)
        .withLicenceReference(ref)
        .build();
  }

  private LicenceResponsibleOrganisation buildLicenceResponsibleOrganisation(Licence licence, int responsibleOrganisationId) {
    var lro = new LicenceResponsibleOrganisation();
    lro.setLicence(licence);
    lro.setResponsibleOrganisationId(responsibleOrganisationId);
    return lro;
  }

  private SearchResultItem buildSearchResultItem(Licence licence, List<String> licensees) {
    return SearchResultItem.newBuilder()
        .withId(licence.getId().toString())
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence.getId(), null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .withDataItemRow(SummaryDataView.newStringKeyValue("Licensee(s)", String.join(", ", licensees)))
        .build();
  }
}
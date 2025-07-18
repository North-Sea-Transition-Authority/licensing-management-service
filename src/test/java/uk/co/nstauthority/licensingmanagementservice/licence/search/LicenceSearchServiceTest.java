package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@ExtendWith(MockitoExtension.class)
class LicenceSearchServiceTest {

  @Mock
  private LicenceService licenceService;

  @InjectMocks
  private LicenceSearchService licenceSearchService;

  @Test
  void getSearchResultItems_NoFilters_ReturnsAllResults() {
    var licence = buildLicence(LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(LicenceType.CARBON_STORAGE, "CS2");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence, licence2));

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm());

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(licence),
            buildSearchResultItem(licence2)
        ));
  }

  @Test
  void getSearchResultItems_FilterByReference_ReturnsFilteredResults() {
    var licence = buildLicence(LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(LicenceType.CARBON_STORAGE, "CS2");
    var licence3 = buildLicence(LicenceType.CARBON_STORAGE, "CS3");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence, licence2, licence3));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setReference("s");

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(licence2),
            buildSearchResultItem(licence3)
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceType_ReturnsFilteredResults() {
    var licence1 = buildLicence(LicenceType.SEAWARD_PRODUCTION, "P1");
    var licence2 = buildLicence(LicenceType.CARBON_STORAGE, "CS2");
    var licence3 = buildLicence(LicenceType.CARBON_STORAGE, "CS3");
    var licence4 = buildLicence(LicenceType.LANDWARD_PRODUCTION, "P2");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence1, licence2, licence3, licence4));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name(), LicenceType.LANDWARD_PRODUCTION.name()));

    var result = licenceSearchService.getSearchResultItems(filterForm);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(licence2),
            buildSearchResultItem(licence3),
            buildSearchResultItem(licence4)
        ));
  }

  private Licence buildLicence(LicenceType licenceType, String ref) {
    var licence = new Licence();
    licence.setType(licenceType);
    licence.setLicenceReference(ref);
    return licence;
  }

  private SearchResultItem buildSearchResultItem(Licence licence) {
    return SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence.getId(), null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .build();
  }
}
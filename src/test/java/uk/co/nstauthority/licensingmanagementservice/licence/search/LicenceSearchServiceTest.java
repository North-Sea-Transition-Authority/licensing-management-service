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
  void getSearchResultItems() {
    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setLicenceReference("P1");

    var licence2 = new Licence();
    licence2.setType(LicenceType.CARBON_STORAGE);
    licence2.setLicenceReference("CS2");

    when(licenceService.getAllLicences()).thenReturn(List.of(licence, licence2));

    var searchResultItem = SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence.getId(), null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .build();

    var searchResultItem2 = SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence2.getId(), null)))
        .withLinkHeadingText(licence2.getLicenceReference())
        .withCaptionText(licence2.getType().getDisplayName())
        .build();

    var result = licenceSearchService.getSearchResultItems();

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(searchResultItem, searchResultItem2));
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class LicenceSearchService {

  private final LicenceService licenceService;

  public LicenceSearchService(LicenceService licenceService) {
    this.licenceService = licenceService;
  }

  public List<SearchResultItem> getSearchResultItems(LicenceSearchFilterForm filterForm) {
    return licenceService.getAllLicences().stream()
        .filter(licence -> FilterUtil.filterTextInput(licence.getLicenceReference(), filterForm.getReference()))
        .map(this::toSearchResultItem)
        .sorted(Comparator.comparing(SearchResultItem::linkHeadingText))
        .toList();
  }

  private SearchResultItem toSearchResultItem(Licence licence) {
    return SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licence.getId(), null)))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .build();
  }
}

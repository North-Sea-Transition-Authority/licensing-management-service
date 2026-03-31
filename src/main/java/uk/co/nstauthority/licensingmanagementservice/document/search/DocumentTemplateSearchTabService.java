package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.mvc.PageView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.QueryPaginationUtil;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.query.SearchTabItem;

@Service
public class DocumentTemplateSearchTabService {

  static final int DEFAULT_PAGE_NUMBER = 0;

  public List<SearchTabItem> getSearchTabItems(List<LmsDocumentTemplateDto> filteredDocumentTemplateItems,
                                               int pageNumber,
                                               DocumentTemplateSearchTab selectedTab) {
    var continuationSearchTabItem = buildSearchTabItem(
        filteredDocumentTemplateItems,
        DocumentTemplateSearchTab.CONTINUATION,
        ApplicationType.CONTINUATION_APPLICATION,
        pageNumber,
        selectedTab
    );

    var extensionAmendmentSearchTabItem = buildSearchTabItem(
        filteredDocumentTemplateItems,
        DocumentTemplateSearchTab.EXTENSION_AMENDMENT,
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        pageNumber,
        selectedTab
    );

    return List.of(
        continuationSearchTabItem,
        extensionAmendmentSearchTabItem
    );
  }

  private SearchTabItem buildSearchTabItem(
      List<LmsDocumentTemplateDto> filteredDocumentTemplateItems,
      DocumentTemplateSearchTab tab,
      ApplicationType applicationType,
      int pageNumber,
      DocumentTemplateSearchTab selectedTab
  ) {
    var items = filteredDocumentTemplateItems.stream()
        .filter(dataItemDto -> applicationType == dataItemDto.applicationType()
            || Objects.isNull(dataItemDto.applicationType()))
        .toList();

    var itemPages = PageView.fromPage(
        QueryPaginationUtil.convertSearchResultsToPage(
            buildSearchResultItems(items),
            tab.equals(selectedTab) ? pageNumber : DEFAULT_PAGE_NUMBER
        ),
        ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(null, null, tab))
    );

    return new SearchTabItem(itemPages, tab.getTabView(items.size()));
  }

  private List<SearchResultItem> buildSearchResultItems(List<LmsDocumentTemplateDto> resultList) {
    return resultList.stream()
        .map(dataItemDto -> SearchResultItem.newBuilder()
            .withLinkHeadingText(dataItemDto.title())
            .withLinkHeadingUrl(dataItemDto.documentTemplateUrl())
            .withCaptionText(dataItemDto.description())
            .build())
        .toList();
  }
}

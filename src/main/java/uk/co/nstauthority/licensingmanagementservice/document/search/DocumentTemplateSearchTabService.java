package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
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
                                               DocumentTemplateSearchTab tab) {
    var continuationItems = filteredDocumentTemplateItems.stream()
        .filter(dataItemDto -> ApplicationType.CONTINUATION_APPLICATION.equals(dataItemDto.applicationType())
                || Objects.isNull(dataItemDto.applicationType()))
        .toList();

    var continuationItemPages = PageView.fromPage(
        QueryPaginationUtil.convertSearchResultsToPage(
            fromDtos(continuationItems),
            DocumentTemplateSearchTab.CONTINUATION.equals(tab) ? pageNumber : DEFAULT_PAGE_NUMBER
        ),
        ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(
            null,
            null,
            DocumentTemplateSearchTab.CONTINUATION
        ))
    );

    var tabToCount = Map.of(
        DocumentTemplateSearchTab.CONTINUATION, continuationItems.size()
    );

    return List.of(
        new SearchTabItem(continuationItemPages, DocumentTemplateSearchTab.CONTINUATION.getTabView(tabToCount))
    );
  }

  private SearchResultItem fromDto(LmsDocumentTemplateDto dataItemDto) {
    return SearchResultItem.newBuilder()
        .withLinkHeadingText(dataItemDto.title())
        .withLinkHeadingUrl(dataItemDto.documentTemplateUrl())
        .withCaptionText(dataItemDto.description())
        .build();
  }

  private List<SearchResultItem> fromDtos(List<LmsDocumentTemplateDto> resultList) {
    return resultList.stream().map(this::fromDto).toList();
  }
}

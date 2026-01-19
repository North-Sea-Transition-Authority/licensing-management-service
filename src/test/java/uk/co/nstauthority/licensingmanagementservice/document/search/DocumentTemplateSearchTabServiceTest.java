package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.query.SearchTabItem;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateSearchTabServiceTest {

  private static final String BASE_URL = "%s?tab=%s&page=%s";

  @InjectMocks
  private DocumentTemplateSearchTabService documentTemplateSearchTabService;

  @Test
  void getSearchTabItems() {
    var continuationItem = LmsDocumentTemplateDtoTestUtil.newBuilder().withApplicationType(ApplicationType.CONTINUATION_APPLICATION).build();
    var allTypesItem = LmsDocumentTemplateDtoTestUtil.newBuilder().withApplicationType(null).build();

    var continuationSearchResult = searchResultItemFromDto(continuationItem);
    var allTypesResult = searchResultItemFromDto(allTypesItem);

    var resultingTabItems =
        documentTemplateSearchTabService.getSearchTabItems(List.of(continuationItem, allTypesItem), 0, DocumentTemplateSearchTab.CONTINUATION);

    var tabToCount = Map.of(
        DocumentTemplateSearchTab.CONTINUATION, 2
    );

    assertThat(resultingTabItems)
        .extracting(
            searchTabItem -> searchTabItem.searchResultsForTab().getPageContent(),
            searchTabItem -> searchTabItem.searchResultsForTab().urlForPage(0),
            SearchTabItem::tabView
        ).containsExactly(
            tuple(
                List.of(continuationSearchResult, allTypesResult),
                BASE_URL.formatted(
                    ReverseRouter.route(on(DocumentTemplateSearchController.class)
                        .renderDocumentTemplateSearch(null, null, null, null)),
                    DocumentTemplateSearchTab.CONTINUATION,
                    0),
                DocumentTemplateSearchTab.CONTINUATION.getTabView(tabToCount)
            )
        );
  }

  private static SearchResultItem searchResultItemFromDto(LmsDocumentTemplateDto dataItemDto) {
    return SearchResultItem.newBuilder()
        .withLinkHeadingText(dataItemDto.title())
        .withLinkHeadingUrl(dataItemDto.documentTemplateUrl())
        .withCaptionText(dataItemDto.description())
        .build();
  }
}
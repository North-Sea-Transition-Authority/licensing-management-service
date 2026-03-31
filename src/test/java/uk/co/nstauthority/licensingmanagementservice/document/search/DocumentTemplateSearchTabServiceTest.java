package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class DocumentTemplateSearchTabServiceTest {

  private static final String BASE_URL = "%s?tab=%s&page=%s";

  @InjectMocks
  private DocumentTemplateSearchTabService documentTemplateSearchTabService;

  @Test
  void getSearchTabItems_continuationItems_appearsInContinuationTab() {
    var continuationItem = LmsDocumentTemplateDtoTestUtil.newBuilder().withApplicationType(ApplicationType.CONTINUATION_APPLICATION).build();
    var allTypesItem = LmsDocumentTemplateDtoTestUtil.newBuilder().withApplicationType(null).build();

    var continuationSearchResult = searchResultItemFromDto(continuationItem);
    var allTypesResult = searchResultItemFromDto(allTypesItem);

    var resultingTabItems =
        documentTemplateSearchTabService.getSearchTabItems(List.of(continuationItem, allTypesItem), 0, DocumentTemplateSearchTab.CONTINUATION);

    var tabToCount = Map.of(
        DocumentTemplateSearchTab.CONTINUATION, 2,
        DocumentTemplateSearchTab.EXTENSION_AMENDMENT, 0
    );

    var continuationTab = resultingTabItems.get(0);
    assertThat(continuationTab.searchResultsForTab().getPageContent())
        .containsExactly(continuationSearchResult, allTypesResult);
    assertThat(continuationTab.searchResultsForTab().urlForPage(0))
        .isEqualTo(BASE_URL.formatted(
            ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateSearch(null, null, null)),
            DocumentTemplateSearchTab.CONTINUATION,
            0));
    assertThat(continuationTab.tabView()).isEqualTo(DocumentTemplateSearchTab.CONTINUATION.getTabView(
        tabToCount.get(DocumentTemplateSearchTab.CONTINUATION)));
  }

  @Test
  void getSearchTabItems_extensionAmendmentItems_appearsInExtensionAmendmentTab() {
    var extensionAmendmentItem = LmsDocumentTemplateDtoTestUtil.newBuilder()
        .withApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION).build();

    var extensionAmendmentSearchResult = searchResultItemFromDto(extensionAmendmentItem);

    var resultingTabItems =
        documentTemplateSearchTabService.getSearchTabItems(List.of(extensionAmendmentItem), 0, DocumentTemplateSearchTab.EXTENSION_AMENDMENT);

    var tabToCount = Map.of(
        DocumentTemplateSearchTab.CONTINUATION, 0,
        DocumentTemplateSearchTab.EXTENSION_AMENDMENT, 1
    );

    var extensionAmendmentTab = resultingTabItems.get(1);
    assertThat(extensionAmendmentTab.searchResultsForTab().getPageContent())
        .containsExactly(extensionAmendmentSearchResult);
    assertThat(extensionAmendmentTab.searchResultsForTab().urlForPage(0))
        .isEqualTo(BASE_URL.formatted(
            ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateSearch(null, null, null)),
            DocumentTemplateSearchTab.EXTENSION_AMENDMENT,
            0));
    assertThat(extensionAmendmentTab.tabView()).isEqualTo(DocumentTemplateSearchTab.EXTENSION_AMENDMENT.getTabView(
        tabToCount.get(DocumentTemplateSearchTab.EXTENSION_AMENDMENT)));
  }

  @Test
  void getSearchTabItems_returnsTabsInCorrectOrder() {
    var resultingTabItems =
        documentTemplateSearchTabService.getSearchTabItems(List.of(), 0, DocumentTemplateSearchTab.CONTINUATION);

    assertThat(resultingTabItems)
        .extracting(searchTabItem -> searchTabItem.tabView().name())
        .containsExactly(
            DocumentTemplateSearchTab.CONTINUATION.name(),
            DocumentTemplateSearchTab.EXTENSION_AMENDMENT.name()
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
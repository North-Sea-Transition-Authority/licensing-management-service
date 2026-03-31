package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.PageView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.query.SearchTabItem;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = DocumentTemplateSearchController.class)
class DocumentTemplateSearchControllerTest extends AbstractControllerTest {

  @MockitoBean
  private DocumentTemplateSearchService documentTemplateSearchService;

  @MockitoBean
  private DocumentTemplateSearchTabService documentTemplateSearchTabService;

  private DocumentTemplateSearchSession documentTemplateSearchSession;
  private DocumentTemplateSearchFilterForm form;

  @BeforeEach
  void setUp() {
    form = new DocumentTemplateSearchFilterForm();
    documentTemplateSearchSession = new DocumentTemplateSearchSession(form);
  }

  @ParameterizedTest
  @MethodSource("getGetMappingsForSecurityTests")
  void renderDocumentTemplateSearch_whenNotLoggedIn_thenRedirectToLoginPage(String url) throws Exception {
    mockMvc.perform(
            get(url)
        )
        .andExpect(redirectionToLoginUrl());
  }

  private static Stream<Arguments> getGetMappingsForSecurityTests() {
    return Stream.of(
        Arguments.of(ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(null, null, null))),
        Arguments.of(ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .clearDocumentTemplateSearchFilters(null, null, null)))

    );
  }

  @AuthorisationSecurityTest
  void renderDocumentTemplateResults_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateResults(null, null, null)))
                .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderDocumentTemplateSearch_whenNotDocumentTemplateManager() throws Exception {
    var filteredItems = List.of(LmsDocumentTemplateDtoTestUtil.newBuilder().build());

    when(documentTemplateSearchService.getDocumentTemplateSearchItems(any(DocumentTemplateSearchFilterForm.class)))
        .thenReturn(filteredItems);

    var searchItemsByTab = getSearchItemsByTab();

    when(documentTemplateSearchTabService.getSearchTabItems(filteredItems, 0, DocumentTemplateSearchTab.CONTINUATION))
        .thenReturn(searchItemsByTab);

    var tabParam = "?tab=%s".formatted(DocumentTemplateSearchTab.CONTINUATION.getAnchor());

    when(teamQueryService.userHasStaticRole(REGULATOR_USER_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.DOCUMENT_TEMPLATE_MANAGER))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateSearch(documentTemplateSearchSession, null, null)) + tabParam)
                .with(user(regulatorUser))
                .flashAttr("form", form)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/documentTemplateSearchPage"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("searchItemsByTab", searchItemsByTab))
        .andExpect(model().attribute("tabs", searchItemsByTab.stream().map(SearchTabItem::tabView).toList()))
        .andExpect(model().attribute("selectedTab", DocumentTemplateSearchTab.CONTINUATION))
        .andExpect(model().attribute("clearFilterUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .clearDocumentTemplateSearchFilters(null, DocumentTemplateSearchTab.CONTINUATION, null))))
        .andExpect(model().attribute("controllerUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(null, null, null))));
  }

  @Test
  void renderDocumentTemplateSearch_whenDocumentTemplateManager() throws Exception {
    var filteredItems = List.of(LmsDocumentTemplateDtoTestUtil.newBuilder().build());

    when(documentTemplateSearchService.getDocumentTemplateSearchItems(any(DocumentTemplateSearchFilterForm.class)))
        .thenReturn(filteredItems);

    var searchItemsByTab = getSearchItemsByTab();

    when(documentTemplateSearchTabService.getSearchTabItems(filteredItems, 0, DocumentTemplateSearchTab.CONTINUATION))
        .thenReturn(searchItemsByTab);

    var tabParam = "?tab=%s".formatted(DocumentTemplateSearchTab.CONTINUATION.getAnchor());

    when(teamQueryService.userHasStaticRole(REGULATOR_USER_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.DOCUMENT_TEMPLATE_MANAGER))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateSearch(documentTemplateSearchSession, null, null)) + tabParam)
                .with(user(regulatorUser))
                .flashAttr("form", form)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/documentTemplateSearchPage"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("searchItemsByTab", searchItemsByTab))
        .andExpect(model().attribute("tabs", searchItemsByTab.stream().map(SearchTabItem::tabView).toList()))
        .andExpect(model().attribute("selectedTab", DocumentTemplateSearchTab.CONTINUATION))
        .andExpect(model().attribute("clearFilterUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .clearDocumentTemplateSearchFilters(null, DocumentTemplateSearchTab.CONTINUATION, null))))
        .andExpect(model().attribute("controllerUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(null, null, null))));
  }

  private List<SearchTabItem> getSearchItemsByTab() {
    var searchResultItem = SearchResultItem.newBuilder().withLinkHeadingUrl("url").build();
    var pageRequest = PageRequest.of(0, 10);
    var page = new PageImpl<>(List.of(searchResultItem), pageRequest, 1);
    var pageView = PageView.fromPage(page, ReverseRouter.route(on(DocumentTemplateSearchController.class)
        .renderDocumentTemplateSearch(null, null, null)));
    return List.of(new SearchTabItem(pageView,
        DocumentTemplateSearchTab.CONTINUATION.getTabView(
            Map.of(DocumentTemplateSearchTab.CONTINUATION, 1).get(DocumentTemplateSearchTab.CONTINUATION))));
  }

  @Test
  void renderDocumentTemplateResults() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .renderDocumentTemplateResults(null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(documentTemplateSearchSession, null, DocumentTemplateSearchTab.CONTINUATION))));
  }

  @Test
  void clearDocumentTemplateSearchFilters() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                .clearDocumentTemplateSearchFilters(null, null, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(documentTemplateSearchSession, null, DocumentTemplateSearchTab.CONTINUATION))));
  }
}
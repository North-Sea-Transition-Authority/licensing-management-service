package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@ContextConfiguration(classes = LicenceCorrectionSearchController.class)
class LicenceCorrectionSearchControllerTest extends AbstractControllerTest {

  private static final String RENDER_CORRECTION_SEARCH_ROUTE =
      ReverseRouter.route(on(LicenceCorrectionSearchController.class).renderCorrectionSearch(null, null));

  private static final String FILTER_RESULTS_ROUTE =
      ReverseRouter.route(on(LicenceCorrectionSearchController.class).filterResults(null, null));

  private static final String CLEAR_SEARCH_FILTERS_ROUTE =
      ReverseRouter.route(on(LicenceCorrectionSearchController.class).clearSearchFilters(null, null));

  private static final String SEARCH_SESSION_ATTRIBUTE = "licenceCorrectionSearchSession";

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private final ServiceUserDetail organisationUser = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private LicenceCorrectionSearchService licenceCorrectionSearchService;

  @Test
  void renderCorrectionSearch_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(get(RENDER_CORRECTION_SEARCH_ROUTE))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectionSearch_whenUserIsInRegulatorTeam_thenRendersSearchPage() throws Exception {
    var form = new LicenceCorrectionSearchFilterForm();
    form.setCorrectionStatuses(List.of("COMPLETE"));
    var searchSession = new LicenceCorrectionSearchSession(form);

    var searchItems = List.of(
        SearchResultItem.newBuilder()
            .withId("correction-id")
            .withLinkHeadingText("COR-1")
            .build()
    );

    when(teamQueryService.userIsInRegulatorTeam(regulatorUser.wuaId())).thenReturn(true);
    when(licenceCorrectionSearchService.getSearchItems(form, regulatorUser)).thenReturn(searchItems);

    mockMvc.perform(
            get(RENDER_CORRECTION_SEARCH_ROUTE)
                .with(user(regulatorUser))
                .flashAttr(SEARCH_SESSION_ATTRIBUTE, searchSession)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/correction/search/correctionSearch"))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("clearFilterUrl", CLEAR_SEARCH_FILTERS_ROUTE))
        .andExpect(model().attribute("correctionStatuses", Map.of(
            "IN_PROGRESS", "In progress",
            "COMPLETE", "Complete"
        )))
        .andExpect(model().attribute("searchItems", searchItems));
  }

  @Test
  void renderCorrectionSearch_whenUserIsNotInRegulatorTeam_thenForbidden() throws Exception {
    when(teamQueryService.userIsInRegulatorTeam(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(RENDER_CORRECTION_SEARCH_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void filterResults_whenUserIsInRegulatorTeam_thenStoresFormInSessionAndRedirects() throws Exception {
    when(teamQueryService.userIsInRegulatorTeam(regulatorUser.wuaId())).thenReturn(true);

    var result = mockMvc.perform(
            post(FILTER_RESULTS_ROUTE)
                .with(csrf())
                .with(user(regulatorUser))
                .param("correctionStatuses", "IN_PROGRESS")
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_CORRECTION_SEARCH_ROUTE))
        .andReturn();

    var expectedForm = new LicenceCorrectionSearchFilterForm();
    expectedForm.setCorrectionStatuses(List.of("IN_PROGRESS"));

    assertThat(result.getRequest().getSession().getAttribute(SEARCH_SESSION_ATTRIBUTE))
        .usingRecursiveComparison()
        .isEqualTo(new LicenceCorrectionSearchSession(expectedForm));
  }

  @Test
  void filterResults_whenUserIsNotInRegulatorTeam_thenForbidden() throws Exception {
    when(teamQueryService.userIsInRegulatorTeam(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            post(FILTER_RESULTS_ROUTE)
                .with(csrf())
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void clearSearchFilters_whenUserIsInRegulatorTeam_thenClearsSessionAndRedirects() throws Exception {
    var form = new LicenceCorrectionSearchFilterForm();
    form.setCorrectionStatuses(List.of("COMPLETE"));

    when(teamQueryService.userIsInRegulatorTeam(regulatorUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(CLEAR_SEARCH_FILTERS_ROUTE)
                .with(user(regulatorUser))
                .sessionAttr(SEARCH_SESSION_ATTRIBUTE, new LicenceCorrectionSearchSession(form))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_CORRECTION_SEARCH_ROUTE))
        .andExpect(request().sessionAttributeDoesNotExist(SEARCH_SESSION_ATTRIBUTE));
  }

  @Test
  void clearSearchFilters_whenUserIsNotInRegulatorTeam_thenForbidden() throws Exception {
    when(teamQueryService.userIsInRegulatorTeam(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(CLEAR_SEARCH_FILTERS_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }
}

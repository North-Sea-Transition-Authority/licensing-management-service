package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
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
      ReverseRouter.route(on(LicenceCorrectionSearchController.class).renderCorrectionSearch(null));

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
    var searchItems = List.of(
        SearchResultItem.newBuilder()
            .withId("correction-id")
            .withLinkHeadingText("COR-1")
            .build()
    );

    when(teamQueryService.userIsInRegulatorTeam(regulatorUser.wuaId())).thenReturn(true);
    when(licenceCorrectionSearchService.getSearchItems(regulatorUser)).thenReturn(searchItems);

    mockMvc.perform(
            get(RENDER_CORRECTION_SEARCH_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/correction/search/correctionSearch"))
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
}
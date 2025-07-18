package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = LicenceSearchController.class)
class LicenceSearchControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceSearchService licenceSearchService;

  private ServiceUserDetail organisationUser;
  public static final String RENDER_SEARCH_PAGE_ROUTE = ReverseRouter.route(on(LicenceSearchController.class).renderSearchPage(null));
  private static final String CLEARED_SEARCH_FILTERS_ROUTE = ReverseRouter.route(on(LicenceSearchController.class).clearSearchFilters(null, null));
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderSearchPage() throws Exception {
    when(licenceSearchService.getSearchResultItems(any(LicenceSearchFilterForm.class))).thenReturn(List.of());

    mockMvc.perform(
            get(RENDER_SEARCH_PAGE_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceSearch"))
        .andExpect(model().attribute("searchItems", List.of()))
        .andExpect(model().attribute("clearFilterUrl", CLEARED_SEARCH_FILTERS_ROUTE))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())));
  }

  @SecurityTest
  void renderSearchPage_whenSessionHasBeenInvoked() throws Exception {
    when(licenceSearchService.getSearchResultItems(any(LicenceSearchFilterForm.class))).thenReturn(List.of());

    var form = new LicenceSearchFilterForm();
    form.setReference("reference");
    var searchSession = new LicenceSearchSession(form);
    searchSession.update(form);

    mockMvc.perform(
            get(RENDER_SEARCH_PAGE_ROUTE)
                .with(user(organisationUser))
                .flashAttr("licenceSearchSession", searchSession)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceSearch"))
        .andExpect(model().attribute("searchItems", List.of()))
        .andExpect(model().attribute("clearFilterUrl", CLEARED_SEARCH_FILTERS_ROUTE))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())));

    verify(licenceSearchService).getSearchResultItems(form);
  }

  @SecurityTest
  void renderLicenceOverview() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(1, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceOverview"))
        .andExpect(model().attribute("pageTitle", licence.getLicenceReference()))
        .andExpect(model().attribute("caption", licence.getType().getDisplayName()));
  }

  @Test
  void filterResults() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceSearchController.class).filterResults(null, null)))
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_SEARCH_PAGE_ROUTE));
  }

  @Test
  void clearSearchFilter() throws Exception {
    mockMvc.perform(
            get(CLEARED_SEARCH_FILTERS_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(RENDER_SEARCH_PAGE_ROUTE));
  }
}
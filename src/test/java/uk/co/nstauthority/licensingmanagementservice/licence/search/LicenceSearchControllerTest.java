package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = LicenceSearchController.class)
class LicenceSearchControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceSearchService licenceSearchService;

  @MockitoBean
  private LicenceAccessService  licenceAccessService;

  @MockitoBean
  private RegulatorRoleService regulatorRoleService;

  private ServiceUserDetail organisationUser;
  public static final String RENDER_SEARCH_PAGE_ROUTE = ReverseRouter.route(on(LicenceSearchController.class).renderSearchPage(null, null));
  private static final String CLEARED_SEARCH_FILTERS_ROUTE = ReverseRouter.route(on(LicenceSearchController.class).clearSearchFilters(null, null));
  public static final String CREATE_LICENCE_ROUTE = ReverseRouter.route(on(LicenceController.class).renderNewLicenceForm());

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    when(licenceAccessService.userHasAccessToCreateLicence(ORGANISATION_USER_WUA_ID)).thenReturn(true);
    when(regulatorRoleService.isRegulator(organisationUser)).thenReturn(true);
  }

  @Test
  void renderSearchPage_whenSessionHasNotBeenInvoked() throws Exception {
    var form = new LicenceSearchFilterForm();
    var result = mockMvc.perform(
            get(RENDER_SEARCH_PAGE_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceSearch"))
        .andExpect(model().attribute("searchItems", List.of()))
        .andExpect(model().attribute("hasSearchBeenInvoked", false))
        .andExpect(model().attribute("clearFilterUrl", CLEARED_SEARCH_FILTERS_ROUTE))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("licenceStatuses",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceStatusType.class)))
        .andExpect(model().attribute("canCreateLicence", true))
        .andExpect(model().attribute("createLicenceUrl", CREATE_LICENCE_ROUTE))
        .andReturn();

    verify(licenceSearchService, never()).getSearchResultItems(form, organisationUser);

    var renderedForm = (LicenceSearchFilterForm) result.getModelAndView().getModel().get("form");
    assertThat(renderedForm.getLicenceTypes()).containsExactlyElementsOf(LicenceType.getDisplayableLicenceTypesNames());
    assertThat(renderedForm.getLicenceStatuses())
        .containsExactlyInAnyOrderElementsOf(Arrays.stream(LicenceStatusType.values()).map(Enum::name).toList());
  }

  @Test
  void renderSearchPage_whenSessionHasBeenInvoked() throws Exception {
    var form = new LicenceSearchFilterForm();
    form.setLicenceReference("reference");
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
        .andExpect(model().attribute("hasSearchBeenInvoked", true))
        .andExpect(model().attribute("clearFilterUrl", CLEARED_SEARCH_FILTERS_ROUTE))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("licenceStatuses",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceStatusType.class)))
        .andExpect(model().attribute("canCreateLicence", true))
        .andExpect(model().attribute("createLicenceUrl", CREATE_LICENCE_ROUTE));
    verify(licenceSearchService).getSearchResultItems(form, organisationUser);
  }

  @Test
  void renderSearchPage_whenRegulatorUser_isRegulatorUserIsTrue() throws Exception {
    when(regulatorRoleService.isRegulator(organisationUser)).thenReturn(true);

    mockMvc.perform(
            get(RENDER_SEARCH_PAGE_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isRegulatorUser", true));
  }

  @Test
  void renderSearchPage_whenNonRegulatorUser_isRegulatorUserIsFalse() throws Exception {
    when(regulatorRoleService.isRegulator(organisationUser)).thenReturn(false);

    mockMvc.perform(
            get(RENDER_SEARCH_PAGE_ROUTE)
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isRegulatorUser", false));
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
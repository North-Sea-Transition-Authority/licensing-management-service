package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = WorkAreaController.class)
class WorkAreaControllerTest extends AbstractControllerTest {

  @MockitoBean
  private WorkAreaService workAreaService;

  @SecurityTest
  void getWorkArea_whenNotLoggedIn_thenRedirectToLoginUrl() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @SecurityTest
  void renderWorkArea_whenNotLoggedIn_thenRedirectToLoginUrl() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(WorkAreaController.class).renderWorkAreaResults(null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @SecurityTest
  void clearWorkAreaFilters_whenNotLoggedIn_thenRedirectToLoginUrl() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void getWorkArea_whenLoggedIn_thenExpectWorkArea() throws Exception {
    var searchResultItem1 = SearchResultItem.newBuilder()
        .withLinkHeadingUrl("https://www.test.fivium/1")
        .build();
    var searchResultItem2 = SearchResultItem.newBuilder()
        .withLinkHeadingUrl("https://www.test.fivium/2")
        .build();
    var searchResultItem3 = SearchResultItem.newBuilder()
        .withLinkHeadingUrl("https://www.test.fivium/3")
        .build();

    var searchResultItems = List.of(
        searchResultItem1,
        searchResultItem2,
        searchResultItem3
    );

    when(workAreaService.getSearchResultItems(any()))
        .thenReturn(searchResultItems);

    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/workarea/workArea"))
        .andExpect(model().attribute("clearFilterUrl",
            ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null))))
        .andReturn()
        .getModelAndView();

    assertThat(modelAndView).isNotNull();
    var model = modelAndView.getModel();

    assertThat(model.get("workAreaItems")).isInstanceOf(List.class);
    var workAreaItems = (List<SearchResultItem>) model.get("workAreaItems");

    assertThat(workAreaItems).containsAll(searchResultItems);
  }

  @Test
  void getWorkArea_canStartApplication() throws Exception {
    var applicationUser = ServiceUserDetailTestUtil.newBuilder().build();
    when(teamQueryService.userHasAtLeastOneRoleIn(applicationUser.wuaId(), Set.of(Role.EDIT_APPLICATION))).thenReturn(true);
    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
                .with(user(applicationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/workarea/workArea"))
        .andExpect(model().attribute("clearFilterUrl",
            ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null))))
        .andExpect(model().attribute("canStartApplication", true))
        .andExpect(model().attribute("startApplicationUrl",
            ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType())))
        .andReturn()
        .getModelAndView();

    assertThat(modelAndView).isNotNull();
  }

  @Test
  void getWorkArea_cantStartApplication() throws Exception {
    var applicationUser = ServiceUserDetailTestUtil.newBuilder().build();
    when(teamQueryService.userHasAtLeastOneRoleIn(eq(applicationUser.wuaId()), anySet())).thenReturn(false);
    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
                .with(user(applicationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/workarea/workArea"))
        .andExpect(model().attribute("clearFilterUrl",
            ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null))))
        .andExpect(model().attribute("canStartApplication", true))
        .andReturn()
        .getModelAndView();

    assertThat(modelAndView).isNotNull();
  }

  @Test
  void renderWorkAreaResults_loggedIn_updateAndRedirect() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(WorkAreaController.class).renderWorkAreaResults(null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                ReverseRouter.route(
                    on(WorkAreaController.class)
                        .getWorkArea(null, null)))).andReturn();
  }

  @Test
  void clearWorkAreaFilters_loggedIn_clearSessionAndRedirect() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                ReverseRouter.route(
                    on(WorkAreaController.class)
                        .getWorkArea(null, null))));
  }
}

package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist.XyzApplicationTaskListController;

@ContextConfiguration(classes = StartXyzApplicationController.class)
class StartXyzApplicationControllerTest extends AbstractControllerTest {

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void getStartApplicationPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.APPLICATION_EDITOR))
    ).thenReturn(true);
    mockMvc.perform(
        get(ReverseRouter.route(on(StartXyzApplicationController.class).startApplication()))
            .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/application/startApplication"))
        .andExpect(model().attribute("actionUrl", ReverseRouter.route(on(StartXyzApplicationController.class)
            .createApplication())))
        .andExpect(model().attribute("backLinkUrl", ReverseRouter.route(on(WorkAreaController.class)
            .getWorkArea(null, null))));
  }

  @SecurityTest
  void getStartApplicationPage_withoutRequiredRole() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.APPLICATION_EDITOR))
    ).thenReturn(false);
    mockMvc.perform(
        get(ReverseRouter.route(on(StartXyzApplicationController.class).startApplication()))
            .with(user(organisationUser)))
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void createApplication() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.APPLICATION_EDITOR))
    ).thenReturn(true);
    var application = new XyzApplication(UUID.randomUUID(), "ref", "type", XyzApplicationStatus.DRAFT);
    when(xyzApplicationService.finalAllMockedApplications()).thenReturn(List.of(application));
    mockMvc.perform(
            post(ReverseRouter.route(on(StartXyzApplicationController.class).createApplication()))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(XyzApplicationTaskListController.class)
            .getTaskList(application.getId(), null,null))));
  }

  @SecurityTest
  void createApplication_withoutRequiredRole() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.APPLICATION_EDITOR))
    ).thenReturn(false);
    mockMvc.perform(
        post(ReverseRouter.route(on(StartXyzApplicationController.class).createApplication()))
            .with(user(organisationUser))
            .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}

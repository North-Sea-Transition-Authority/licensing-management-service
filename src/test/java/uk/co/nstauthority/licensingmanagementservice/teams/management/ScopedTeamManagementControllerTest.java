package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.NewOrganisationTeamFormValidator;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = ScopedTeamManagementController.class)
class ScopedTeamManagementControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private NewOrganisationTeamFormValidator newOrganisationTeamFormValidator;

  private static ServiceUserDetail invokingUser;

  @BeforeAll
  static void setUp() {
    invokingUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(1L)
        .build();
  }

  @Test
  void renderCreateNewOrgTeam() throws Exception {
    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(ScopedTeamManagementController.class).renderCreateNewOrgTeam(null)))
        .with(user(invokingUser)))
        .andExpect(status().isOk());
  }

  @SecurityTest
  void renderCreateNewOrgTeam_noAccess() throws Exception {
    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(ScopedTeamManagementController.class).renderCreateNewOrgTeam(null)))
            .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void handleCreateNewOrgTeam() throws Exception {
    var orgGroup = new OrganisationGroup();
    orgGroup.setOrganisationGroupId(50);
    orgGroup.setName("Some Org");

    var newTeam = new Team(UUID.randomUUID());

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    when(newOrganisationTeamFormValidator.validate(any(), any()))
        .thenReturn(true);

    when(organisationApi.findOrganisationGroup(eq(50), any(), any()))
        .thenReturn(Optional.of(orgGroup));

    when(teamManagementService.createScopedTeam(eq(orgGroup.getName()), eq(TeamType.ORGANISATION), refEq(TeamScopeReference.from("50", "ORGGRP"))))
        .thenReturn(newTeam);

    mockMvc.perform(post(ReverseRouter.route(on(ScopedTeamManagementController.class).handleCreateNewOrgTeam(null, null)))
        .with(csrf())
        .with(user(invokingUser))
        .param("orgGroupId", "50"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(newTeam.getId(), null))));
  }

  @Test
  void handleCreateNewOrgTeam_invalidForm() throws Exception {
    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    when(newOrganisationTeamFormValidator.validate(any(), any()))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(ScopedTeamManagementController.class).handleCreateNewOrgTeam(null, null)))
        .with(csrf())
        .with(user(invokingUser))
        .param("orgGroupId", ""))
        .andExpect(status().isOk()); // No redirect to next page

    verify(teamManagementService, never()).createScopedTeam(any(), any(), any());
  }

  @SecurityTest
  void handleCreateNewOrgTeam_noAccess() throws Exception {
    when(teamQueryService.userHasStaticRole(1L, TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(ScopedTeamManagementController.class).handleCreateNewOrgTeam(null, null)))
            .with(csrf())
            .with(user(invokingUser))
            .param("orgGroupId", ""))
        .andExpect(status().isForbidden()); // No redirect to next page

    verify(teamManagementService, never()).createScopedTeam(any(), any(), any());
  }
}

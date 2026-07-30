package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.AllowedDomainService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.AddMemberFormValidator;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.MemberRolesFormValidator;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamTypeView;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamView;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
@ContextConfiguration(classes = TeamManagementController.class)
class TeamManagementControllerTest extends AbstractControllerTest {

  @MockitoBean
  private MemberRolesFormValidator memberRolesFormValidator;

  @MockitoBean
  private AddMemberFormValidator addMemberFormValidator;

  @MockitoBean
  private EnergyPortalConfiguration energyPortalConfiguration;

  @MockitoBean
  private EnergyPortalUserService energyPortalUserService;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @MockitoBean
  private AllowedDomainService  allowedDomainService;

  private static Team regTeam;
  private static Team externalContributors;
  private static Team organisationTeam;
  private static TeamMemberView regTeamMemberView;
  private static ServiceUserDetail invokingUser;
  private static TeamMemberView applicationScopedTeamMemberView;

  @BeforeAll
  static void setUp() {
    regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);
    regTeam.setName("reg team one");

    organisationTeam = new Team(UUID.randomUUID());
    organisationTeam.setTeamType(TeamType.ORGANISATION);
    organisationTeam.setName("org team");

    regTeamMemberView = new TeamMemberView(
        1L,
        "Ms",
        "Test",
        "User",
        "test@example.com",
        "020123456",
        regTeam.getId(),
        List.of(Role.MANAGE_TEAM),
        false
    );

    externalContributors = new Team(UUID.randomUUID());
    externalContributors.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalContributors.setName("EXTERNAL CONTRIBUTORS");
    externalContributors.setScopeId(UUID.randomUUID().toString());
    externalContributors.setScopeType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name());

    applicationScopedTeamMemberView = new TeamMemberView(
        1L,
        "Ms",
        "Test",
        "User",
        "test@example.com",
        "020123456",
        regTeam.getId(),
        List.of(Role.MANAGE_TEAM),
        true
    );


    invokingUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(1L)
        .build();
  }

  @Test
  void renderTeamTypeList() throws Exception {
    when(teamManagementService.getTeamTypesUserIsMemberOf(invokingUser.wuaId()))
        .thenReturn(Set.of(TeamType.ORGANISATION, TeamType.LICENCE_MANAGEMENT));

    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var teamTypeViews = (List<TeamTypeView>) modelAndView.getModel().get("teamTypeViews");

    assertThat(teamTypeViews)
        .extracting(TeamTypeView::teamTypeName)
        .containsExactly(TeamType.LICENCE_MANAGEMENT.getDisplayName(), TeamType.ORGANISATION.getDisplayName());
  }

  @Test
  void renderTeamTypeList_singeTypeRedirects() throws Exception {
    when(teamManagementService.getTeamTypesUserIsMemberOf(invokingUser.wuaId()))
        .thenReturn(Set.of(TeamType.ORGANISATION));

    mockMvc.perform(get(ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null)))
            .with(user(invokingUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null))));
  }

  @Test
  void renderTeamTypeList_regWithOrgManageCanSeeOrgTeams() throws Exception {
    when(teamManagementService.getTeamTypesUserIsMemberOf(invokingUser.wuaId()))
        .thenReturn(Set.of(TeamType.LICENCE_MANAGEMENT));

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var teamTypeViews = (List<TeamTypeView>) modelAndView.getModel().get("teamTypeViews");

    assertThat(teamTypeViews)
        .extracting(TeamTypeView::teamTypeName)
        .containsExactly(TeamType.LICENCE_MANAGEMENT.getDisplayName(), TeamType.ORGANISATION.getDisplayName());
  }

  @Test
  void renderTeamTypeList_noManageableTeams() throws Exception {
    when(teamManagementService.getTeamTypesUserIsMemberOf(invokingUser.wuaId()))
        .thenReturn(Set.of());

    mockMvc.perform(get(ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null)))
            .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderTeamsOfType_staticTeamRedirectsToSingleInstance() throws Exception {
    when(teamManagementService.getStaticTeamOfTypeUserIsMemberOf(TeamType.LICENCE_MANAGEMENT, invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamsOfType(TeamType.LICENCE_MANAGEMENT.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null))));
  }

  @Test
  void renderTeamsOfType_singleScopedTeamRedirectsToInstance_notOrgAdmin() throws Exception {
    when(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(Set.of(organisationTeam));

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamMemberList(organisationTeam.getId(), null))));
  }

  @Test
  void renderTeamsOfType_singleScopedTeamRedirectsToInstance_isOrgAdmin() throws Exception {
    when(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(Set.of(organisationTeam));

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/teamManagement/teamInstances"))
        .andExpect(model().attributeExists("teamViews"));
  }

  @Test
  void renderTeamsOfType_scopedTeamReturnList() throws Exception {

    var firstOrganisationTeamByName = new Team(UUID.randomUUID());
    firstOrganisationTeamByName.setTeamType(TeamType.ORGANISATION);
    firstOrganisationTeamByName.setName("a team name");

    var secondOrganisationTeamByName = new Team(UUID.randomUUID());
    secondOrganisationTeamByName.setTeamType(TeamType.ORGANISATION);
    secondOrganisationTeamByName.setName("b team name");

    var thirdOrganisationTeamByName = new Team(UUID.randomUUID());
    thirdOrganisationTeamByName.setTeamType(TeamType.ORGANISATION);
    thirdOrganisationTeamByName.setName("C team name");

    when(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(Set.of(secondOrganisationTeamByName, thirdOrganisationTeamByName, firstOrganisationTeamByName));

    var modelAndView = mockMvc.perform(get(ReverseRouter.route(on(TeamManagementController.class)
            .renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var teamTypeViews = (List<TeamView>) modelAndView.getModel().get("teamViews");

    assertThat(teamTypeViews)
        .extracting(TeamView::teamName)
        .containsExactly(
            firstOrganisationTeamByName.getName(),
            secondOrganisationTeamByName.getName(),
            thirdOrganisationTeamByName.getName()
        );
  }

  @Test
  void renderTeamsOfType_noManageableTeams() throws Exception {
    when(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(Set.of());

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderTeamsOfType_noManageableTeams_orgAdminNotForbidden() throws Exception {
    when(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(Set.of());

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    var modelAndView = mockMvc.perform(get(ReverseRouter.route(on(TeamManagementController.class)
            .renderTeamsOfType(TeamType.ORGANISATION.getUrlSlug(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var createNewInstanceUrl = (String) modelAndView.getModel().get("createNewInstanceUrl");

    assertThat(createNewInstanceUrl)
        .isEqualTo(TeamType.ORGANISATION.getCreateNewInstanceRoute());
  }


  @Test
  void renderTeamMemberList_whenNotMemberOfTeam_thenForbidden() throws Exception {

    var team = regTeam;

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderTeamMemberList_whenMemberOfTeam_thenOk() throws Exception {

    var team = regTeam;

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk());
  }

  @Test
  void renderTeamMemberList_whenOrganisationTeam_andNotMemberOfTeam_andUserHasManageAnyOrganisationRole_thenOk() throws Exception {

    // GIVEN an organisation team
    var team = organisationTeam;

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    // AND the invoking user is not a direct member
    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(false);

    // WHEN the invoking user has the CREATE_MANAGE_ANY_ORGANISATION_TEAM in the regulator team
    when(teamManagementService.userCanManageAnyOrganisationTeam(invokingUser.wuaId()))
        .thenReturn(true);

    // THEN the invoking user will be able to view the team
    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk());
  }

  @Test
  void renderTeamMemberList_whenOrganisationTeam_andNotMemberOfTeam_andUserWithoutManageAnyOrganisationRole_thenForbidden() throws Exception {

    // GIVEN an organisation team
    var team = organisationTeam;

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    // AND the invoking user is not a direct member
    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(false);

    // WHEN the invoking user does not have the CREATE_MANAGE_ANY_ORGANISATION_TEAM in the regulator team
    when(teamManagementService.userCanManageAnyOrganisationTeam(invokingUser.wuaId()))
        .thenReturn(false);

    // THEN the invoking user will not be able to view the team
    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = TeamType.class, mode = EnumSource.Mode.EXCLUDE, names = "ORGANISATION")
  void renderTeamMemberList_whenNotOrganisationTeam_andNotMemberOfTeam_andCanManageAnyOrganisationRole_thenForbidden(
      TeamType nonOrganisationTeamType) throws Exception {

    // GIVEN an non-organisation team
    var team = new Team(UUID.randomUUID());
    team.setTeamType(nonOrganisationTeamType);

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    // AND the invoking user is not a direct member
    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(false);

    // WHEN the invoking user has the CREATE_MANAGE_ANY_ORGANISATION_TEAM in the regulator team
    when(teamManagementService.userCanManageAnyOrganisationTeam(invokingUser.wuaId()))
        .thenReturn(true);

    // THEN the invoking user will not be able to view the team
    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderTeamMemberList_whenIsMemberOfTeamAndTeamManager_thenAssetModelProperties() throws Exception {
    var scopeId = UUID.randomUUID();
    regTeam.setScopeId(scopeId.toString());
    regTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);

    when(teamManagementService.canManageTeam(regTeam, invokingUser.wuaId()))
        .thenReturn(true);

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.isMemberOfTeam(regTeam, invokingUser.wuaId()))
        .thenReturn(true);

    when(teamManagementService.getTeamMemberViewsForTeam(regTeam))
        .thenReturn(List.of(regTeamMemberView));

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/teamManagement/teamMembers"))
        .andExpect(model().attribute("teamName", regTeam.getName()))
        .andExpect(model().attribute("teamMemberViews", List.of(regTeamMemberView)))
        .andExpect(model().attribute("canManageTeam", true))
           .andExpect(model().attribute("backUrl", ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null))))
           .andExpect(model().attribute(
            "addMemberUrl",
            ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToTeam(regTeam.getId(), null))
        ))
        .andExpect(model().attribute("rolesInTeam", regTeam.getTeamType().getAllowedRoles()));
  }

  @Test
  void renderTeamMemberList_whenIsMemberOfTeamAndNotTeamManager_thenAssetModelProperties() throws Exception {

    when(teamManagementService.canManageTeam(regTeam, invokingUser.wuaId()))
        .thenReturn(false);

    when(teamManagementService.isMemberOfTeam(regTeam, invokingUser.wuaId()))
        .thenReturn(true);

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getTeamMemberViewsForTeam(regTeam))
        .thenReturn(List.of(regTeamMemberView));

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/teamManagement/teamMembers"))
        .andExpect(model().attribute("teamName", regTeam.getName()))
        .andExpect(model().attribute("teamMemberViews", List.of(regTeamMemberView)))
        .andExpect(model().attribute("canManageTeam", false))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null))))
        .andExpect(model().attribute(
            "addMemberUrl",
            ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToTeam(regTeam.getId(), null))
        ));
  }

  @Test
  void renderTeamMemberList_noTeamFound() throws Exception {
    var nonExistentTeamId = UUID.randomUUID();
    when(teamManagementService.getTeam(nonExistentTeamId))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Team with id %s not found".formatted(nonExistentTeamId)));

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(nonExistentTeamId, null)))
                .with(user(invokingUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderTeamMemberList_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderAddMemberToTeam() throws Exception {
    when(teamManagementService.getTeam(organisationTeam.getId()))
        .thenReturn(organisationTeam);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(List.of(organisationTeam));

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    var modelAndView = mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderAddMemberToTeam(organisationTeam.getId(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var registerUrl = (String) modelAndView.getModel().get("registerUrl");

    assertThat(registerUrl)
        .isEqualTo("https://example.com");
  }

  @Test
  void renderAddMemberToTeam_noTeamFound() throws Exception {
    var nonExistentTeamId = UUID.randomUUID();
    when(teamManagementService.getTeam(nonExistentTeamId))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Team with id %s not found".formatted(nonExistentTeamId)));

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToTeam(nonExistentTeamId, null)))
                .with(user(invokingUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderAddMemberToTeam_noAccess() throws Exception {
    when(teamManagementService.getTeam(organisationTeam.getId()))
        .thenReturn(organisationTeam);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.ORGANISATION, invokingUser.wuaId()))
        .thenReturn(List.of());

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderAddMemberToTeam(organisationTeam.getId(), null)))
            .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void handleAddMemberToTeam() throws Exception {
    var epaUser = new User.Builder()
        .webUserAccountId(999L)
        .isAccountShared(false)
        .canLogin(true)
        .build();

    when(addMemberFormValidator.isValid(any(), any())).thenReturn(true);

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    when(energyPortalUserService.findUsersByEmail("test@email", "Find user to add to team"))
        .thenReturn(List.of(EnergyPortalUserJson.from(epaUser)));

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToTeam(regTeam.getId(), null, null)))
                .with(csrf())
                .with(user(invokingUser))
                .param("emailAddress", "test@email" ))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(TeamManagementController.class).renderUserTeamRoles(regTeam.getId(), 999L, null))));
  }

  @Test
  void handleAddMemberToTeam_invalidForm() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    when(addMemberFormValidator.isValid(any(), any())).thenReturn(false);

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToTeam(regTeam.getId(), null, null)))
                .with(csrf())
                .with(user(invokingUser)))
        .andExpect(status().isOk()); // No redirect to next page
  }

  @Test
  void handleAddMemberToTeam_invalidUser() throws Exception {
    when(addMemberFormValidator.isValid(any(), any())).thenReturn(true);

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    when(energyPortalUserService.findUsersByEmail("foo", "Find user to add to team"))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToTeam(regTeam.getId(), null, null)))
                .with(csrf())
                .with(user(invokingUser))
                .param("email", "foo"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void handleAddMemberToTeam_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToTeam(regTeam.getId(), null, null)))
                .with(csrf())
                .with(user(invokingUser))
                .param("email", "foo"))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void renderUserTeamRoles(boolean isAllowed) throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    when(teamManagementService.getTeamMemberView(regTeam, 999L))
        .thenReturn(regTeamMemberView);

    when(allowedDomainService.isAllowedDomain(regTeamMemberView.email(), regTeam)).thenReturn(
        isAllowed
    );

    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderUserTeamRoles(regTeam.getId(), 999L, null)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("userHasAllowedEmail", isAllowed))
        .andReturn().getModelAndView();

    var roleMap = (Map<String, String>) modelAndView.getModel().get("rolesNamesMap");

    assertThat(roleMap)
        .containsExactly(
            Map.entry(Role.MANAGE_TEAM.name(), Role.MANAGE_TEAM.getName()),
            Map.entry(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM.name(), Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM.getName()),
            Map.entry(Role.OFFLINE_LICENCE_ADMINISTRATOR.name(), Role.OFFLINE_LICENCE_ADMINISTRATOR.getName()),
            Map.entry(Role.SCHEDULE_ADMINISTRATOR.name(), Role.SCHEDULE_ADMINISTRATOR.getName()),
            Map.entry(Role.WORK_PROGRAMME_ADMINISTRATOR.name(), Role.WORK_PROGRAMME_ADMINISTRATOR.getName()),
            Map.entry(Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR.name(), Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR.getName()),
            Map.entry(Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER.name(), Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER.getName()),
            Map.entry(Role.DOCUMENT_TEMPLATE_MANAGER.name(), Role.DOCUMENT_TEMPLATE_MANAGER.getName())
        );

    var teamMemberViewModel = (TeamMemberView) modelAndView.getModel().get("teamMemberView");
    assertThat(teamMemberViewModel).isEqualTo(regTeamMemberView);

    var rolesInTeam = (List<Role>) modelAndView.getModel().get("rolesInTeam");

    assertThat(rolesInTeam)
        .containsExactly(
            Role.MANAGE_TEAM,
            Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
            Role.OFFLINE_LICENCE_ADMINISTRATOR,
            Role.SCHEDULE_ADMINISTRATOR,
            Role.WORK_PROGRAMME_ADMINISTRATOR,
            Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR,
            Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER,
            Role.DOCUMENT_TEMPLATE_MANAGER
        );
  }

  @Test
  void renderUserTeamRoles_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());


    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderUserTeamRoles(regTeam.getId(), 999L, null)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateUserTeamRoles() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    mockMvc.perform(post(
            ReverseRouter.route(on(TeamManagementController.class).updateUserTeamRoles(regTeam.getId(), 999L, null, null, null)))
            .with(csrf())
            .with(user(invokingUser))
            .param("roles", "MANAGE_TEAM"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null))));

    verify(teamManagementService).setUserTeamRoles(999L, regTeam, List.of(Role.MANAGE_TEAM), invokingUser);
  }

  @Test
  void updateUserTeamRoles_invalidForm() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    doAnswer( invocation -> {
          BindingResult bindingResult = invocation.getArgument(3);
          bindingResult.addError(new ObjectError("Error", "error"));
          return invocation;
        }
    ).when(memberRolesFormValidator).validate(any(), eq(999L), eq(regTeam), any());

    when(teamManagementService.getTeamMemberView(regTeam, 999L))
        .thenReturn(regTeamMemberView);

    mockMvc.perform(post(
            ReverseRouter.route(on(TeamManagementController.class).updateUserTeamRoles(regTeam.getId(), 999L, null, null, null)))
            .with(csrf())
            .with(user(invokingUser))
            .param("roles", "MANAGE_TEAM"))
        .andExpect(status().isOk()); // No redirect to next page

    verify(teamManagementService, never()).setUserTeamRoles(any(), any(), any(), any());
  }

  @Test
  void updateUserTeamRoles_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    mockMvc.perform(post(
            ReverseRouter.route(on(TeamManagementController.class).updateUserTeamRoles(regTeam.getId(), 999L, null, null, null)))
            .with(csrf())
            .with(user(invokingUser))
            .param("roles", "MANAGE_TEAM"))
        .andExpect(status().isForbidden());

    verify(teamManagementService, never()).setUserTeamRoles(any(), any(), any(), any());
  }

  @Test
  void renderRemoveTeamMember() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    when(teamManagementService.getTeamMemberView(regTeam, 999L))
        .thenReturn(regTeamMemberView);

    when(teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(regTeam, 999L))
        .thenReturn(true);

    var modelAndView = mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderRemoveTeamMember(regTeam.getId(), 999L)))
                .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    var teamMemberViewModel = (TeamMemberView) modelAndView.getModel().get("teamMemberView");
    var teamName = (String) modelAndView.getModel().get("teamName");
    var canRemoveTeamMember = (boolean) modelAndView.getModel().get("canRemoveTeamMember");

    assertThat(teamMemberViewModel).isEqualTo(regTeamMemberView);
    assertThat(teamName).isEqualTo(regTeam.getName());
    assertThat(canRemoveTeamMember).isTrue();
  }

  @Test
  void renderRemoveTeamMember_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(TeamManagementController.class).renderRemoveTeamMember(regTeam.getId(), 999L)))
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void handleRemoveTeamMember() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleRemoveTeamMember(regTeam.getId(), 999L)))
                .with(csrf())
                .with(user(invokingUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(regTeam.getId(), null))));

    verify(teamManagementService).removeUserFromTeam(999L, regTeam);
  }

  @Test
  void handleRemoveTeamMember_noAccess() throws Exception {
    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    mockMvc.perform(
            post(ReverseRouter.route(on(TeamManagementController.class).handleRemoveTeamMember(regTeam.getId(), 999L)))
                .with(csrf())
                .with(user(invokingUser)))
        .andExpect(status().isForbidden());

    verify(teamManagementService, never()).removeUserFromTeam(any(), any());
  }

  @Test
  void renderExternalContributorsTeamList() throws Exception {
    var id = UUID.fromString(externalContributors.getScopeId());

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(id);

    ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();

    when(teamManagementService.canManageTeam(externalContributors, invokingUser.wuaId())).thenReturn(false);
    when(teamManagementService.isMemberOfTeam(externalContributors, invokingUser.wuaId())).thenReturn(true);
    when(teamManagementService.getTeam(externalContributors.getId())).thenReturn(externalContributors);

    when(teamManagementService.getTeamMemberViewsForTeam(externalContributors))
        .thenReturn(List.of(applicationScopedTeamMemberView));

    when(scheduleWorkProgrammeApplicationService.getScheduleWorkProgrammeApplicationById(id))
        .thenReturn(scheduleWorkProgrammeApplication);

    when(scheduleWorkProgrammeApplicationService.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(scheduleWorkProgrammeApplication))
        .thenReturn(scheduleWorkProgrammeApplicationDetail);

    externalContributors.setScopeType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name());

    mockMvc
        .perform(get(ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(
            externalContributors.getId(), null))).with(
            user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/teamManagement/teamMembers"))
        .andExpect(model().attribute("teamName", externalContributors.getName()))
        .andExpect(model().attribute("teamMemberViews", List.of(applicationScopedTeamMemberView)))
        .andExpect(model().attribute("canManageTeam", false))
        .andExpect(model().attribute("cancelUrl",
            ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(
                scheduleWorkProgrammeApplicationDetail.getId(), null))
        ))
        .andExpect(model().attribute("saveAndCompleteUrl",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
                scheduleWorkProgrammeApplicationDetail.getId(), null, null))
        ))
        .andExpect(model().attribute("currentEndPoint",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(null, null, null))
        ))
        .andExpect(model().attribute("addMemberUrl",
            ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToScheduleExternalContributorsTeam(
                externalContributors.getId(), null))
        ));
  }

  @Test
  void renderContinuationExternalContributorsTeamList() throws Exception {
    var id = UUID.fromString(externalContributors.getScopeId());

    LicenceContinuationApplication licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(id);

    LicenceContinuationApplicationDetail licenceContinuationApplicationDetail =  LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();

    when(teamManagementService.canManageTeam(externalContributors, invokingUser.wuaId())).thenReturn(false);
    when(teamManagementService.isMemberOfTeam(externalContributors, invokingUser.wuaId())).thenReturn(true);
    when(teamManagementService.getTeam(externalContributors.getId())).thenReturn(externalContributors);

    when(teamManagementService.getTeamMemberViewsForTeam(externalContributors))
        .thenReturn(List.of(applicationScopedTeamMemberView));

    when(licenceContinuationService.getDetailByIdOrThrow(id))
        .thenReturn(licenceContinuationApplicationDetail);

    externalContributors.setScopeType(ApplicationType.CONTINUATION_APPLICATION.name());

    mockMvc.perform(get(ReverseRouter.route(
            on(TeamManagementController.class).renderExternalContributorsTeamList(
                externalContributors.getId(),
                null)))
            .with(user(invokingUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/teamManagement/teamMembers"))
        .andExpect(model().attribute("teamName", externalContributors.getName()))
        .andExpect(model().attribute("teamMemberViews", List.of(applicationScopedTeamMemberView)))
        .andExpect(model().attribute("canManageTeam", false))
        .andExpect(model().attribute("cancelUrl",
                                     ReverseRouter.route(on(LicenceContinuationExternalContributorController.class).renderForm(
                                         licenceContinuationApplicationDetail.getId(), null))
        ))
        .andExpect(model().attribute("saveAndCompleteUrl",
                                     ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
                                         licenceContinuationApplicationDetail.getId(), null, null))
        ))
        .andExpect(model().attribute("currentEndPoint",
                                     ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(null, null, null))
        ))
        .andExpect(model().attribute("addMemberUrl",
                                     ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToScheduleExternalContributorsTeam(
                                         externalContributors.getId(), null))
        ));
  }

  @Test
  void renderAddMemberToScheduleExternalContributorsTeam() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    var modelAndView = mockMvc.perform(get(ReverseRouter.route(
                                  on(TeamManagementController.class).renderAddMemberToScheduleExternalContributorsTeam(
                                      externalContributors.getId(), null)))
                                  .with(user(invokingUser)))
                              .andExpect(status().isOk())
                              .andReturn().getModelAndView();

    var registerUrl = (String) modelAndView.getModel().get("registerUrl");

    assertThat(registerUrl)
        .isEqualTo("https://example.com");
  }

  @Test
  void renderAddMemberToScheduleExternalContributorsTeam_noTeamFound() throws Exception {
    var nonExistentTeamId = UUID.randomUUID();
    when(teamManagementService.getTeam(nonExistentTeamId))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Team with id %s not found".formatted(nonExistentTeamId)));

    mockMvc.perform(
               get(ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToScheduleExternalContributorsTeam(nonExistentTeamId, null)))
                   .with(user(invokingUser)))
           .andExpect(status().isNotFound());
  }

  @Test
  void renderAddMemberToScheduleExternalContributorsTeam_noAccess() throws Exception {
    when(teamManagementService.getTeam(organisationTeam.getId()))
        .thenReturn(organisationTeam);

    when(teamManagementService.canManageTeam(externalContributors, invokingUser.wuaId()))
        .thenReturn(true);

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    mockMvc.perform(get(ReverseRouter.route(
               on(TeamManagementController.class).renderAddMemberToScheduleExternalContributorsTeam(organisationTeam.getId(), null)))
               .with(user(invokingUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void handleAddMemberToScheduleExternalContributorsTeam() throws Exception {
    var epaUser = new User.Builder()
        .webUserAccountId(999L)
        .isAccountShared(false)
        .canLogin(true)
        .build();

    when(addMemberFormValidator.isValid(any(), any())).thenReturn(true);

    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(energyPortalUserService.findUsersByEmail("test@email", "Find user to add to team"))
        .thenReturn(List.of(EnergyPortalUserJson.from(epaUser)));

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToScheduleExternalContributorsTeam(
                   externalContributors.getId(), null, null)))
                   .with(csrf())
                   .with(user(invokingUser))
                   .param("emailAddress", "test@email" ))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(
               ReverseRouter.route(on(TeamManagementController.class).renderUserScheduleExternalContributorsTeamRoles(
                   externalContributors.getId(), 999L, null))));
  }

  @Test
  void handleAddMemberToScheduleExternalContributorsTeam_invalidForm() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.EXTERNAL_CONTRIBUTORS, invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(addMemberFormValidator.isValid(any(), any())).thenReturn(false);

    when(energyPortalConfiguration.registrationUrl())
        .thenReturn("https://example.com");

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToScheduleExternalContributorsTeam(
                   externalContributors.getId(), null, null)))
                   .with(csrf())
                   .with(user(invokingUser)))
           .andExpect(status().isOk());
  }

  @Test
  void handleAddMemberToScheduleExternalContributorsTeam_invalidUser() throws Exception {
    when(addMemberFormValidator.isValid(any(), any())).thenReturn(true);

    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(energyPortalUserService.findUsersByEmail("foo", "Find user to add to team"))
        .thenReturn(List.of());

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToScheduleExternalContributorsTeam(
                   externalContributors.getId(), null, null)))
                   .with(csrf())
                   .with(user(invokingUser))
                   .param("email", "foo"))
           .andExpect(status().isBadRequest());
  }

  @Test
  void handleAddMemberToScheduleExternalContributorsTeam_noAccess() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleAddMemberToScheduleExternalContributorsTeam(
                   externalContributors.getId(), null, null)))
                   .with(csrf())
                   .with(user(invokingUser))
                   .param("email", "foo"))
           .andExpect(status().isForbidden());
  }

  @Test
  void renderUserScheduleExternalContributorsTeamRoles() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(teamManagementService.getTeamMemberView(externalContributors, 999L))
        .thenReturn(applicationScopedTeamMemberView);

    var modelAndView = mockMvc.perform(
                                  get(ReverseRouter.route(on(TeamManagementController.class).renderUserScheduleExternalContributorsTeamRoles(
                                      externalContributors.getId(), 999L, null)))
                                      .with(user(invokingUser)))
                              .andExpect(status().isOk())
                              .andReturn().getModelAndView();

    var roleMap = (Map<String, String>) modelAndView.getModel().get("rolesNamesMap");

    assertThat(roleMap)
        .containsExactly(
            Map.entry(Role.EXTERNAL_APPLICATION_EDITOR.name(), Role.EXTERNAL_APPLICATION_EDITOR.getName()),
            Map.entry(Role.EXTERNAL_APPLICATION_VIEWER.name(), Role.EXTERNAL_APPLICATION_VIEWER.getName())
        );

    var teamMemberViewModel = modelAndView.getModel().get("teamMemberView");
    assertThat(teamMemberViewModel).isEqualTo(applicationScopedTeamMemberView);

    var rolesInTeam = (List<Role>) modelAndView.getModel().get("rolesInTeam");

    assertThat(rolesInTeam)
        .containsExactly(
            Role.EXTERNAL_APPLICATION_EDITOR,
            Role.EXTERNAL_APPLICATION_VIEWER
        );
  }

  @Test
  void renderUserScheduleExternalContributorsTeamRoles_noAccess() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    mockMvc.perform(
               get(ReverseRouter.route(on(TeamManagementController.class).renderUserScheduleExternalContributorsTeamRoles(
                   externalContributors.getId(), 999L, null)))
                   .with(user(invokingUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void updateUserScheduleExternalContributorsTeamRoles() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    mockMvc.perform(post(
               ReverseRouter.route(on(TeamManagementController.class).updateUserScheduleExternalContributorsTeamRoles(
                   externalContributors.getId(), 999L, null, null, null)))
               .with(csrf())
               .with(user(invokingUser))
               .param("roles", "MANAGE_TEAM"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(
               ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(
                   externalContributors.getId(), null))));

    verify(teamManagementService).setUserTeamRoles(999L, externalContributors, List.of(Role.MANAGE_TEAM), invokingUser);
  }

  @Test
  void updateUserScheduleExternalContributorsTeamRoles_invalidForm() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    doAnswer( invocation -> {
          BindingResult bindingResult = invocation.getArgument(3);
          bindingResult.addError(new ObjectError("Error", "error"));
          return invocation;
        }
    ).when(memberRolesFormValidator).validate(any(), eq(999L), eq(externalContributors), any());

    when(teamManagementService.getTeamMemberView(externalContributors, 999L))
        .thenReturn(applicationScopedTeamMemberView);

    mockMvc.perform(post(
               ReverseRouter.route(on(TeamManagementController.class).updateUserScheduleExternalContributorsTeamRoles(
                   externalContributors.getId(), 999L, null, null, null)))
               .with(csrf())
               .with(user(invokingUser))
               .param("roles", "MANAGE_TEAM"))
           .andExpect(status().isOk());

    verify(teamManagementService, never()).setUserTeamRoles(any(), any(), any(), any());
  }

  @Test
  void updateUserScheduleExternalContributorsTeamRoles_noAccess() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    mockMvc.perform(post(
               ReverseRouter.route(on(TeamManagementController.class).updateUserScheduleExternalContributorsTeamRoles(
                   externalContributors.getId(), 999L, null, null, null)))
               .with(csrf())
               .with(user(invokingUser))
               .param("roles", "MANAGE_TEAM"))
           .andExpect(status().isForbidden());

    verify(teamManagementService, never()).setUserTeamRoles(any(), any(), any(), any());
  }

  @Test
  void renderRemoveScheduleExternalContributorsTeamMember() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    when(teamManagementService.getTeamMemberView(externalContributors, 999L))
        .thenReturn(applicationScopedTeamMemberView);

    when(teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(externalContributors, 999L))
        .thenReturn(true);

    var modelAndView = mockMvc.perform(
                                  get(ReverseRouter.route(on(TeamManagementController.class).renderRemoveScheduleExternalContributorsTeamMember(
                                      externalContributors.getId(), 999L)))
                                      .with(user(invokingUser)))
                              .andExpect(status().isOk())
                              .andReturn().getModelAndView();

    var teamMemberViewModel = (TeamMemberView) modelAndView.getModel().get("teamMemberView");
    var teamName = (String) modelAndView.getModel().get("teamName");
    var canRemoveTeamMember = (boolean) modelAndView.getModel().get("canRemoveTeamMember");

    assertThat(teamMemberViewModel).isEqualTo(applicationScopedTeamMemberView);
    assertThat(teamName).isEqualTo(externalContributors.getName());
    assertThat(canRemoveTeamMember).isTrue();
  }

  @Test
  void renderRemoveScheduleExternalContributorsTeamMember_noAccess() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    mockMvc.perform(
               get(ReverseRouter.route(on(TeamManagementController.class).renderRemoveScheduleExternalContributorsTeamMember(
                   externalContributors.getId(), 999L)))
                   .with(user(invokingUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void handleRemoveScheduleExternalContributorsTeamMember() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(externalContributors));

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleRemoveScheduleExternalContributorsTeamMember(
                   externalContributors.getId(), 999L)))
                   .with(csrf())
                   .with(user(invokingUser)))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(
               ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(
                   externalContributors.getId(), null))));

    verify(teamManagementService).removeUserFromTeam(999L, externalContributors);
  }

  @Test
  void handleRemoveScheduleExternalContributorsTeamMember_noAccess() throws Exception {
    when(teamManagementService.getTeam(externalContributors.getId()))
        .thenReturn(externalContributors);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(externalContributors.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    mockMvc.perform(
               post(ReverseRouter.route(on(TeamManagementController.class).handleRemoveScheduleExternalContributorsTeamMember(
                   externalContributors.getId(), 999L)))
                   .with(csrf())
                   .with(user(invokingUser)))
           .andExpect(status().isForbidden());

    verify(teamManagementService, never()).removeUserFromTeam(any(), any());
  }
}
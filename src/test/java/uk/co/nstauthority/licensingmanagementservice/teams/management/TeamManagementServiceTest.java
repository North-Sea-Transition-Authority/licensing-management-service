package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.starter.accounts.EnergyPortalServiceAccessService;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalAccountsMessagePublishingService;
import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.UserCancelledEvent;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class TeamManagementServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private EnergyPortalServiceAccessService energyPortalServiceAccessService;

  @Mock
  private EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private ApplicationAccessService  applicationAccessService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private TeamManagementService teamManagementService;

  @Captor
  private ArgumentCaptor<Team> teamArgumentCaptor;
  @Captor
  private ArgumentCaptor<List<TeamRole>> teamRoleListCaptor;

  static final String PORTAL_USER_LOOKUP_PURPOSE = "Fetch user in team";
  static final String PORTAL_USERS_LOOKUP_PURPOSE = "Fetch users in team";
  static final String PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE = "Validate user account";

  private static Team regTeam;
  private static Team orgTeam1;
  private static Team orgTeam2;

  private static Team externalTeam1;
  private static Team externalTeam2;

  private static TeamRole regTeamUser1RoleManage;
  private static TeamRole regTeamUser1RoleOrgAdmin;
  private static TeamRole regTeamUser2RoleOrgAdmin;

  private static TeamRole orgTeam1User1RoleManage;
  private static TeamRole orgTeam2User1RoleManage;

  private static final Long USER_1_WUA_ID = 1L;
  private static User user1;
  private static final Long USER_2_WUA_ID = 2L;
  private static User user2;

  private static final ServiceUserDetail userDetail = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(10L)
      .build();

  @BeforeAll
  static void setUp() {
    regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);
    regTeamUser1RoleManage = new TeamRole();
    regTeamUser1RoleManage.setTeam(regTeam);
    regTeamUser1RoleManage.setWuaId(USER_1_WUA_ID);
    regTeamUser1RoleManage.setRole(Role.MANAGE_TEAM);

    regTeamUser1RoleOrgAdmin = new TeamRole();
    regTeamUser1RoleOrgAdmin.setTeam(regTeam);
    regTeamUser1RoleOrgAdmin.setWuaId(USER_1_WUA_ID);
    regTeamUser1RoleOrgAdmin.setRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);

    regTeamUser2RoleOrgAdmin = new TeamRole();
    regTeamUser2RoleOrgAdmin.setTeam(regTeam);
    regTeamUser2RoleOrgAdmin.setWuaId(USER_2_WUA_ID);
    regTeamUser2RoleOrgAdmin.setRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);

    orgTeam1 = new Team(UUID.randomUUID());
    orgTeam1.setTeamType(TeamType.ORGANISATION);
    orgTeam1User1RoleManage = new TeamRole();
    orgTeam1User1RoleManage.setTeam(orgTeam1);
    orgTeam1User1RoleManage.setWuaId(USER_1_WUA_ID);
    orgTeam1User1RoleManage.setRole(Role.MANAGE_TEAM);

    orgTeam2 = new Team(UUID.randomUUID());
    orgTeam2.setTeamType(TeamType.ORGANISATION);
    orgTeam2User1RoleManage = new TeamRole();
    orgTeam2User1RoleManage.setTeam(orgTeam2);
    orgTeam2User1RoleManage.setWuaId(USER_1_WUA_ID);
    orgTeam2User1RoleManage.setRole(Role.MANAGE_TEAM);

    externalTeam1 = TeamTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withTeamType(TeamType.EXTERNAL_CONTRIBUTORS)
        .build();

    externalTeam2 = TeamTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withTeamType(TeamType.EXTERNAL_CONTRIBUTORS)
        .build();

    user1 = new User();
    user1.setWebUserAccountId(USER_1_WUA_ID);
    user1.setTitle("Ms");
    user1.setForename("User");
    user1.setSurname("One");
    user1.setPrimaryEmailAddress("one@example.com");
    user1.setTelephoneNumber("1");
    user1.setCanLogin(true);
    user1.setIsAccountShared(false);

    user2 = new User();
    user2.setWebUserAccountId(USER_2_WUA_ID);
    user2.setTitle("Mr");
    user2.setForename("User");
    user2.setSurname("Two");
    user2.setPrimaryEmailAddress("two@example.com");
    user2.setTelephoneNumber("2");
    user2.setCanLogin(true);
    user2.setIsAccountShared(false);

  }

  private void allowAllRolesFor(TeamType teamType) {
    when(teamQueryService.getAvailableRoles(teamType)).thenReturn(teamType.getAllowedRoles());
  }

  @Test
  void createScopedTeam() {
    var scopeRef = TeamScopeReference.from("1", "OU");

    teamManagementService.createScopedTeam("foo", TeamType.ORGANISATION, scopeRef);

    verify(teamRepository).save(teamArgumentCaptor.capture());
    var newTeam = teamArgumentCaptor.getValue();

    assertThat(newTeam.getName()).isEqualTo("foo");
    assertThat(newTeam.getTeamType()).isEqualTo(TeamType.ORGANISATION);
    assertThat(newTeam.getScopeType()).isEqualTo(scopeRef.getType());
    assertThat(newTeam.getScopeId()).isEqualTo(scopeRef.getId());
  }

  @Test
  void createScopedTeam_wrongType() {
    var scopeRef = TeamScopeReference.from("1", "OU");

    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.createScopedTeam("foo", TeamType.LICENCE_MANAGEMENT, scopeRef));
    verify(teamRepository, never()).save(any());
  }

  @Test
  void createScopedTeam_alreadyExists() {
    var scopeRef = TeamScopeReference.from("1", "OU");

    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, "OU", "1"))
        .thenReturn(Optional.of(orgTeam1));

    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.createScopedTeam("foo", TeamType.ORGANISATION, scopeRef));

    verify(teamRepository, never()).save(any());
  }

  @Test
  void updateTeamName(){
    var newName = "New Team Name";

    teamManagementService.updateTeamName(orgTeam1, newName);
    verify(teamRepository).save(teamArgumentCaptor.capture());

    assertThat(teamArgumentCaptor.getValue().getName()).isEqualTo(newName);
  }

  @Test
  void getTeamTypesUserIsMemberOf() {

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(regTeamUser1RoleManage, orgTeam1User1RoleManage, orgTeam2User1RoleManage));

    assertThat(teamManagementService.getTeamTypesUserIsMemberOf(USER_1_WUA_ID))
        .containsExactlyInAnyOrder(TeamType.LICENCE_MANAGEMENT, TeamType.ORGANISATION);
  }

  @Test
  void getStaticTeamOfTypeUserCanManage() {
    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of(regTeamUser1RoleManage, orgTeam1User1RoleManage, orgTeam2User1RoleManage));

    assertThat(teamManagementService.getStaticTeamOfTypeUserCanManage(TeamType.LICENCE_MANAGEMENT, USER_1_WUA_ID))
        .hasValue(regTeam);
  }

  @Test
  void getStaticTeamOfTypeUserCanManage_notStatic() {
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.getStaticTeamOfTypeUserCanManage(TeamType.ORGANISATION, USER_1_WUA_ID));
  }

  @Test
  void getScopedTeamOfTypeUserCanManage() {
    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of(regTeamUser1RoleManage, orgTeam1User1RoleManage));

    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(false);

    assertThat(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.ORGANISATION, USER_1_WUA_ID))
        .containsExactlyInAnyOrder(orgTeam1);
  }

  @Test
  void getScopedTeamOfTypeUserCanManage_regulatorWithRoleCanManageAllOrgs() {
    // User has direct manage team role in reg team and org team 1
    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of(regTeamUser1RoleManage, orgTeam1User1RoleManage));

    // User has the special create/manage any org team priv
    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    // There are 2 org teams
    when(teamRepository.findByTeamType(TeamType.ORGANISATION))
        .thenReturn(List.of(orgTeam1, orgTeam2));

    // Verify they can manage both org team 1 and 2
    assertThat(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.ORGANISATION, USER_1_WUA_ID))
        .containsExactlyInAnyOrder(orgTeam1, orgTeam2);
  }

  @Test
  void getScopedTeamOfTypeUserCanManage_regulatorWithRoleCanManageAllExternalContributors() {
    when(userDetailService.getUserDetail()).thenReturn(userDetail);
    when(applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(userDetail)).thenReturn(true);
    when(teamRepository.findByTeamType(TeamType.EXTERNAL_CONTRIBUTORS)).thenReturn(List.of(externalTeam1, externalTeam2));

    assertThat(teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.EXTERNAL_CONTRIBUTORS, 10L))
        .containsExactlyInAnyOrder(externalTeam1, externalTeam2);
  }

  @Test
  void getScopedTeamOfTypeUserCanManage_notScoped() {
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.getScopedTeamsOfTypeUserCanManage(TeamType.LICENCE_MANAGEMENT, USER_1_WUA_ID));
  }

  @Test
  void getEnergyPortalUser() {
    teamManagementService.getEnergyPortalUser("foo");
    verify(energyPortalUserService).findUsersByEmail(eq("foo"), anyString());
  }

  @Test
  void getEnergyPortalUser_whenMultipleUsersFound_thenThrow() {
    var emailAddress = "foo";

    when(energyPortalUserService.findUsersByEmail(eq(emailAddress), anyString()))
        .thenReturn(List.of(EnergyPortalUserTestUtil.newBuilder().buildJson(), EnergyPortalUserTestUtil.newBuilder().buildJson()));

    assertThatThrownBy(() -> teamManagementService.getEnergyPortalUser("foo"))
        .isInstanceOf(TeamManagementException.class)
        .hasMessage("More than one UK Energy Portal user exists with the email address %s".formatted(emailAddress)
        );
  }

  @Test
  void getTeam() {
    var uuid = UUID.randomUUID();
    when(teamRepository.findById(uuid))
        .thenReturn(Optional.of(regTeam));

    assertThat(teamManagementService.getTeam(uuid))
        .isEqualTo(regTeam);
  }

  @Test
  void getTeamMemberView() {
    when(teamRoleRepository.findByWuaIdAndTeam(USER_1_WUA_ID, regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage, regTeamUser1RoleOrgAdmin));

    var user = new User();
    user.setWebUserAccountId(1L);
    user.setTitle("Ms");
    user.setForename("Foo");
    user.setSurname("Bar");
    user.setPrimaryEmailAddress("text@example.com");
    user.setTelephoneNumber("012345678");
    user.setCanLogin(true);
    user.setIsAccountShared(false);

    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(1L), PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(Optional.of(EnergyPortalUserJson.from(user)));

    var teamMemberView = teamManagementService.getTeamMemberView(regTeam, USER_1_WUA_ID);

    assertThat(teamMemberView.wuaId()).isEqualTo(user.getWebUserAccountId());
    assertThat(teamMemberView.title()).isEqualTo(user.getTitle());
    assertThat(teamMemberView.forename()).isEqualTo(user.getForename());
    assertThat(teamMemberView.surname()).isEqualTo(user.getSurname());
    assertThat(teamMemberView.email()).isEqualTo(user.getPrimaryEmailAddress());
    assertThat(teamMemberView.telNo()).isEqualTo(user.getTelephoneNumber());
    assertThat(teamMemberView.teamId()).isEqualTo(regTeam.getId());
    assertThat(teamMemberView.roles()).containsExactlyInAnyOrder(regTeamUser1RoleManage.getRole(),
        regTeamUser1RoleOrgAdmin.getRole());
  }

  @Test
  void getTeamMemberViewsForTeam() {

    // the list returns roles not in the order declared in the TeamType enum
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleOrgAdmin, regTeamUser1RoleManage, regTeamUser2RoleOrgAdmin));

    when(energyPortalUserService.findByWuaIds(
        List.of(
            WebUserAccountId.from(1L),
            WebUserAccountId.from(2L)
        ), PORTAL_USERS_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(user1), EnergyPortalUserJson.from(user2)));

    var teamMemberViews = teamManagementService.getTeamMemberViewsForTeam(regTeam);

    assertThat(teamMemberViews)
        .extracting(
            TeamMemberView::wuaId,
            TeamMemberView::title,
            TeamMemberView::forename,
            TeamMemberView::surname,
            TeamMemberView::email,
            TeamMemberView::telNo,
            TeamMemberView::teamId,
            TeamMemberView::roles
        )
        .containsExactly(
            tuple(
                user1.getWebUserAccountId(),
                user1.getTitle(),
                user1.getForename(),
                user1.getSurname(),
                user1.getPrimaryEmailAddress(),
                user1.getTelephoneNumber(),
                regTeam.getId(),
                List.of(regTeamUser1RoleManage.getRole(),regTeamUser1RoleOrgAdmin.getRole())
            ),
            tuple(
                user2.getWebUserAccountId(),
                user2.getTitle(),
                user2.getForename(),
                user2.getSurname(),
                user2.getPrimaryEmailAddress(),
                user2.getTelephoneNumber(),
                regTeam.getId(),
                List.of(regTeamUser2RoleOrgAdmin.getRole())
            )
        );
  }

  @Test
  void setUserTeamRoles() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(1L), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.of(EnergyPortalUserJson.from(user1)));
    when(teamRoleRepository.findAllByWuaId(1L)).thenReturn(List.of(new TeamRole()));

    teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam,
        List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM), userDetail);

    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_1_WUA_ID, regTeam);
    verify(teamRoleRepository).saveAll(teamRoleListCaptor.capture());

    assertThat(teamRoleListCaptor.getValue()).extracting(TeamRole::getTeam)
        .contains(regTeam, regTeam);
    assertThat(teamRoleListCaptor.getValue()).extracting(TeamRole::getWuaId)
        .contains(USER_1_WUA_ID, USER_1_WUA_ID);
    assertThat(teamRoleListCaptor.getValue()).extracting(TeamRole::getRole)
        .contains(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);

    verify(energyPortalAccountsMessagePublishingService).publishUsersRolesForTeam(
        USER_1_WUA_ID,
        regTeam.getId().toString(),
        regTeam.getTeamType().name(),
        Set.of(Role.MANAGE_TEAM.name(), Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM.name())
    );
    verify(energyPortalServiceAccessService, never()).addUser(anyLong());
    verify(energyPortalServiceAccessService, never()).removeUser(anyLong());
  }

  @Test
  void setUserTeamRole_isNewUser() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(USER_1_WUA_ID), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.of(EnergyPortalUserJson.from(user1)));
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(), List.of(new TeamRole()));

    teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam,
        List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM), userDetail);

    verify(energyPortalAccountsMessagePublishingService).publishUsersRolesForTeam(
        USER_1_WUA_ID,
        regTeam.getId().toString(),
        regTeam.getTeamType().name(),
        Set.of(Role.MANAGE_TEAM.name(), Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM.name())
    );
    verify(energyPortalServiceAccessService).addUser(USER_1_WUA_ID);
    verify(energyPortalServiceAccessService, never()).removeUser(anyLong());
  }

  @Test
  void setUserTeamRoles_whenNoTeamRolesLeft_thenRemoveEpasAccess() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(USER_1_WUA_ID), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.of(EnergyPortalUserJson.from(user1)));
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(new TeamRole()), List.of());

    teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam, List.of(), userDetail);

    verify(energyPortalServiceAccessService).removeUser(USER_1_WUA_ID);
    verify(energyPortalServiceAccessService, never()).addUser(anyLong());
  }

  @Test
  void setUserTeamRoles_invalidRoles() {
    // Covers both roles that do not belong to the team type and roles held back until a later release phase
    when(teamQueryService.getAvailableRoles(TeamType.LICENCE_MANAGEMENT))
        .thenReturn(List.of(Role.MANAGE_TEAM));

    var roleList = List.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(
            () -> teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam, roleList, userDetail));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(any(), any());
    verify(teamRoleRepository, never()).saveAll(any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishUsersRolesForTeam(
        anyLong(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void setUserTeamRoles_noEpaUser() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(1L), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.empty());

    var roleList = List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam, roleList, userDetail));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(any(), any());
    verify(teamRoleRepository, never()).saveAll(any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishUsersRolesForTeam(
        anyLong(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void setUserTeamRoles_sharedAccount() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    var epaUser = new User();
    epaUser.setIsAccountShared(true);

    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(1L), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.empty());

    var roleList = List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam, roleList, userDetail));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(any(), any());
    verify(teamRoleRepository, never()).saveAll(any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishUsersRolesForTeam(
        anyLong(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void setUserTeamRoles_canNotLogin() {
    allowAllRolesFor(TeamType.LICENCE_MANAGEMENT);
    var epaUser = new User();
    epaUser.setCanLogin(false);

    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(1L), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE))
        .thenReturn(Optional.empty());

    var roleList = List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);
    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.setUserTeamRoles(USER_1_WUA_ID, regTeam, roleList, userDetail));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(any(), any());
    verify(teamRoleRepository, never()).saveAll(any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishUsersRolesForTeam(
        anyLong(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void removeUserFromTeam() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    when(teamRoleRepository.findAllByWuaId(2L)).thenReturn(List.of(new TeamRole()));

    teamManagementService.removeUserFromTeam(USER_2_WUA_ID, regTeam);
    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_2_WUA_ID, regTeam);

    verify(energyPortalAccountsMessagePublishingService).publishRemoveUserFromTeam(
        USER_2_WUA_ID,
        regTeam.getId().toString()
    );
    verify(energyPortalServiceAccessService, never()).removeUser(anyLong());
  }

  @Test
  void removeUserFromTeam_userRemovedFromAllTeams() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    when(teamRoleRepository.findAllByWuaId(2L)).thenReturn(Collections.emptyList());

    teamManagementService.removeUserFromTeam(USER_2_WUA_ID, regTeam);

    verify(energyPortalAccountsMessagePublishingService).publishRemoveUserFromTeam(
        USER_2_WUA_ID,
        regTeam.getId().toString()
    );
    verify(energyPortalServiceAccessService).removeUser(USER_2_WUA_ID);
  }

  @Test
  void removeUserFromTeam_lastTeamManager() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    assertThatExceptionOfType(TeamManagementException.class)
        .isThrownBy(() -> teamManagementService.removeUserFromTeam(USER_1_WUA_ID, regTeam));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(anyLong(), any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishRemoveUserFromTeam(
        anyLong(),
        any()
    );
  }

  @Test
  void removeAllUsersFromTeam_removesEachDistinctMember() {
    var externalTeam1User1Role = new TeamRole();
    externalTeam1User1Role.setTeam(externalTeam1);
    externalTeam1User1Role.setWuaId(USER_1_WUA_ID);
    externalTeam1User1Role.setRole(Role.EXTERNAL_APPLICATION_EDITOR);

    var externalTeam1User2Role = new TeamRole();
    externalTeam1User2Role.setTeam(externalTeam1);
    externalTeam1User2Role.setWuaId(USER_2_WUA_ID);
    externalTeam1User2Role.setRole(Role.EXTERNAL_APPLICATION_VIEWER);

    when(teamRoleRepository.findByTeam(externalTeam1))
        .thenReturn(List.of(externalTeam1User1Role, externalTeam1User2Role));
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID)).thenReturn(Collections.emptyList());
    when(teamRoleRepository.findAllByWuaId(USER_2_WUA_ID)).thenReturn(Collections.emptyList());

    teamManagementService.removeAllUsersFromTeam(externalTeam1);

    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_1_WUA_ID, externalTeam1);
    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_2_WUA_ID, externalTeam1);
    verify(energyPortalAccountsMessagePublishingService)
        .publishRemoveUserFromTeam(USER_1_WUA_ID, externalTeam1.getId().toString());
    verify(energyPortalAccountsMessagePublishingService)
        .publishRemoveUserFromTeam(USER_2_WUA_ID, externalTeam1.getId().toString());
    verify(energyPortalServiceAccessService).removeUser(USER_1_WUA_ID);
    verify(energyPortalServiceAccessService).removeUser(USER_2_WUA_ID);
  }

  @Test
  void removeAllUsersFromTeam_whenNoMembers_doesNothing() {
    when(teamRoleRepository.findByTeam(externalTeam2)).thenReturn(Collections.emptyList());

    teamManagementService.removeAllUsersFromTeam(externalTeam2);

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(anyLong(), any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishRemoveUserFromTeam(anyLong(), any());
  }

  @Test
  void onUserCancelledEvent() {
    when(teamRoleRepository.findAllByWuaId(USER_2_WUA_ID))
        .thenReturn(List.of(regTeamUser2RoleOrgAdmin), List.of());

    teamManagementService.onUserCancelledEvent(new UserCancelledEvent(USER_2_WUA_ID));

    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_2_WUA_ID, regTeam);
    verify(energyPortalAccountsMessagePublishingService).publishRemoveUserFromTeam(
        USER_2_WUA_ID,
        regTeam.getId().toString()
    );
    verify(energyPortalServiceAccessService).removeUser(USER_2_WUA_ID);
  }

  @Test
  void onUserCancelledEvent_userNotOnAnyTeams() {
    when(teamRoleRepository.findAllByWuaId(USER_2_WUA_ID)).thenReturn(List.of());

    teamManagementService.onUserCancelledEvent(new UserCancelledEvent(USER_2_WUA_ID));

    verify(teamRoleRepository, never()).deleteByWuaIdAndTeam(anyLong(), any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishRemoveUserFromTeam(anyLong(), any());
    verify(energyPortalServiceAccessService, never()).removeUser(anyLong());
  }

  @Test
  void onUserCancelledEvent_lastTeamManager_guardBypassed() {
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(regTeamUser1RoleManage), List.of());

    teamManagementService.onUserCancelledEvent(new UserCancelledEvent(USER_1_WUA_ID));

    verify(teamRoleRepository).deleteByWuaIdAndTeam(USER_1_WUA_ID, regTeam);
    verify(energyPortalAccountsMessagePublishingService).publishRemoveUserFromTeam(
        USER_1_WUA_ID,
        regTeam.getId().toString()
    );
  }

  @Test
  void willManageTeamRoleBePresentAfterMemberRoleUpdate() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    assertThat(teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(regTeam, USER_2_WUA_ID,
        List.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)))
        .isTrue();
  }

  @Test
  void willManageTeamRoleBePresentAfterMemberRoleUpdate_newRolesIncludeManage() {
    assertThat(teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(regTeam, USER_1_WUA_ID,
        List.of(Role.MANAGE_TEAM, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)))
        .isTrue();
  }

  @Test
  void willManageTeamRoleBePresentAfterMemberRoleUpdate_noManageRoleLeft() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    assertThat(teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(regTeam, USER_1_WUA_ID,
        List.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)))
        .isFalse();
  }

  @Test
  void willManageTeamRoleBePresentAfterMemberRemoval() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    assertThat(teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(regTeam, USER_2_WUA_ID))
        .isTrue();
  }

  @Test
  void willManageTeamRoleBePresentAfterMemberRemoval_noManageRoleLeft() {
    when(teamRoleRepository.findByTeam(regTeam))
        .thenReturn(List.of(regTeamUser1RoleManage));

    assertThat(teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(regTeam, USER_1_WUA_ID))
        .isFalse();
  }

  @Test
  void doesScopedTeamWithReferenceExist_existingTeam() {
    var scopeRef = TeamScopeReference.from("1", "OU");

    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, "OU", "1"))
        .thenReturn(Optional.of(orgTeam1));

    assertThat(teamManagementService.doesScopedTeamWithReferenceExist(TeamType.ORGANISATION, scopeRef))
        .isTrue();
  }

  @Test
  void doesScopedTeamWithReferenceExist_noExistingTeam() {
    var scopeRef = TeamScopeReference.from("1", "OU");

    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, "OU", "1"))
        .thenReturn(Optional.empty());

    assertThat(teamManagementService.doesScopedTeamWithReferenceExist(TeamType.ORGANISATION, scopeRef))
        .isFalse();
  }

  @Test
  void userCanManageAnyOrganisationTeam_whenHasRole_thenTrue() {

    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    assertThat(teamManagementService.userCanManageAnyOrganisationTeam(USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userCanManageAnyOrganisationTeam_whenNoRole_thenFalse() {

    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(false);

    assertThat(teamManagementService.userCanManageAnyOrganisationTeam(USER_1_WUA_ID)).isFalse();
  }

  @Test
  void isMemberOfTeam_whenMemberOfTeam_thenTrue() {

    when(teamRoleRepository.existsByTeamAndWuaId(regTeam, USER_1_WUA_ID))
        .thenReturn(true);

    assertThat(teamManagementService.isMemberOfTeam(regTeam, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void isMemberOfTeam_whenNotMemberOfTeam_thenFalse() {

    when(teamRoleRepository.existsByTeamAndWuaId(regTeam, USER_1_WUA_ID))
        .thenReturn(false);

    assertThat(teamManagementService.isMemberOfTeam(regTeam, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void canManageTeam_whenScopedTeam_andCanManageTeam_thenTrue() {

    var scopedTeam = new Team(UUID.randomUUID());
    scopedTeam.setTeamType(TeamType.ORGANISATION);

    var teamRole = new TeamRole();
    teamRole.setTeam(scopedTeam);
    teamRole.setRole(Role.MANAGE_TEAM);

    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of(teamRole));

    assertThat(teamManagementService.canManageTeam(scopedTeam, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void canManageTeam_whenScopedTeam_andCannotManageTeam_thenFalse() {

    var scopedTeam = new Team(UUID.randomUUID());
    scopedTeam.setTeamType(TeamType.ORGANISATION);

    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of());

    assertThat(teamManagementService.canManageTeam(scopedTeam, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void canManageTeam_whenOrganisationScopedTeam_andCannotManageTeam_andHasManageAnyOrganisationTeamRole_thenTrue() {

    // GIVEN a scoped organisation team
    var scopedTeam = new Team(UUID.randomUUID());
    scopedTeam.setTeamType(TeamType.ORGANISATION);

    // AND the user doesn't have the manage team permission in that team
    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of());

    // WHEN the user has the CREATE_MANAGE_ANY_ORGANISATION_TEAM role in the regulator team
    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    when(teamRepository.findByTeamType(TeamType.ORGANISATION))
        .thenReturn(List.of(scopedTeam));

    // THEN the user can manage the team
    assertThat(teamManagementService.canManageTeam(scopedTeam, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void canManageTeam_whenStaticTeam_andCanManageTeam_thenTrue() {

    var staticTeam = new Team((UUID.randomUUID()));
    staticTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);

    var teamRole = new TeamRole();
    teamRole.setTeam(staticTeam);
    teamRole.setRole(Role.MANAGE_TEAM);

    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of(teamRole));

    assertThat(teamManagementService.canManageTeam(staticTeam, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void canManageTeam_whenStaticTeam_andCannotManageTeam_thenFalse() {

    var staticTeam = new Team((UUID.randomUUID()));
    staticTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);

    when(teamRoleRepository.findByWuaIdAndRole(USER_1_WUA_ID, Role.MANAGE_TEAM))
        .thenReturn(List.of());

    assertThat(teamManagementService.canManageTeam(staticTeam, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void getStaticTeamOfTypeUserIsMemberOf_whenScopedTeamType_thenException() {

    var scopedTeamType = TeamType.ORGANISATION;

    assertThatThrownBy(() -> teamManagementService.getStaticTeamOfTypeUserIsMemberOf(scopedTeamType, USER_1_WUA_ID))
        .isInstanceOf(TeamManagementException.class);
  }

  @Test
  void getStaticTeamOfTypeUserIsMemberOf_whenNotMemberOfTeamOfType_thenEmptyOptional() {

    var staticTeamType = TeamType.LICENCE_MANAGEMENT;

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of());

    var resultingTeam = teamManagementService.getStaticTeamOfTypeUserIsMemberOf(staticTeamType, USER_1_WUA_ID);

    assertThat(resultingTeam).isEmpty();
  }

  @Test
  void getStaticTeamOfTypeUserIsMemberOf_whenMemberOfTeamOfType_thenTeamReturned() {

    var staticTeamType = TeamType.LICENCE_MANAGEMENT;

    var expectedTeam = new Team(UUID.randomUUID());
    expectedTeam.setTeamType(staticTeamType);

    var teamRole = new TeamRole();
    teamRole.setTeam(expectedTeam);

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(teamRole));

    var resultingTeam = teamManagementService.getStaticTeamOfTypeUserIsMemberOf(staticTeamType, USER_1_WUA_ID);

    assertThat(resultingTeam).contains(expectedTeam);
  }

  @Test
  void getScopedTeamsOfTypeUserIsMemberOf_whenStaticTeamType_thenException() {

    var staticTeamType = TeamType.LICENCE_MANAGEMENT;

    assertThatThrownBy(() -> teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(staticTeamType, USER_1_WUA_ID))
        .isInstanceOf(TeamManagementException.class);
  }

  @Test
  void getScopedTeamsOfTypeUserIsMemberOf_whenUserNotMemberOfAnyTeamOfType_thenEmptySetReturned() {

    var scopedTeamType = TeamType.ORGANISATION;

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of());

    var resultingScopedTeams = teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(scopedTeamType, USER_1_WUA_ID);

    assertThat(resultingScopedTeams).isEmpty();
  }

  @Test
  void getScopedTeamsOfTypeUserIsMemberOf_whenUserMemberOfTeamOfType_thenScopedTeamsReturned() {

    var scopedTeamType = TeamType.ORGANISATION;

    var firstTeamOfType = new Team(UUID.randomUUID());
    firstTeamOfType.setTeamType(scopedTeamType);

    var firstRoleForFirstTeam = new TeamRole();
    firstRoleForFirstTeam.setTeam(firstTeamOfType);
    firstRoleForFirstTeam.setRole(Role.MANAGE_TEAM);

    var secondRoleForFirstTeam = new TeamRole();
    secondRoleForFirstTeam.setTeam(firstTeamOfType);
    secondRoleForFirstTeam.setRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);

    var secondTeamOfType = new Team(UUID.randomUUID());
    secondTeamOfType.setTeamType(scopedTeamType);

    var firstRoleForSecondTeam = new TeamRole();
    firstRoleForSecondTeam.setTeam(secondTeamOfType);
    firstRoleForSecondTeam.setRole(Role.MANAGE_TEAM);

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(firstRoleForSecondTeam, firstRoleForFirstTeam, secondRoleForFirstTeam));

    var resultingScopedTeams = teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(scopedTeamType, USER_1_WUA_ID);

    assertThat(resultingScopedTeams)
        .containsExactlyInAnyOrder(firstTeamOfType, secondTeamOfType);
  }

  @Test
  void getScopedTeamsOfTypeUserIsMemberOf_whenUserHasManageAnyOrganisationTeamRole_thenAllOrganisationTeamsReturned() {

    var scopedTeamType = TeamType.ORGANISATION;

    var teamUserIsMemberOf = new Team(UUID.randomUUID());
    teamUserIsMemberOf.setTeamType(scopedTeamType);

    var roleForTeamUserIsMemberOf = new TeamRole();
    roleForTeamUserIsMemberOf.setTeam(teamUserIsMemberOf);
    roleForTeamUserIsMemberOf.setRole(Role.MANAGE_TEAM);

    var teamUserIsNotMemberOf = new Team(UUID.randomUUID());
    teamUserIsNotMemberOf.setTeamType(scopedTeamType);

    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID))
        .thenReturn(List.of(roleForTeamUserIsMemberOf));

    when(teamQueryService.userHasStaticRole(USER_1_WUA_ID, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(true);

    when(teamRepository.findByTeamType(scopedTeamType))
        .thenReturn(List.of(teamUserIsNotMemberOf, teamUserIsMemberOf));

    var resultingScopedTeams = teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(scopedTeamType, USER_1_WUA_ID);

    assertThat(resultingScopedTeams)
        .containsExactlyInAnyOrder(teamUserIsNotMemberOf, teamUserIsMemberOf);
  }

  @Test
  void getReassignUserOptions_hasResults() {
    var currentAssigneeWuaId = 1L;
    var currentAssigneeTeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(currentAssigneeWuaId)
        .withTeam(regTeam)
        .build();

    var otherUserTeamRole = TeamRoleTestUtil.newBuilder()
        .withTeam(regTeam)
        .withWuaId(2L)
        .build();
    var epaUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(2)
        .withForename("Forename")
        .withSurname("Surname")
        .withEmailAddress("Email")
        .build();

    var organisationTeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(3L)
        .withTeam(orgTeam1)
        .build();

    when(teamRoleRepository.findAllByRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)).thenReturn(
        List.of(currentAssigneeTeamRole, otherUserTeamRole, organisationTeamRole));
    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(2L)), PORTAL_USERS_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(epaUser)));

    var expectedResult = Map.of("2", "Forename Surname (Email)");

    assertThat(teamManagementService.getReassignUserOptions(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, currentAssigneeWuaId, TeamType.LICENCE_MANAGEMENT))
        .isEqualTo(expectedResult);
  }

  @Test
  void getReassignUserOptions_whenCurrentAssigneeIsNull_hasResults() {
    var otherUserTeamRole = TeamRoleTestUtil.newBuilder()
        .withTeam(regTeam)
        .withWuaId(2L)
        .build();
    var epaUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(2)
        .withForename("Forename")
        .withSurname("Surname")
        .withEmailAddress("Email")
        .build();


    when(teamRoleRepository.findAllByRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)).thenReturn(
        List.of(otherUserTeamRole));
    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(2L)), PORTAL_USERS_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(epaUser)));

    var expectedResult = Map.of("2", "Forename Surname (Email)");

    assertThat(teamManagementService.getReassignUserOptions(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, null, TeamType.LICENCE_MANAGEMENT))
        .isEqualTo(expectedResult);
  }

  @Test
  void getReassignUserOptions_noResults() {
    var currentAssigneeWuaId = 1L;

    when(teamRoleRepository.findAllByRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)).thenReturn(List.of());
    when(energyPortalUserService.findByWuaIds(List.of(), PORTAL_USERS_LOOKUP_PURPOSE))
        .thenReturn(List.of());

    assertThat(teamManagementService.getReassignUserOptions(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, currentAssigneeWuaId, TeamType.LICENCE_MANAGEMENT))
        .isEmpty();
  }

  @Test
  void getActiveTeamMembersViewsForTeamAndRole_whenActiveMembersExist_thenReturnList() {
    var teamId = UUID.randomUUID();
    var team = new Team(teamId);
    team.setTeamType(TeamType.LICENCE_MANAGEMENT);
    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(1L).build();
    var user = EnergyPortalUserJson.from(EnergyPortalUserTestUtil.newBuilder().canLogin(true).build());
    var expectedTeamMemberView = TeamMemberView.fromEpaUser(user, teamId, List.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM),regTeam);

    when(teamRoleRepository.findAllByTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(List.of(teamRole));
    when(energyPortalUserService.getByWuaId(refEq(WebUserAccountId.from(1L)), eq(PORTAL_USER_LOOKUP_PURPOSE)))
        .thenReturn(user);

    assertThat(teamManagementService.getActiveTeamMembersViewsForTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .isEqualTo(List.of(expectedTeamMemberView));
  }

  @Test
  void getActiveTeamMembersViewsForTeamAndRole_whenNoMemberIsActive_thenReturnEmptyList() {
    var teamId = UUID.randomUUID();
    var team = new Team(teamId);
    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(1L).build();
    var user = EnergyPortalUserJson.from(EnergyPortalUserTestUtil.newBuilder().canLogin(false).build());

    when(teamRoleRepository.findAllByTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(List.of(teamRole));
    when(energyPortalUserService.getByWuaId(refEq(WebUserAccountId.from(1L)), eq(PORTAL_USER_LOOKUP_PURPOSE)))
        .thenReturn(user);

    assertThat(teamManagementService.getActiveTeamMembersViewsForTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .isEmpty();
  }

  @Test
  void getActiveTeamMembersViewsForTeamAndRole_whenNoTeamRoles_thenReturnEmptyList() {
    var teamId = UUID.randomUUID();
    var team = new Team(teamId);
    when(teamRoleRepository.findAllByTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .thenReturn(List.of());

    assertThat(teamManagementService.getActiveTeamMembersViewsForTeamAndRole(team, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .isEmpty();
  }

  @Test
  void getScopedTeams() {
    var team = TeamTestUtil.newBuilder().build();
    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeIdIn(TeamType.ORGANISATION, "OU", List.of("scope ref")))
        .thenReturn(List.of(team));
    assertThat(teamManagementService.getScopedTeams(TeamType.ORGANISATION, "OU", List.of("scope ref")))
        .isEqualTo(List.of(team));
  }
}
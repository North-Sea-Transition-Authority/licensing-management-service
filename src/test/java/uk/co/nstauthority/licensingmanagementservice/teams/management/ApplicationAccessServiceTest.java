package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService.ORGANISATION_GROUP;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @Mock
  private UserDetailService userDetailsService;

  @InjectMocks
  private ApplicationAccessService applicationAccessService;

  private static final Long USER_1_WUA_ID = 1L;
  private ServiceUserDetail organisationUser;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(USER_1_WUA_ID)
        .build();
  }

  @Test
  void userHasAccessToApplication_whenExternalContributor_Continuation_returnsTrue() {
    String appId = "123";

    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(appId);
    externalTeam.setScopeType(ApplicationType.CONTINUATION_APPLICATION.name());

    TeamRole role = new TeamRole();
    role.setTeam(externalTeam);
    role.setRole(Role.EXTERNAL_APPLICATION_EDITOR);


    when(userDetailsService.getUserDetail()).thenReturn(organisationUser);
    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(appId, ApplicationType.CONTINUATION_APPLICATION, null, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenExternalContributor_Continuation_returnsFalse() {
    String appId = "123";

    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(appId);
    externalTeam.setScopeType(ApplicationType.CONTINUATION_APPLICATION.name());

    TeamRole role = new TeamRole();
    role.setTeam(externalTeam);
    role.setRole(Role.VIEW_ANY_LICENCE);

    when(userDetailsService.getUserDetail()).thenReturn(organisationUser);
    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(appId, ApplicationType.CONTINUATION_APPLICATION, null, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenExternalContributor_returnsTrue() {
    String appId = "123";

    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(appId);
    externalTeam.setScopeType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name());

    TeamRole role = new TeamRole();
    role.setTeam(externalTeam);
    role.setRole(Role.EXTERNAL_APPLICATION_EDITOR);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));
    when(organisationUnitQueryService.findOrganisationGroupIdsByUnitId(100)).thenReturn(List.of());

    assertThat(applicationAccessService.userHasAccessToApplication(appId, ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, 100, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenOrganisationGroupMember_returnsTrue() {
    Integer orgUnitId = 100;
    String groupId = "999";

    when(organisationUnitQueryService.findOrganisationGroupIdsByUnitId(orgUnitId))
        .thenReturn(List.of(999));

    Team orgTeam = new Team(UUID.randomUUID());
    orgTeam.setTeamType(TeamType.ORGANISATION);
    orgTeam.setScopeId(groupId);
    orgTeam.setScopeType(ORGANISATION_GROUP);

    TeamRole role = new TeamRole();
    role.setTeam(orgTeam);
    role.setRole(Role.APPLICATION_SUBMITTER);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(
        applicationAccessService.userHasAccessToApplication("123", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, orgUnitId, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenNoRelevantRole_returnsFalse() {
    Team team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    team.setScopeId("123");

    TeamRole role = new TeamRole();
    role.setTeam(team);
    role.setRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));
    when(organisationUnitQueryService.findOrganisationGroupIdsByUnitId(100)).thenReturn(List.of());

    assertThat(applicationAccessService.userHasAccessToApplication("123", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, 100, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsTrue() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.ORGANISATION)
        .withScopeType(ScopeType.ORGANISATION_GROUP.name())
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId()))
        .thenReturn(Set.of(teamRole));

    boolean userHasEditorOrSubmitterRoleInOrganisationGroup = applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(
        organisationUser);

    assertTrue(userHasEditorOrSubmitterRoleInOrganisationGroup);
  }

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsFalse() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.LICENCE_MANAGEMENT)
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId()))
        .thenReturn(Set.of(teamRole));

    boolean userHasEditorOrSubmitterRoleInOrganisationGroup = applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(organisationUser);

    assertFalse(userHasEditorOrSubmitterRoleInOrganisationGroup);
  }

  @Test
  void getOrganisationUnitIds_returnsCorrectUnitIds() {
    int unitId1 = 5001;
    int unitId2 = 5002;
    var unitJson1 = new OrganisationUnitJson(unitId1, "Unit 1");
    var unitJson2 = new OrganisationUnitJson(unitId2, "Unit 2");

    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(any())).thenReturn(List.of(unitJson1, unitJson2));

    var result = applicationAccessService.getOrganisationUnitIds(organisationUser);
    assertThat(result).containsExactlyInAnyOrder(unitId1, unitId2);
  }

  @Test
  void getAuthorizedOrganisationUnitIds_whenNoGroups_returnsEmptySet() {
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of())).thenReturn(List.of());

    var result = applicationAccessService.getOrganisationUnitIds(organisationUser);

    assertThat(result).isEmpty();
  }
}
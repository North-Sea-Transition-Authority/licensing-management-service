package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private ApplicationAccessService applicationAccessService;

  private static final Long USER_1_WUA_ID = 1L;

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
    orgTeam.setScopeType("ORGANISATION");

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
}
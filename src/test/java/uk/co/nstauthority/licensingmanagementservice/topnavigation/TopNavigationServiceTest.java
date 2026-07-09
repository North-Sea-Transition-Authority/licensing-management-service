package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class TopNavigationServiceTest {

  private static final Long USER_WUA_ID = 100L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(USER_WUA_ID)
      .build();

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private TopNavigationService topNavigationService;

  @Test
  void getTopNavigationItems_withoutLoggedInUser() {
    when(userDetailService.isUserLoggedIn()).thenReturn(false);

    var topNavigationItems = topNavigationService.getTopNavigationItems();

    assertThat(topNavigationItems).isEmpty();
  }

  @Test
  void getTopNavigationItems_whenUserIsInLicenceManagementTeam() {
    var teamRole = getTeamRole(TeamType.LICENCE_MANAGEMENT, Role.MANAGE_TEAM);

    givenLoggedInUserWithRoles(Set.of(teamRole));

    var topNavigationItems = topNavigationService.getTopNavigationItems();

    assertThat(topNavigationItems).containsExactly(
        TopNavigationItem.WORK_AREA,
        TopNavigationItem.LICENCES,
        TopNavigationItem.TEAMS,
        TopNavigationItem.LICENCE_CONTACTS,
        TopNavigationItem.DOCUMENT_LIBRARY
    );
  }

  @Test
  void getTopNavigationItems_whenUserIsInNonLicenceManagementTeam_thenDoesntIncludeDocumentLibrary() {
    var teamRole = getTeamRole(TeamType.OFFSHORE_PRODUCTION_LICENSING, Role.MANAGE_TEAM);

    givenLoggedInUserWithRoles(Set.of(teamRole));

    var topNavigationItems = topNavigationService.getTopNavigationItems();

    assertThat(topNavigationItems).containsExactly(
        TopNavigationItem.WORK_AREA,
        TopNavigationItem.LICENCES,
        TopNavigationItem.TEAMS,
        TopNavigationItem.LICENCE_CONTACTS
    );
  }

  @Test
  void getTopNavigationItems_whenUserHasNoRoles_thenOnlyIncludesUnrestrictedItems() {
    givenLoggedInUserWithRoles(Set.of());

    var topNavigationItems = topNavigationService.getTopNavigationItems();

    assertThat(topNavigationItems).containsExactly(
        TopNavigationItem.WORK_AREA,
        TopNavigationItem.LICENCES,
        TopNavigationItem.TEAMS,
        TopNavigationItem.LICENCE_CONTACTS
    );
  }

  private static TeamRole getTeamRole(TeamType teamType, Role role) {
    var team = new Team();
    team.setTeamType(teamType);

    return TeamRoleTestUtil.newBuilder()
        .withTeam(team)
        .withRole(role)
        .withWuaId(USER_WUA_ID)
        .build();
  }

  private void givenLoggedInUserWithRoles(Set<TeamRole> teamRoles) {
    when(userDetailService.isUserLoggedIn()).thenReturn(true);
    when(userDetailService.getUserDetail()).thenReturn(USER);
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(teamRoles);
  }
}

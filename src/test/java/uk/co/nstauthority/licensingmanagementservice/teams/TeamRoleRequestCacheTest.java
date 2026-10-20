package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamRoleRequestCacheTest {

  private static final long USER_1_WUA_ID = 1L;
  private static final long USER_2_WUA_ID = 2L;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @InjectMocks
  private TeamRoleRequestCache teamRoleRequestCache;

  @Test
  void getTeamRolesForUser_whenCalledRepeatedlyForSameUser_thenQueriesOnce() {
    var teamRoles = List.of(TeamRoleTestUtil.newBuilder().withWuaId(USER_1_WUA_ID).withRole(Role.APPLICATION_EDITOR).build());
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID)).thenReturn(teamRoles);

    var firstResult = teamRoleRequestCache.getTeamRolesForUser(USER_1_WUA_ID);
    var secondResult = teamRoleRequestCache.getTeamRolesForUser(USER_1_WUA_ID);

    assertThat(firstResult).isEqualTo(teamRoles);
    assertThat(secondResult).isEqualTo(teamRoles);
    verify(teamRoleRepository, times(1)).findAllByWuaId(USER_1_WUA_ID);
  }

  @Test
  void getTeamRolesForUser_whenCalledForDifferentUsers_thenQueriesEachUser() {
    var user1TeamRoles = List.of(TeamRoleTestUtil.newBuilder().withWuaId(USER_1_WUA_ID).withRole(Role.APPLICATION_EDITOR).build());
    var user2TeamRoles = List.of(TeamRoleTestUtil.newBuilder().withWuaId(USER_2_WUA_ID).withRole(Role.MANAGE_TEAM).build());
    when(teamRoleRepository.findAllByWuaId(USER_1_WUA_ID)).thenReturn(user1TeamRoles);
    when(teamRoleRepository.findAllByWuaId(USER_2_WUA_ID)).thenReturn(user2TeamRoles);

    var user1Result = teamRoleRequestCache.getTeamRolesForUser(USER_1_WUA_ID);
    var user2Result = teamRoleRequestCache.getTeamRolesForUser(USER_2_WUA_ID);

    assertThat(user1Result).isEqualTo(user1TeamRoles);
    assertThat(user2Result).isEqualTo(user2TeamRoles);
  }
}

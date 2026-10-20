package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamRoleRequestCacheTest {

  private static final long WUA_ID = 1L;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @InjectMocks
  private TeamRoleRequestCache teamRoleRequestCache;

  @Test
  void getTeamRolesForUser() {
    var teamRoles = List.of(TeamRoleTestUtil.newBuilder().withWuaId(WUA_ID).withRole(Role.APPLICATION_EDITOR).build());
    when(teamRoleRepository.findAllByWuaId(WUA_ID)).thenReturn(teamRoles);

    var result = teamRoleRequestCache.getTeamRolesForUser(WUA_ID);

    assertThat(result).isEqualTo(teamRoles);
  }
}

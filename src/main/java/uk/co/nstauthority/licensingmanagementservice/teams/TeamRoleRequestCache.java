package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds each user's team roles for the lifetime of a single HTTP request, so the access rules, the top navigation and
 * the page being rendered share one lookup. Used internally by {@link TeamQueryService}; go through that service rather
 * than using this directly.
 */
@Component
@RequestScope
public class TeamRoleRequestCache {

  private final TeamRoleRepository teamRoleRepository;
  private final Map<Long, List<TeamRole>> teamRolesByWuaId = new HashMap<>();

  public TeamRoleRequestCache(TeamRoleRepository teamRoleRepository) {
    this.teamRoleRepository = teamRoleRepository;
  }

  public List<TeamRole> getTeamRolesForUser(long wuaId) {
    return teamRolesByWuaId.computeIfAbsent(wuaId, teamRoleRepository::findAllByWuaId);
  }
}

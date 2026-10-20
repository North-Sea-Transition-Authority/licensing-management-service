package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds each user's team roles for the lifetime of a single HTTP request, so the access rules, the top navigation and
 * the page being rendered share one lookup. Used internally by {@link TeamQueryService}; go through that service rather
 * than using this directly.
 */
@Component
public class TeamRoleRequestCache extends RequestScopedCache<Long, List<TeamRole>> {

  private final TeamRoleRepository teamRoleRepository;

  public TeamRoleRequestCache(TeamRoleRepository teamRoleRepository) {
    this.teamRoleRepository = teamRoleRepository;
  }

  public List<TeamRole> getTeamRolesForUser(long wuaId) {
    return get(wuaId);
  }

  @Override
  protected List<TeamRole> load(Long wuaId) {
    return teamRoleRepository.findAllByWuaId(wuaId);
  }
}

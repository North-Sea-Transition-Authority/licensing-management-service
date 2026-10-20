package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;

@Service
public class TeamQueryService {
  private final TeamRepository teamRepository;
  private final TeamRoleRepository teamRoleRepository;
  private final FeatureFlagService featureFlagService;
  private final TeamRoleRequestCache teamRoleRequestCache;

  public TeamQueryService(
      TeamRepository teamRepository,
      TeamRoleRepository teamRoleRepository,
      FeatureFlagService featureFlagService,
      TeamRoleRequestCache teamRoleRequestCache
  ) {
    this.teamRepository = teamRepository;
    this.teamRoleRepository = teamRoleRepository;
    this.featureFlagService = featureFlagService;
    this.teamRoleRequestCache = teamRoleRequestCache;
  }

  public List<Role> getAvailableRoles(TeamType teamType) {
    return featureFlagService.filterEnabled(teamType.getAllowedRoles());
  }

  public boolean userHasStaticRole(Long wuaId, TeamType teamType, Role role) {
    return userHasAtLeastOneStaticRole(wuaId, teamType, Set.of(role));
  }

  public boolean userHasAtLeastOneStaticRole(Long wuaId, TeamType teamType, Set<Role> roles) {
    assertRolesValidForTeamType(roles, teamType);
    if (teamType.isScoped()) {
      throw new IllegalArgumentException("TeamType %s is not static".formatted(teamType));
    }

    return getUserTeamRoles(wuaId).stream()
        .anyMatch(teamRole -> teamRole.getTeam().getTeamType() == teamType && roles.contains(teamRole.getRole()));
  }

  public boolean userHasScopedRole(Long wuaId, TeamType teamType, TeamScopeReference scopeRef, Role role) {
    return userHasAtLeastOneScopedRole(wuaId, teamType, scopeRef, Set.of(role));
  }

  public boolean userHasAtLeastOneScopedRole(Long wuaId, TeamType teamType, TeamScopeReference scopeRef, Set<Role> roles) {
    assertRolesValidForTeamType(roles, teamType);
    if (!teamType.isScoped()) {
      throw new IllegalArgumentException("TeamType %s is not scoped".formatted(teamType));
    }

    return getUserTeamRoles(wuaId).stream()
        .anyMatch(teamRole -> teamRole.getTeam().getTeamType() == teamType
            && Objects.equals(teamRole.getTeam().getScopeType(), scopeRef.getType())
            && Objects.equals(teamRole.getTeam().getScopeId(), scopeRef.getId())
            && roles.contains(teamRole.getRole()));
  }

  public boolean userHasAtLeastOneRoleIn(Long wuaId, Set<Role> roles) {
    return getUserTeamRoles(wuaId).stream()
        .anyMatch(teamRole -> roles.contains(teamRole.getRole()));
  }

  public boolean userHasRoleInTeamType(Long wuaId, TeamType teamType, Set<Role> roles) {
    assertRolesValidForTeamType(roles, teamType);
    return getUserTeamRoles(wuaId).stream()
        .anyMatch(teamRole -> teamRole.getTeam().getTeamType() == teamType && roles.contains(teamRole.getRole()));
  }

  public Team getStaticTeam(TeamType teamType) {
    if (teamType.isScoped()) {
      throw new IllegalArgumentException("TeamType %s is not static".formatted(teamType));
    }
    var teams = teamRepository.findByTeamType(teamType);
    if (teams.isEmpty()) {
      throw new IllegalArgumentException("TeamType %s is doesn't have static team associated with it".formatted(teamType));
    }
    if (teams.size() > 1) {
      throw new IllegalArgumentException("TeamType %s has more than one static team associated with it".formatted(teamType));
    }
    return teams.getFirst();
  }

  private void assertRolesValidForTeamType(Set<Role> roles, TeamType teamType) {
    roles.forEach(role -> {
      if (!teamType.getAllowedRoles().contains(role)) {
        throw new IllegalArgumentException("Role %s is not valid for TeamType %s".formatted(role, teamType));
      }
    });
  }

  public Set<TeamRole> getTeamRolesForUser(long wuaId) {
    return new HashSet<>(getUserTeamRoles(wuaId));
  }

  public boolean userIsInRegulatorTeam(long wuaId) {
    return getUserTeamRoles(wuaId).stream()
        .map(TeamRole::getTeam)
        .anyMatch(team -> team.getTeamType().isRegulator());
  }

  public List<TeamRole> getAllTeamRolesWithRoles(Collection<Role> roles) {
    return teamRoleRepository.findAllByRoleIn(roles);
  }

  private List<TeamRole> getUserTeamRoles(long wuaId) {
    return teamRoleRequestCache.getTeamRolesForUser(wuaId);
  }
}

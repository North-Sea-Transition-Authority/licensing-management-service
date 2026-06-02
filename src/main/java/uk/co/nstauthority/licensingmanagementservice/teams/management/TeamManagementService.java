package uk.co.nstauthority.licensingmanagementservice.teams.management;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.energyportal.starter.accounts.EnergyPortalServiceAccessService;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalServiceProviderUserRolesService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
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
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.UserCancelledEvent;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@Service
public class TeamManagementService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TeamManagementService.class);

  static final String PORTAL_USER_LOOKUP_PURPOSE = "Fetch user in team";
  static final String PORTAL_USERS_LOOKUP_PURPOSE = "Fetch users in team";
  static final String PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE = "Validate user account";
  static final String TEAM_TYPE_UNEXPECTED_STATIC_ERROR = "TeamType %s is static, expected scoped";

  private final TeamRepository teamRepository;
  private final TeamRoleRepository teamRoleRepository;
  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;
  private final EnergyPortalServiceAccessService energyPortalServiceAccessService;
  private final EnergyPortalServiceProviderUserRolesService energyPortalServiceProviderUserRolesService;
  private final ApplicationAccessService applicationAccessService;
  private final UserDetailService userDetailService;

  TeamManagementService(
      TeamRepository teamRepository,
      TeamRoleRepository teamRoleRepository,
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService,
      EnergyPortalServiceAccessService energyPortalServiceAccessService,
      EnergyPortalServiceProviderUserRolesService energyPortalServiceProviderUserRolesService,
      ApplicationAccessService applicationAccessService,
      UserDetailService userDetailService
  ) {
    this.teamRepository = teamRepository;
    this.teamRoleRepository = teamRoleRepository;
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.energyPortalServiceAccessService = energyPortalServiceAccessService;
    this.energyPortalServiceProviderUserRolesService = energyPortalServiceProviderUserRolesService;
    this.applicationAccessService = applicationAccessService;
    this.userDetailService = userDetailService;
  }

  public Team createScopedTeam(String name, TeamType teamType, TeamScopeReference scopeRef) {
    if (!teamType.isScoped()) {
      throw new TeamManagementException("Team of type %s is not scoped".formatted(teamType));
    }

    if (doesScopedTeamWithReferenceExist(teamType, scopeRef)) {
      throw new TeamManagementException("Team of type %s scope type %s and scope id %s already exists"
          .formatted(teamType, scopeRef.getId(), scopeRef.getType()));
    }

    var team = new Team();
    team.setName(name);
    team.setTeamType(teamType);
    team.setScopeType(scopeRef.getType());
    team.setScopeId(scopeRef.getId());
    return teamRepository.save(team);
  }

  public void updateTeamName(Team team, String newName) {
    team.setName(newName);
    teamRepository.save(team);
  }

  public Set<TeamType> getTeamTypesUserIsMemberOf(long wuaId) {
    return teamRoleRepository.findAllByWuaId(wuaId)
        .stream()
        .map(teamRole -> teamRole.getTeam().getTeamType())
        .collect(Collectors.toSet());
  }

  public Optional<Team> getStaticTeamOfTypeUserCanManage(TeamType teamType, Long wuaId) {
    if (teamType.isScoped()) {
      throw new TeamManagementException("TeamType %s is scoped, expected static".formatted(teamType));
    }
    return getTeamsOfTypeUserCanManage(teamType, wuaId).stream()
        .findFirst();
  }

  Optional<Team> getStaticTeamOfTypeUserIsMemberOf(TeamType teamType, Long wuaId) {
    if (teamType.isScoped()) {
      throw new TeamManagementException("TeamType %s is scoped, expected static".formatted(teamType));
    }
    return getTeamsOfTypeUserIsMemberOf(teamType, wuaId)
        .stream()
        .findFirst();
  }

  public List<Team> getScopedTeamsOfTypeUserCanManage(TeamType teamType, Long wuaId) {
    if (!teamType.isScoped()) {
      throw new TeamManagementException(TEAM_TYPE_UNEXPECTED_STATIC_ERROR.formatted(teamType));
    }
    var teams = new ArrayList<>(getTeamsOfTypeUserCanManage(teamType, wuaId));

    if (teamType.equals(TeamType.ORGANISATION) && userCanManageAnyOrganisationTeam(wuaId)) {
      // If we want org teams, and the user is a regulator who can manage any org team, include all the org teams.
      teams.addAll(getAllScopedTeamsOfType(TeamType.ORGANISATION));
    } else if (teamType.isApplicationScoped()
               && applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(userDetailService.getUserDetail())) {
      teams.addAll(getAllScopedTeamsOfType(teamType));
    }

    return teams.stream()
        .distinct() // Remove possible dupes from adding all scoped teams the user may already be a team manager of
        .toList();
  }

  public Set<Team> getScopedTeamsOfTypeUserIsMemberOf(TeamType teamType, Long wuaId) {

    if (!teamType.isScoped()) {
      throw new TeamManagementException(TEAM_TYPE_UNEXPECTED_STATIC_ERROR.formatted(teamType));
    }

    var teams = new HashSet<>(getTeamsOfTypeUserIsMemberOf(teamType, wuaId));

    if (teamType.equals(TeamType.ORGANISATION) && userCanManageAnyOrganisationTeam(wuaId)) {
      // If we want org teams, and the user is a regulator who can manage any org team, include all the org teams.
      teams.addAll(getAllScopedTeamsOfType(TeamType.ORGANISATION));
    }

    return new HashSet<>(teams);
  }

  public Optional<EnergyPortalUserJson> getEnergyPortalUser(String emailAddress) {
    var energyPortalUserDtos = energyPortalUserService.findUsersByEmail(
        emailAddress,
        "Find user to add to team"
    );

    if (energyPortalUserDtos.size() > 1) {
      throw new TeamManagementException(
          "More than one UK Energy Portal user exists with the email address %s".formatted(emailAddress)
      );
    }
    return energyPortalUserDtos
        .stream()
        .findFirst();
  }

  public Team getTeam(UUID teamId) throws ResponseStatusException {
    return teamRepository.findById(teamId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team with id %s not found".formatted(teamId)));
  }

  public TeamMemberView getTeamMemberView(Team team, Long wuaId) {
    var teamRoles = teamRoleRepository.findByWuaIdAndTeam(wuaId, team).stream()
        .map(TeamRole::getRole)
        .toList();

    var user = energyPortalUserService.findByWuaId(WebUserAccountId.from(wuaId), PORTAL_USER_LOOKUP_PURPOSE)
        .orElseThrow(() -> new TeamManagementException("WuaId %s not found via EPA".formatted(wuaId)));

    return TeamMemberView.fromEpaUser(user, team.getId(), teamRoles, team);
  }

  public List<TeamMemberView> getTeamMemberViewsForTeam(Team team) {
    var teamRoles = teamRoleRepository.findByTeam(team);

    var memberWuaIds = teamRoles.stream()
        .map(TeamRole::getWuaId)
        .distinct()
        .toList();

    var webUserAccountIds = memberWuaIds.stream()
        .map(WebUserAccountId::from)
        .toList();

    var epaUsers = energyPortalUserService.findByWuaIds(webUserAccountIds, PORTAL_USERS_LOOKUP_PURPOSE);

    return memberWuaIds.stream()
        .map(wuaId -> {
          var epaUser = epaUsers.stream()
              .filter(u -> u.webUserAccountId().equals(wuaId))
              .findFirst()
              .orElseThrow(() -> new TeamManagementException("WuaId %s not found in EPA user set".formatted(wuaId)));

          List<Role> userRoles = teamRoles.stream()
              .filter(teamRole -> teamRole.getWuaId().equals(wuaId))
              .map(TeamRole::getRole)
              .toList();

          List<Role> orderedUserRoles = team.getTeamType().getAllowedRoles()
              .stream()
              .filter(userRoles::contains)
              .toList();

          return TeamMemberView.fromEpaUser(epaUser, team.getId(), orderedUserRoles, team);
        })
        .sorted(Comparator.comparing(TeamMemberView::forename, String::compareToIgnoreCase)
            .thenComparing(TeamMemberView::surname, String::compareToIgnoreCase))
        .toList();
  }

  @Transactional
  public void setUserTeamRoles(Long wuaId, Team team, List<Role> roles, ServiceUserDetail instigatingUser) {
    if (!new HashSet<>(team.getTeamType().getAllowedRoles()).containsAll(roles)) {
      throw new TeamManagementException("Roles %s are not valid for team type %s".formatted(roles, team.getTeamType()));
    }

    // Check the user is valid
    var userOptional = energyPortalUserService
        .findByWuaId(WebUserAccountId.from(wuaId), PORTAL_VALIDATE_USERS_LOOKUP_PURPOSE);

    if (userOptional.isEmpty()) {
      throw new TeamManagementException("User account with wuaId %s does not exist".formatted(wuaId));
    }
    var user = userOptional.get();
    if (user.sharedAccount()) {
      throw new TeamManagementException(
          "User account with wuaId %s is a shared account so can't be added to teams".formatted(wuaId));
    }
    if (!user.canLogin()) {
      throw new TeamManagementException(
          "User account with wuaId %s is not active so can't be added to teams".formatted(wuaId));
    }

    var isNewUser = teamRoleRepository.findAllByWuaId(user.webUserAccountId()).isEmpty();

    teamRoleRepository.deleteByWuaIdAndTeam(wuaId, team);

    var newTeamRoles = roles.stream()
        .map(role -> {
          var teamRole = new TeamRole();
          teamRole.setTeam(team);
          teamRole.setRole(role);
          teamRole.setWuaId(wuaId);
          return teamRole;
        }).toList();
    teamRoleRepository.saveAll(newTeamRoles);

    if (!team.getTeamType().isApplicationScoped() && !doesTeamHaveTeamManager(team)) {
      throw new TeamManagementException("At least 1 team manager must exist in team %s".formatted(team.getId()));
    }

    energyPortalServiceProviderUserRolesService.publishUsersRolesForTeam(
        wuaId,
        team.getId().toString(),
        team.getTeamType().name(),
        roles.stream().map(Role::name).collect(Collectors.toSet())
    );

    if (!isNewUser) {
      return;
    }

    energyPortalServiceAccessService.addUser(user.webUserAccountId());
  }

  @Transactional
  public void removeUserFromTeam(Long wuaId, Team team) {
    if (!willManageTeamRoleBePresentAfterMemberRemoval(team, wuaId)) {
      throw new TeamManagementException(
          "Can't remove last team manager user %s from team %s".formatted(wuaId, team.getId()));
    }

    handleUserRemovalFromTeam(wuaId, team);
  }

  @Transactional
  @EventListener(UserCancelledEvent.class)
  void onUserCancelledEvent(UserCancelledEvent event) {
    var wuaId = event.wuaId();
    for (var team : getTeamsUserIsMemberOf(wuaId)) {
      handleUserRemovalFromTeam(wuaId, team);
      LOGGER.info("Removed user {} from team {}", wuaId, team.getId());
    }
  }

  private void handleUserRemovalFromTeam(Long wuaId, Team team) {
    teamRoleRepository.deleteByWuaIdAndTeam(wuaId, team);
    energyPortalServiceProviderUserRolesService.publishRemoveUserFromTeam(wuaId, team.getId().toString());

    if (teamRoleRepository.findAllByWuaId(wuaId).isEmpty()) {
      energyPortalServiceAccessService.removeUser(wuaId);
    }
  }

  public boolean willManageTeamRoleBePresentAfterMemberRoleUpdate(Team team, Long wuaId, List<Role> membersNewRoles) {
    if (membersNewRoles.contains(Role.MANAGE_TEAM)) {
      return true;
    }
    return willManageTeamRoleBePresentAfterMemberRemoval(team, wuaId);
  }

  public boolean willManageTeamRoleBePresentAfterMemberRemoval(Team team, Long wuaId) {
    if (!team.getTeamType().isApplicationScoped()) {
      return teamRoleRepository.findByTeam(team).stream()
          .filter(teamRole -> !teamRole.getWuaId().equals(wuaId))
          .anyMatch(teamRole -> teamRole.getRole().equals(Role.MANAGE_TEAM));
    } else  {
      return true;
    }
  }

  public boolean doesScopedTeamWithReferenceExist(TeamType teamType, TeamScopeReference scopeRef) {
    return getScopedTeam(teamType, scopeRef).isPresent();
  }

  public Optional<Team> getScopedTeam(TeamType teamType, TeamScopeReference scopeRef) {
    return teamRepository.findByTeamTypeAndScopeTypeAndScopeId(teamType, scopeRef.getType(), scopeRef.getId());
  }

  public List<Team> getScopedTeams(TeamType teamType, String scopeType, Collection<String> scopeReferences) {
    return teamRepository.findByTeamTypeAndScopeTypeAndScopeIdIn(teamType, scopeType, scopeReferences);
  }

  public boolean canManageTeam(Team team, long wuaId) {
    if (team.getTeamType().isScoped()) {
      return getScopedTeamsOfTypeUserCanManage(team.getTeamType(), wuaId)
          .stream()
          .anyMatch(scopedTeam -> scopedTeam.getId().equals(team.getId()));
    } else {
      return getStaticTeamOfTypeUserCanManage(team.getTeamType(), wuaId).isPresent();
    }
  }

  public boolean isMemberOfTeam(Team team, long wuaId) {
    return teamRoleRepository.existsByTeamAndWuaId(team, wuaId);
  }

  public boolean userCanManageAnyOrganisationTeam(long wuaId) {
    return teamQueryService.userHasStaticRole(wuaId, TeamType.LICENCE_MANAGEMENT, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM);
  }

  public Map<String, String> getReassignUserOptions(Role role, Long currentAssigneeWuaId, TeamType teamType) {
    var wuaIds = teamRoleRepository.findAllByRole(role)
        .stream()
        .filter(teamRole -> teamRole.getTeam().getTeamType().equals(teamType))
        .map(TeamRole::getWuaId)
        .filter(id -> !Objects.equals(id, currentAssigneeWuaId))
        .map(WebUserAccountId::from)
        .toList();

    return energyPortalUserService.findByWuaIds(wuaIds, PORTAL_USERS_LOOKUP_PURPOSE)
        .stream()
        .collect(Collectors.toMap(
            user -> user.webUserAccountId().toString(),
            user -> "%s (%s)".formatted(user.displayName(), user.emailAddress()))
        );
  }

  public List<TeamMemberView> getActiveTeamMembersViewsForTeamAndRole(Team team, Role role) {
    return teamRoleRepository.findAllByTeamAndRole(team, role)
        .stream()
        .map(teamMember -> energyPortalUserService.getByWuaId(
            WebUserAccountId.from(teamMember.getWuaId()),
            PORTAL_USER_LOOKUP_PURPOSE)
        )
        .filter(EnergyPortalUserJson::canLogin)
        .map(user -> TeamMemberView.fromEpaUser(user, team.getId(), List.of(role), team))
        .toList();
  }

  public List<Team> getAllScopedTeamsOfType(TeamType teamType) {
    if (!teamType.isScoped()) {
      throw new TeamManagementException(TEAM_TYPE_UNEXPECTED_STATIC_ERROR.formatted(teamType));
    }
    return teamRepository.findByTeamType(teamType);
  }

  private boolean doesTeamHaveTeamManager(Team team) {
    return teamRoleRepository.findByTeam(team).stream()
        .anyMatch(teamRole -> teamRole.getRole().equals(Role.MANAGE_TEAM));
  }

  private List<Team> getTeamsUserCanManage(Long wuaId) {
    var userTeamRoles = teamRoleRepository.findByWuaIdAndRole(wuaId, Role.MANAGE_TEAM);
    return userTeamRoles.stream()
        .map(TeamRole::getTeam)
        .toList();
  }

  private Set<Team> getTeamsUserIsMemberOf(Long wuaId) {
    var userTeamRoles = teamRoleRepository.findAllByWuaId(wuaId);
    return userTeamRoles.stream()
        .map(TeamRole::getTeam)
        .collect(Collectors.toSet());
  }

  private List<Team> getTeamsOfTypeUserCanManage(TeamType teamType, Long wuaId) {
    return getTeamsUserCanManage(wuaId).stream()
        .filter(team -> team.getTeamType().equals(teamType))
        .toList();
  }

  private Set<Team> getTeamsOfTypeUserIsMemberOf(TeamType teamType, Long wuaId) {
    return getTeamsUserIsMemberOf(wuaId).stream()
        .filter(team -> team.getTeamType().equals(teamType))
        .collect(Collectors.toSet());
  }
}
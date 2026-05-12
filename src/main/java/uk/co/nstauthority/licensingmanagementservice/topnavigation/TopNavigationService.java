package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
public class TopNavigationService {

  private final UserDetailService userDetailService;
  private final TeamQueryService teamQueryService;

  public TopNavigationService(
      UserDetailService userDetailService,
      TeamQueryService teamQueryService
  ) {
    this.userDetailService = userDetailService;
    this.teamQueryService = teamQueryService;
  }

  public List<TopNavigationItem> getTopNavigationItems() {
    if (!userDetailService.isUserLoggedIn()) {
      return Collections.emptyList();
    }

    var user = userDetailService.getUserDetail();
    var usersRoles = teamQueryService.getTeamRolesForUser(user.wuaId());

    return EnumSet.allOf(TopNavigationItem.class)
        .stream()
        .filter(item ->
            // For nav items that can appear for any team
            item.getRequiredTeamType() == null
            // For nav items that can only appear for specific team and roles
            || userHasRoleInTeam(item, usersRoles)
        )
        .sorted(Comparator.comparing(TopNavigationItem::getDisplayOrder))
        .toList();
  }

  private boolean userHasRoleInTeam(TopNavigationItem item, Set<TeamRole> usersRoles) {
    return usersRoles.stream()
        .anyMatch(teamRole ->
            teamRole.getTeam().getTeamType() == item.getRequiredTeamType()
                && item.getRequiredRoles().contains(teamRole.getRole())
        );
  }
}
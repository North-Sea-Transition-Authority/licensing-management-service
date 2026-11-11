package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class TopNavigationService {

  private static final Set<TopNavigationItem> EXTERNAL_CONTRIBUTOR_ITEMS = EnumSet.of(
      TopNavigationItem.WORK_AREA,
      TopNavigationItem.LICENCES,
      TopNavigationItem.EVENT_TRACKER
  );

  private final UserDetailService userDetailService;
  private final TeamQueryService teamQueryService;
  private final FeatureFlagService featureFlagService;

  public TopNavigationService(
      UserDetailService userDetailService,
      TeamQueryService teamQueryService,
      FeatureFlagService featureFlagService
  ) {
    this.userDetailService = userDetailService;
    this.teamQueryService = teamQueryService;
    this.featureFlagService = featureFlagService;
  }

  public List<TopNavigationItem> getTopNavigationItems() {
    if (!userDetailService.isUserLoggedIn()) {
      return Collections.emptyList();
    }

    var user = userDetailService.getUserDetail();
    var usersRoles = teamQueryService.getTeamRolesForUser(user.wuaId());
    var userIsInRegulatorTeam = usersRoles.stream()
        .anyMatch(teamRole -> teamRole.getTeam().getTeamType().isRegulator());
    var userIsOnlyExternalContributor = !usersRoles.isEmpty() && usersRoles.stream()
        .allMatch(teamRole -> teamRole.getTeam().getTeamType() == TeamType.EXTERNAL_CONTRIBUTORS);

    return EnumSet.allOf(TopNavigationItem.class)
        .stream()
        // Hide items whose release phase is not yet switched on
        .filter(item -> featureFlagService.isEnabled(item.getReleasePhase()))
        // Hide regulator-only items from users outside a regulator team
        .filter(item -> !item.isRegulatorOnly() || userIsInRegulatorTeam)
        // Restrict users whose only roles are external contributor roles to a limited set of items
        .filter(item -> !userIsOnlyExternalContributor || EXTERNAL_CONTRIBUTOR_ITEMS.contains(item))
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
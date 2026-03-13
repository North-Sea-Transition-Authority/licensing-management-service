package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
public class LicenceContinuationActionService {
  private static final Map<LicenceContinuationActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(LicenceContinuationActionItem.class);
  private static final Map<LicenceContinuationApplicationStatus,
      Set<LicenceContinuationActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(LicenceContinuationApplicationStatus.class);

  private final TeamQueryService teamQueryService;

  public LicenceContinuationActionService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;

    var registeredActions = LicenceContinuationActionBuilder.newBuilder()
        .registerAction(LicenceContinuationActionItem.CONFIRM_CONTINUATION)
          .requiresAnyRoleFrom(ApplicationAccessService.CONTINUATION_REVIEWER_ROLES)
          .requiresAnyStatusFrom(LicenceContinuationApplicationStatus.SUBMITTED)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
  }

  public List<ActionItemView> getAvailableUserActionItems(
      LicenceContinuationApplicationDetail applicationDetail,
      ServiceUserDetail user
  ) {
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    return EnumSet.allOf(LicenceContinuationActionItem.class).stream()
        .filter(STATUS_TO_ACTIONS.getOrDefault(applicationDetail.getStatus(), Set.of())::contains)
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        .map(actionItem -> actionItem.toActionItemView(applicationDetail))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }
}

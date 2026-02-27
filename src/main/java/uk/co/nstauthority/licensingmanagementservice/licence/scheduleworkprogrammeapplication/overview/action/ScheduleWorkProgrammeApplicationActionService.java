package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleWorkProgrammeApplicationActionService {
  private static final Map<ScheduleWorkProgrammeApplicationActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
  private static final Map<ScheduleWorkProgrammeApplicationStatus,
      Set<ScheduleWorkProgrammeApplicationActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(ScheduleWorkProgrammeApplicationStatus.class);

  private final TeamQueryService teamQueryService;

  public ScheduleWorkProgrammeApplicationActionService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;

    var registeredActions = ScheduleWorkProgrammeApplicationActionBuilder.newBuilder()
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD)
          .requiresAnyRoleFrom(StreamUtil.unionSets(
              ApplicationAccessService.STEWARD_ROLES,
              ApplicationAccessService.CASE_MANAGER_ROLES
          ).toArray(Role[]::new))
          .requiresAnyStatusFrom(ScheduleWorkProgrammeApplicationStatus.SUBMITTED)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
  }

  public List<ActionItemView> getAvailableUserActionItems(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ServiceUserDetail user
  ) {
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    return EnumSet.allOf(ScheduleWorkProgrammeApplicationActionItem.class).stream()
        .filter(STATUS_TO_ACTIONS.getOrDefault(applicationDetail.getStatus(), Set.of())::contains)
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        .map(actionItem -> actionItem.toActionItemView(applicationDetail))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }
}

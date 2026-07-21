package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleWorkProgrammeApplicationActionService {
  private static final Map<ScheduleWorkProgrammeApplicationActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
  private static final Map<ApplicationStatus,
      Set<ScheduleWorkProgrammeApplicationActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(ApplicationStatus.class);
  private static final Map<ScheduleWorkProgrammeApplicationActionItem,
      Function<ScheduleWorkProgrammeApplicationDetail, Long>> ACTIONS_TO_USER_GRANT_PREDICATES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
  private static final Map<ScheduleWorkProgrammeApplicationActionItem,
      Predicate<ScheduleWorkProgrammeApplicationDetail>> ACTIONS_TO_PRIMARY_PREDICATES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);

  private final TeamQueryService teamQueryService;

  public ScheduleWorkProgrammeApplicationActionService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;

    var registeredActions = ScheduleWorkProgrammeApplicationActionBuilder.newBuilder()
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .requiresAnyRoleFrom(StreamUtil.unionSets(
              ApplicationAccessService.STEWARD_ROLES,
              ApplicationAccessService.CASE_MANAGER_ROLES
          ).toArray(Role[]::new))
          .isPrimaryButton(false)
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .requiresAnyRoleFrom(ApplicationAccessService.CASE_MANAGER_ROLES.toArray(Role[]::new))
            .orGrantedToUser(detail -> detail.getScheduleWorkProgrammeApplication().getStewardWuaId())
          .isPrimaryButton(true)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
    ACTIONS_TO_USER_GRANT_PREDICATES.putAll(registeredActions.userGrantPredicateMap);
    ACTIONS_TO_PRIMARY_PREDICATES.putAll(registeredActions.primaryActionPredicateMap);
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
        .filter(action -> {
          var grantedUserId = ACTIONS_TO_USER_GRANT_PREDICATES
              .getOrDefault(action, detail -> null)
              .apply(applicationDetail);
          return CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles)
              || Objects.equals(grantedUserId, user.wuaId());
        })
        .map(actionItem -> actionItem.toActionItemView(applicationDetail, isPrimary(applicationDetail, actionItem)))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }

  private static boolean isPrimary(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ScheduleWorkProgrammeApplicationActionItem actionItem
  ) {
    return ACTIONS_TO_PRIMARY_PREDICATES.getOrDefault(actionItem, detail -> false)
        .test(applicationDetail);
  }
}

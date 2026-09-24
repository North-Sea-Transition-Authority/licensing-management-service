package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.CaseManagerRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.application.StewardRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleWorkProgrammeApplicationActionService {
  private static final Map<ScheduleWorkProgrammeApplicationActionItem,
      Function<ScheduleWorkProgrammeApplicationDetail, Set<Role>>> ACTIONS_TO_ROLES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
  private static final Map<ApplicationStatus,
      Set<ScheduleWorkProgrammeApplicationActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(ApplicationStatus.class);
  private static final Map<ScheduleWorkProgrammeApplicationActionItem,
      Function<ScheduleWorkProgrammeApplicationDetail, Long>> ACTIONS_TO_USER_GRANT_PREDICATES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
  private static final Map<ScheduleWorkProgrammeApplicationActionItem,
      BiPredicate<ScheduleWorkProgrammeApplicationDetail, Set<Role>>> ACTIONS_TO_PRIMARY_PREDICATES
      = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);

  private final TeamQueryService teamQueryService;

  public ScheduleWorkProgrammeApplicationActionService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;

    var registeredActions = ScheduleWorkProgrammeApplicationActionBuilder.newBuilder()
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .requiresAnyRoleFrom(detail -> StreamUtil.toSet(stewardRole(detail), caseManagerRole(detail)))
          .isPrimaryButton((detail, userRoles) ->
              hasRole(caseManagerRole(detail), userRoles)
              && !stewardIsAllocated(detail)
          )
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .requiresAnyRoleFrom(detail -> StreamUtil.toSet(caseManagerRole(detail)))
            .orGrantedToUser(detail -> detail.getScheduleWorkProgrammeApplication().getStewardWuaId())
          .isPrimaryButton((detail, userRoles) ->
              hasRole(stewardRole(detail), userRoles)
              && stewardIsAllocated(detail)
          )
        .registerAction(ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION)
          .requiresAnyStatusFrom(ApplicationStatus.DSP_UPLOADED)
          .requiresAnyRoleFrom(detail -> StreamUtil.toSet(caseManagerRole(detail)))
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
          var requiredRoles = ACTIONS_TO_ROLES
              .getOrDefault(action, detail -> Set.<Role>of())
              .apply(applicationDetail);
          return CollectionUtils.containsAny(requiredRoles, userRoles)
              || Objects.equals(grantedUserId, user.wuaId());
        })
        .map(actionItem -> actionItem.toActionItemView(applicationDetail, isPrimary(applicationDetail, actionItem, userRoles)))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }

  private static boolean isPrimary(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ScheduleWorkProgrammeApplicationActionItem actionItem,
      Set<Role> userRoles
  ) {
    return ACTIONS_TO_PRIMARY_PREDICATES.getOrDefault(actionItem, (detail, roles) -> false)
        .test(applicationDetail, userRoles);
  }

  private static boolean hasRole(Optional<Role> requiredRole, Set<Role> userRoles) {
    return requiredRole.map(userRoles::contains).orElse(false);
  }

  private static Optional<Role> caseManagerRole(ScheduleWorkProgrammeApplicationDetail detail) {
    return CaseManagerRoles.getRequiredRoleForLicenceType(detail.getLicence().getType());
  }

  private static Optional<Role> stewardRole(ScheduleWorkProgrammeApplicationDetail detail) {
    return StewardRoles.getRequiredRoleForLicenceType(detail.getLicence().getType());
  }

  private static boolean stewardIsAllocated(ScheduleWorkProgrammeApplicationDetail detail) {
    return detail.getScheduleWorkProgrammeApplication().getStewardWuaId() != null;
  }
}

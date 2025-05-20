package uk.co.nstauthority.template.xyzapplication.processing.action;

import static java.util.stream.Collectors.toSet;
import static uk.co.nstauthority.template.teams.Role.EDIT_APPLICATION;
import static uk.co.nstauthority.template.teams.Role.VIEW_APPLICATION;
import static uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus.DRAFT;
import static uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus.SUBMITTED;
import static uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionItem.PROGRESS_APPLICATION;
import static uk.co.nstauthority.template.xyzapplication.processing.action.CaseProcessingActionItem.VERIFY_APPLICATION;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamQueryService;
import uk.co.nstauthority.template.teams.TeamRole;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;

@Service
public class CaseProcessingActionService {

  private static final Map<XyzApplicationStatus, Set<CaseProcessingActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(XyzApplicationStatus.class);
  private static final Map<CaseProcessingActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(CaseProcessingActionItem.class);

  private final TeamQueryService teamQueryService;

  public CaseProcessingActionService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;

    var registeredActions = RegisterAndSetPermissionsForAnActionBuilder.newBuilder()
        .registerAction(PROGRESS_APPLICATION)
        .requiresAnyRoleFrom(EDIT_APPLICATION)
        .requiresAnyStatusFrom(
            DRAFT
        )
        .registerAction(VERIFY_APPLICATION)
        .requiresAnyRoleFrom(VIEW_APPLICATION)
        .requiresAnyStatusFrom(
            SUBMITTED
        )
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
  }


  public Set<CaseProcessingActionItem> getAvailableUserActionItems(
      XyzApplication xyzApplication,
      ServiceUserDetail user
  ) {
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(toSet());

    return EnumSet.allOf(CaseProcessingActionItem.class).stream()
        // remove the actions which aren't applicable to the current application status
        .filter(STATUS_TO_ACTIONS.get(xyzApplication.getStatus())::contains)
        // remove actions that users can't see given their roles
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        .collect(toSet());
  }
}

package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class MemberRolesFormValidator {
  private final TeamManagementService teamManagementService;
  private final TeamQueryService teamQueryService;

  public MemberRolesFormValidator(TeamManagementService teamManagementService, TeamQueryService teamQueryService) {
    this.teamManagementService = teamManagementService;
    this.teamQueryService = teamQueryService;
  }

  public void validate(MemberRolesForm form, Long wuaId, Team team, Errors errors) {
    if (form.getRoles() == null || form.getRoles().isEmpty()) {
      errors.rejectValue("roles", "roles.required", "Select at least one role");
      return;
    }

    var roles = form.getRoles().stream()
        .map(Role::valueOf)
        .toList();

    if (!new HashSet<>(teamQueryService.getAvailableRoles(team.getTeamType())).containsAll(roles)) {
      errors.rejectValue("roles", "roles.notAvailable", "Select roles that are available for this team");
      return;
    }

    if (!team.getTeamType().isApplicationScoped()
        && !teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(team, wuaId, roles)) {
      errors.rejectValue("roles", "roles.noTeamManager",
          "There must always be at least one user who can add, remove and update members of this team.");
    }
  }
}

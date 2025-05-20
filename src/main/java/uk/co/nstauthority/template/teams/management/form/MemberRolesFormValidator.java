package uk.co.nstauthority.template.teams.management.form;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.Team;
import uk.co.nstauthority.template.teams.management.TeamManagementService;

@Service
public class MemberRolesFormValidator {
  private final TeamManagementService teamManagementService;

  public MemberRolesFormValidator(TeamManagementService teamManagementService) {
    this.teamManagementService = teamManagementService;
  }

  public void validate(MemberRolesForm form, Long wuaId, Team team, Errors errors) {
    if (form.getRoles() == null || form.getRoles().isEmpty()) {
      errors.rejectValue("roles", "roles.required", "Select at least one role");
      return;
    }

    var roles = form.getRoles().stream()
        .map(Role::valueOf)
        .toList();

    if (!teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(team, wuaId, roles)) {
      errors.rejectValue("roles", "roles.noTeamManager",
          "There must always be at least one user who can add, remove and update members of this team.");
    }
  }
}

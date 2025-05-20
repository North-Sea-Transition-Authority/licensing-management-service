package uk.co.nstauthority.template.authorisation.rules;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.nstauthority.template.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.template.authorisation.RolesAndTeamType;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamType;

@Controller
@RequestMapping("/role-and-team-type-test")
public class RoleAndTeamTypeEndpoints {

  @GetMapping("/has-role-in-team-type")
  @HasRolesInTeamType(value = {
      @RolesAndTeamType(roles = Role.EDIT_APPLICATION, teamType = TeamType.ORGANISATION)
  })
  public ResponseEntity<String> hasRoleWithTeamType() {
    return ResponseEntity.ok("role in team type test endpoint");
  }

  @GetMapping("/has-multiple-roles-in-team-type")
  @HasRolesInTeamType(value = {
      @RolesAndTeamType(roles = {Role.EDIT_APPLICATION, Role.VIEW_APPLICATION}, teamType = TeamType.ORGANISATION)
  })
  public ResponseEntity<String> hasMultipleRolesInTeamType() {
    return ResponseEntity.ok("multiple role and team type test endpoint");
  }

  @GetMapping("/has-multiple-roles-in-multiple-team-types")
  @HasRolesInTeamType(value = {
      @RolesAndTeamType(roles = {Role.EDIT_APPLICATION, Role.VIEW_APPLICATION}, teamType = TeamType.ORGANISATION),
      @RolesAndTeamType(roles = {Role.MANAGE_TEAM}, teamType = TeamType.REGULATOR)
  })
  public ResponseEntity<String> hasMultipleRolesInMultipleTeamTypes() {
    return ResponseEntity.ok("multiple role in multiple team type test endpoint");
  }

  @GetMapping("/has-no-roles-provided")
  @HasRolesInTeamType(value = {})
  public ResponseEntity<String> noRolesProvided() {
    return ResponseEntity.ok("no roles test endpoint");
  }
}

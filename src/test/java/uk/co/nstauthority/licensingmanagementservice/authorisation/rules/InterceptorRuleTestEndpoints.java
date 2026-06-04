package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;


import static uk.co.nstauthority.licensingmanagementservice.xyzapplication.processing.action.CaseProcessingActionItem.PROGRESS_APPLICATION;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("/interceptor-rule-test")
public class InterceptorRuleTestEndpoints {

  @GetMapping("has-any-role-in-team-type")
  @HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType(TeamType.LICENCE_MANAGEMENT)
  public ResponseEntity<String> hasAnyRoleInTeamType() {
    return ResponseEntity.ok("has any role in team type test endpoint");
  }

  @GetMapping("has-role-in-static-team")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam(@HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MANAGEMENT, roles = {Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE}))
  public ResponseEntity<String> hasRoleInStaticTeam() {
    return ResponseEntity.ok("has role in static team test endpoint");
  }

  @GetMapping("has-role-in-static-team-multiple-teams")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam({
      @HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MANAGEMENT, roles = {Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE}),
      @HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.OFFSHORE_PRODUCTION_LICENSING, roles = {Role.MANAGE_TEAM})
  })
  public ResponseEntity<String> hasRoleInStaticTeam_multipleTeams() {
    return ResponseEntity.ok("has role in static team multiple teams test endpoint");
  }

  @GetMapping("has-role-in-static-team-scoped-team")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam(
      @HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.ORGANISATION, roles = {Role.MANAGE_TEAM, Role.VIEW_ORGANISATION_LICENCES})
  )
  public ResponseEntity<String> hasRoleInStaticTeam_scopedTeam() {
    return ResponseEntity.ok("has role in static team scoped team test endpoint");
  }

  @GetMapping("has-role-in-static-team-no-roles")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam(@HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MANAGEMENT, roles = {}))
  public ResponseEntity<String> hasRoleInStaticTeam_noProvidedRoles() {
    return ResponseEntity.ok("has role in static team no provided roles test endpoint");
  }

  @GetMapping("/test-with-action")
  @ActionEndPointInterceptorRule.ActionEndPoint(PROGRESS_APPLICATION)
  String getAction() {
    return "some data action";
  }

  @GetMapping("/test-with-licence-action")
  @LicenceActionEndPointInterceptorRule.ActionEndPoint({LicenceActionItem.CREATE_LICENCE_SCHEDULE})
  public String getLicenceAction() {
    return "licence action data";
  }

  @GetMapping("/log-work-area-item")
  @LogWorkAreaItemView(itemType = WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION, pathVariable = "itemId")
  public ResponseEntity<String> logWorkAreaItem() {
    return ResponseEntity.ok("log work area item test endpoint");
  }

  @GetMapping("/log-work-area-item-disabled")
  @LogWorkAreaItemView(itemType = WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION, pathVariable = "itemId", disable = true)
  public ResponseEntity<String> logWorkAreaItemDisabled() {
    return ResponseEntity.ok("log work area item disabled test endpoint");
  }
}

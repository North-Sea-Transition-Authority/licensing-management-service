package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;


import static uk.co.nstauthority.licensingmanagementservice.xyzapplication.processing.action.CaseProcessingActionItem.PROGRESS_APPLICATION;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationStatus;

@Controller
@RequestMapping("/interceptor-rule-test")
public class InterceptorRuleTestEndpoints {

  @GetMapping("has-any-role-in-team-type")
  @HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType(TeamType.LICENCE_MAINTENANCE)
  public ResponseEntity<String> hasAnyRoleInTeamType() {
    return ResponseEntity.ok("has any role in team type test endpoint");
  }

  @GetMapping("has-role-in-static-team")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam(@HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MAINTENANCE, roles = {Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE}))
  public ResponseEntity<String> hasRoleInStaticTeam() {
    return ResponseEntity.ok("has role in static team test endpoint");
  }

  @GetMapping("has-role-in-static-team-multiple-teams")
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam({
      @HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MAINTENANCE, roles = {Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE}),
      @HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.PRODUCTION, roles = {Role.MANAGE_TEAM})
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
  @HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam(@HasRoleInStaticTeamInterceptorRule.TeamRoles(teamType = TeamType.LICENCE_MAINTENANCE, roles = {}))
  public ResponseEntity<String> hasRoleInStaticTeam_noProvidedRoles() {
    return ResponseEntity.ok("has role in static team no provided roles test endpoint");
  }

  @GetMapping("/test-with-action")
  @ActionEndPointInterceptorRule.ActionEndPoint(PROGRESS_APPLICATION)
  String getAction() {
    return "some data action";
  }

  @GetMapping("xyzapplication-has-status-one-status/{applicationId}")
  @XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus(XyzApplicationStatus.APPROVED)
  public ResponseEntity<String> xyzApplicationHasStatus_oneStatus(XyzApplication xyzApplication) {
    return ResponseEntity.ok("xyzApplication has status one status test endpoint");
  }

  @GetMapping("xyzapplication-has-status-many-statuses/{applicationId}")
  @XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus({XyzApplicationStatus.APPROVED, XyzApplicationStatus.SUBMITTED})
  public ResponseEntity<String> xyzApplicationHasStatus_manyStatuses(XyzApplication xyzApplication) {
    return ResponseEntity.ok("xyzApplication has status many statuses test endpoint");
  }

  @GetMapping("xyzapplication-has-status-no-status/{applicationId}")
  @XyzApplicationHasStatusInterceptorRule.XyzApplicationHasStatus({})
  public ResponseEntity<String> xyzApplicationHasStatus_noStatus(XyzApplication xyzApplication) {
    return ResponseEntity.ok("xyzApplication has status no statuses test endpoint");
  }
}

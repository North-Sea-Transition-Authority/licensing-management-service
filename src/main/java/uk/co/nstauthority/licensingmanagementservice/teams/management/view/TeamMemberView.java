package uk.co.nstauthority.licensingmanagementservice.teams.management.view;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;

public record TeamMemberView(
    Long wuaId,
    String title,
    String forename,
    String surname,
    String email,
    String telNo,
    UUID teamId,
    List<Role> roles,
    boolean isApplicationScoped
) {
  public String getDisplayName() {
    return Stream.of(title, forename, surname)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.joining(" "));
  }

  public String getEditUrl() {
    if (isApplicationScoped) {
      return ReverseRouter.route(on(TeamManagementController.class)
          .renderUserScheduleExternalContributorsTeamRoles(teamId, wuaId, null));
    }
    return ReverseRouter.route(on(TeamManagementController.class)
        .renderUserTeamRoles(teamId, wuaId, null));
  }

  public String getRemoveUrl() {
    if (isApplicationScoped) {
      return ReverseRouter.route(on(TeamManagementController.class)
          .renderRemoveScheduleExternalContributorsTeamMember(teamId, wuaId));
    }
    return ReverseRouter.route(on(TeamManagementController.class)
        .renderRemoveTeamMember(teamId, wuaId));
  }

  public static TeamMemberView fromEpaUser(EnergyPortalUserJson user, UUID teamId, List<Role> roles, Team team) {
    return new TeamMemberView(
        user.webUserAccountId(),
        user.title(),
        user.forename(),
        user.surname(),
        user.emailAddress(),
        user.telephoneNumber(),
        teamId,
        roles,
        team.getTeamType().isApplicationScoped()
    );
  }
}
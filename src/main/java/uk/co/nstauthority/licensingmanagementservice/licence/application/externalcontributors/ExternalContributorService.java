package uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class ExternalContributorService {

  private final TeamManagementService teamManagementService;

  public ExternalContributorService(TeamManagementService teamManagementService) {
    this.teamManagementService = teamManagementService;
  }

  public Team getExternalContributorsTeam(TeamScopeReference scopeRef) {
    return teamManagementService.getScopedTeam(TeamType.EXTERNAL_CONTRIBUTORS, scopeRef)
        .orElseThrow(() -> new LmsEntityNotFoundException(String.format(
            "No external contributors team found for scope %s : %s",
            scopeRef.getType(),
            scopeRef.getId()
        )));
  }

  public void clearExternalContributors(TeamScopeReference scopeRef) {
    teamManagementService.removeAllUsersFromTeam(getExternalContributorsTeam(scopeRef));
  }

  public boolean isSectionComplete(Boolean addExternalContributors, TeamScopeReference scopeRef) {
    if (addExternalContributors == null) {
      return false;
    }

    if (BooleanUtils.isFalse(addExternalContributors)) {
      return true;
    }

    return teamManagementService.teamHasMembers(getExternalContributorsTeam(scopeRef));
  }
}

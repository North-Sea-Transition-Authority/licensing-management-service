package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@Service
public class IndustryTeamService {

  private final TeamManagementService teamManagementService;

  public IndustryTeamService(TeamManagementService teamManagementService) {
    this.teamManagementService = teamManagementService;
  }

  public List<TeamMemberView> getSubmitterDetails(Integer organisationGroupId) {
    var scopeRef = TeamScopeReference.from(
        String.valueOf(organisationGroupId),
        ScopeType.ORGANISATION_GROUP.name()
    );

    var teamOptional = teamManagementService.getScopedTeam(TeamType.ORGANISATION, scopeRef);

    if (teamOptional.isEmpty()) {
      return List.of();
    }

    return teamManagementService.getActiveTeamMembersViewsForTeamAndRole(
        teamOptional.get(),
        Role.APPLICATION_SUBMITTER
    );
  }
}

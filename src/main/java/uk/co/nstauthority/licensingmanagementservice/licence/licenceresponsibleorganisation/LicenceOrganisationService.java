package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class LicenceOrganisationService {


  private final TeamQueryService teamQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public LicenceOrganisationService(TeamQueryService teamQueryService,
                                    OrganisationGroupQueryService organisationGroupQueryService) {
    this.teamQueryService = teamQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  public List<OrganisationUnitJson> getUsersOrgUnits(ServiceUserDetail user) {
    var usersOrgGroupIds = getUsersOrgGroupIds(user);

    if (usersOrgGroupIds.isEmpty()) {
      return Collections.emptyList();
    }

    return organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(usersOrgGroupIds);
  }

  private List<Integer> getUsersOrgGroupIds(ServiceUserDetail user) {
    return teamQueryService.getTeamRolesForUser(user.wuaId())
        .stream()
        .map(TeamRole::getTeam)
        .filter(team -> team.getTeamType() == TeamType.ORGANISATION)
        .map(Team::getScopeId)
        .map(Integer::valueOf)
        .toList();
  }
}

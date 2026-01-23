package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class LicenceAccessService {

  private final TeamQueryService teamQueryService;

  LicenceAccessService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;
  }

  public boolean userHasAccessToCreateLicence(Long wuaId) {
    var allowedRoles = Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR);
    return teamQueryService.userHasRoleInTeamType(wuaId, TeamType.LICENCE_MANAGEMENT, allowedRoles);
  }
}


package uk.co.nstauthority.licensingmanagementservice.teams;

import static uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService.CONTINUATION_REVIEWER_ROLES;
import static uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService.DECISION_ISSUER_ROLES;

import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

@Service
public class RegulatorRoleService {

  private final TeamQueryService teamQueryService;

  public RegulatorRoleService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;
  }

  public boolean isRegulator(ServiceUserDetail userDetail) {
    return teamQueryService.userIsInRegulatorTeam(userDetail.wuaId());
  }

  public boolean isContinuationReviewer(ServiceUserDetail userDetail) {
    return teamQueryService.userHasAtLeastOneStaticRole(
        userDetail.wuaId(),
        TeamType.OFFSHORE_PRODUCTION_LICENSING,
        CONTINUATION_REVIEWER_ROLES
    );
  }

  public boolean isContinuationIssuer(ServiceUserDetail userDetail) {
    return teamQueryService.userHasAtLeastOneStaticRole(
        userDetail.wuaId(),
        TeamType.REGULATIONS_LICENSING,
        Set.of(Role.CONTINUATION_ISSUER)
    );
  }

  public boolean isDecisionIssuer(ServiceUserDetail userDetail) {
    return teamQueryService.getTeamRolesForUser(userDetail.wuaId()).stream()
        .anyMatch(teamRole -> DECISION_ISSUER_ROLES.contains(teamRole.getRole()));
  }
}
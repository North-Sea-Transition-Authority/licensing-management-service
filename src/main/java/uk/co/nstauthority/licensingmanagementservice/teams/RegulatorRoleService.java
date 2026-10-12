package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.CaseManagerRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.application.StewardRoles;

@Service
public class RegulatorRoleService {

  public static final Set<Role> CONTINUATION_REVIEWER_ROLES = Set.of(
      Role.CONTINUATION_REVIEWER_OFFSHORE,
      Role.CONTINUATION_REVIEWER_ONSHORE
  );
  public static final Set<Role> DECISION_ISSUER_ROLES = Set.of(
      Role.DECISION_ISSUER_OFFSHORE,
      Role.DECISION_ISSUER_CARBON_STORAGE,
      Role.DECISION_ISSUER_ONSHORE
  );

  private final TeamQueryService teamQueryService;

  public RegulatorRoleService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;
  }

  public boolean isRegulator(ServiceUserDetail userDetail) {
    return teamQueryService.userIsInRegulatorTeam(userDetail.wuaId());
  }

  public boolean isContinuationReviewer(ServiceUserDetail userDetail) {
    return hasAnyRole(userDetail, CONTINUATION_REVIEWER_ROLES);
  }

  public boolean isContinuationIssuer(ServiceUserDetail userDetail) {
    return teamQueryService.userHasAtLeastOneStaticRole(
        userDetail.wuaId(),
        TeamType.REGULATIONS_LICENSING,
        Set.of(Role.CONTINUATION_ISSUER)
    );
  }

  public boolean isLicenceContactsManager(ServiceUserDetail userDetail) {
    return teamQueryService.userHasAtLeastOneStaticRole(
        userDetail.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.LICENCE_CONTACTS_MANAGER)
    );
  }

  public boolean isDecisionIssuer(ServiceUserDetail userDetail) {
    return hasAnyRole(userDetail, DECISION_ISSUER_ROLES);
  }

  public Set<LicenceType> getStewardOrCaseManagerLicenceTypes(ServiceUserDetail userDetail) {
    var userRoles = teamQueryService.getTeamRolesForUser(userDetail.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    return Arrays.stream(LicenceType.values())
        .filter(licenceType ->
            Stream.of(
                    StewardRoles.getRequiredRoleForLicenceType(licenceType),
                    CaseManagerRoles.getRequiredRoleForLicenceType(licenceType)
                )
                .flatMap(Optional::stream)
                .anyMatch(userRoles::contains)
        )
        .collect(Collectors.toSet());
  }

  private boolean hasAnyRole(ServiceUserDetail userDetail, Set<Role> roles) {
    return teamQueryService.getTeamRolesForUser(userDetail.wuaId()).stream()
        .anyMatch(teamRole -> roles.contains(teamRole.getRole()));
  }
}
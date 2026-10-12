package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class ApplicationAccessService {

  private final TeamQueryService teamQueryService;

  private static final Set<Role> ALLOWED_DRAFTER_ROLES = Set.of(
      Role.EXTERNAL_APPLICATION_EDITOR,
      Role.APPLICATION_EDITOR,
      Role.APPLICATION_SUBMITTER
  );

  public ApplicationAccessService(TeamQueryService teamQueryService) {
    this.teamQueryService = teamQueryService;
  }

  public boolean userHasAccessToApplication(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> orgUnitIdToGroupId,
      Long wuaId
  ) {
    var teamRoles = teamQueryService.getTeamRolesForUser(wuaId);

    var isRegulator = teamRoles.stream()
        .anyMatch(teamRole -> teamRole.getTeam().getTeamType().isRegulator());

    if (isRegulator && applicationDetail.hasBeenSubmitted()) {
      return true;
    }

    return userHasIndustryAccessToApplication(applicationDetail, orgUnitIdToGroupId, teamRoles);
  }

  public boolean userHasIndustryAccessToApplication(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> orgUnitIdToGroupId,
      Set<TeamRole> teamRoles
  ) {
    var licenceApplication = applicationDetail.getLicenceApplication();
    var licenseeGroupIds = getApplicationLicenseeGroupIds(applicationDetail, orgUnitIdToGroupId);

    return teamRoles.stream()
        .filter(teamRole -> ALLOWED_DRAFTER_ROLES.contains(teamRole.getRole()))
        .map(TeamRole::getTeam)
        .anyMatch(team ->
            isExternalContributor(team, licenceApplication)
            || isLicenseeOrgGroupMember(team, licenseeGroupIds)
        );
  }

  private Set<String> getApplicationLicenseeGroupIds(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> orgUnitIdToGroupId
  ) {
    if (applicationDetail.hasBeenSubmitted()) {
      return orgUnitIdToGroupId.values().stream()
          .map(String::valueOf)
          .collect(Collectors.toSet());
    }

    return Optional.ofNullable(applicationDetail.getResponsibleOrganisationUnitId())
        .map(orgUnitIdToGroupId::get)
        .map(groupId -> Set.of(String.valueOf(groupId)))
        .orElse(Set.of());
  }

  private boolean isLicenseeOrgGroupMember(Team team, Set<String> organisationGroupIds) {
    return team.getTeamType() == TeamType.ORGANISATION
        && ScopeType.ORGANISATION_GROUP.name().equals(team.getScopeType())
        && organisationGroupIds.contains(team.getScopeId());
  }

  private boolean isExternalContributor(Team team, LicenceApplication licenceApplication) {
    return team.getTeamType() == TeamType.EXTERNAL_CONTRIBUTORS
        && licenceApplication.getId().toString().equals(team.getScopeId())
        && team.getScopeType().equals(licenceApplication.getApplicationType().name());
  }

}
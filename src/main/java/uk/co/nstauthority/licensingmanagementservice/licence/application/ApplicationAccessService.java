package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class ApplicationAccessService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final TeamQueryService teamQueryService;
  private final Set<Role> editorSubmitterRoles = Set.of(Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER);

  public ApplicationAccessService(
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService,
      TeamQueryService teamQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.teamQueryService = teamQueryService;
  }

  public boolean userHasAccessToApplication(
      String applicationId,
      ApplicationType applicationType,
      Integer organisationUnitId,
      Long wuaId
  ) {
    var allowedRoles = Set.of(
        Role.EXTERNAL_APPLICATION_EDITOR,
        Role.APPLICATION_EDITOR,
        Role.APPLICATION_SUBMITTER
    );

    var organisationGroupIds = organisationUnitQueryService.findOrganisationGroupIdByUnitId(organisationUnitId)
        .map(String::valueOf)
        .map(Set::of)
        .orElse(Set.of());

    return teamQueryService.getTeamRolesForUser(wuaId).stream()
                           .filter(teamRole -> allowedRoles.contains(teamRole.getRole()))
                           .anyMatch(teamRole -> isMatchingExternalContributorOrOrganisationTeam(
                               teamRole.getTeam(),
                               applicationId,
                               applicationType,
                               organisationGroupIds)
                           );
  }

  private boolean isMatchingExternalContributorOrOrganisationTeam(
      Team team,
      String applicationId,
      ApplicationType applicationType,
      Set<String> organisationGroupIds
  ) {
    boolean isExternalContributor = team.getTeamType() == TeamType.EXTERNAL_CONTRIBUTORS
                                    && applicationId.equals(team.getScopeId())
                                    && team.getScopeType().equals(applicationType.name());

    boolean isOrganisationGroup = team.getTeamType() == TeamType.ORGANISATION
                                  && organisationGroupIds.contains(team.getScopeId())
                                  && team.getScopeType().equals(ScopeType.ORGANISATION_GROUP.name());

    return isExternalContributor || isOrganisationGroup;
  }

  public boolean userHasAccessToStartApplication(
      Long wuaId
  ) {
    return teamQueryService.userHasRoleInTeamType(wuaId, TeamType.ORGANISATION, editorSubmitterRoles);
  }

  public boolean userHasEditorOrSubmitterRoleInOrganisationGroup(ServiceUserDetail userDetail) {
    return !getOrganisationGroupIds(userDetail).isEmpty();
  }

  private Set<Integer> getOrganisationGroupIds(ServiceUserDetail userDetail) {
    return teamQueryService.getTeamRolesForUser(userDetail.wuaId())
        .stream()
        .filter(teamRole ->
                    teamRole.getTeam().getTeamType() == TeamType.ORGANISATION
                    && ScopeType.ORGANISATION_GROUP.name().equals(teamRole.getTeam().getScopeType())
                    && editorSubmitterRoles.contains(teamRole.getRole())
        )
        .map(teamRole -> Integer.valueOf(teamRole.getTeam().getScopeId()))
        .collect(Collectors.toSet());
  }

  public boolean userIsSubmitterForOrganisationUnit(Integer organisationUnitId, Long wuaId) {
    return organisationUnitQueryService.findOrganisationGroupIdByUnitId(organisationUnitId)
        .map(groupId -> teamQueryService.userHasScopedRole(
            wuaId,
            TeamType.ORGANISATION,
            TeamScopeReference.from(String.valueOf(groupId), ScopeType.ORGANISATION_GROUP.name()),
            Role.APPLICATION_SUBMITTER
        ))
        .orElse(false);
  }

  public Set<Integer> getOrganisationUnitIds(ServiceUserDetail userDetail) {
    return organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(
            getOrganisationGroupIds(userDetail).stream().toList()
        )
        .stream()
        .map(OrganisationUnitJson::organisationUnitId)
        .collect(Collectors.toSet());
  }
}
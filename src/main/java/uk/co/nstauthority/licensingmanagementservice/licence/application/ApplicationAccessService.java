package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class ApplicationAccessService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final TeamQueryService teamQueryService;
  public static final String ORGANISATION = "ORGANISATION";

  public ApplicationAccessService(
      OrganisationUnitQueryService organisationUnitQueryService,
      TeamQueryService teamQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.teamQueryService = teamQueryService;
  }

  public boolean userHasAccessToApplication(
      String applicationId,
      ApplicationType applicationType,
      Integer organisationUnitId,
      Long wuaId
  ) {
    var allowedRoles = Set.of(
        Role.MANAGE_TEAM,
        Role.EXTERNAL_APPLICATION_EDITOR,
        Role.APPLICATION_EDITOR,
        Role.APPLICATION_SUBMITTER
    );


    var organisationGroupIds = organisationUnitQueryService.findOrganisationGroupIdsByUnitId(organisationUnitId)
                                                           .stream()
                                                           .map(String::valueOf)
                                                           .collect(Collectors.toSet());

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
                                  && team.getScopeType().equals(ORGANISATION);

    return isExternalContributor || isOrganisationGroup;
  }

  public boolean userHasAccessToStartApplication(
      Long wuaId
  ) {
    var allowedRoles = Set.of(
        Role.APPLICATION_EDITOR,
        Role.APPLICATION_SUBMITTER
    );

    return teamQueryService.userHasRoleInTeamType(wuaId, TeamType.ORGANISATION, allowedRoles);
  }
}
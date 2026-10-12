package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Service
public class IndustryRoleService {

  private static final Set<Role> EDITOR_SUBMITTER_ROLES = Set.of(Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER);

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final TeamQueryService teamQueryService;

  public IndustryRoleService(
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService,
      TeamQueryService teamQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.teamQueryService = teamQueryService;
  }

  public boolean userHasEditorOrSubmitterRole(Long wuaId) {
    return teamQueryService.userHasRoleInTeamType(wuaId, TeamType.ORGANISATION, EDITOR_SUBMITTER_ROLES);
  }

  public boolean userHasEditorOrSubmitterRoleInOrganisationGroup(Long wuaId) {
    return !getEditorOrSubmitterOrganisationGroupIds(wuaId).isEmpty();
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

  public Set<Integer> getEditorOrSubmitterOrganisationUnitIds(Long wuaId) {
    return organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(
            List.copyOf(getEditorOrSubmitterOrganisationGroupIds(wuaId))
        )
        .stream()
        .map(OrganisationUnitJson::organisationUnitId)
        .collect(Collectors.toSet());
  }

  private Set<Integer> getEditorOrSubmitterOrganisationGroupIds(Long wuaId) {
    return teamQueryService.getTeamRolesForUser(wuaId)
        .stream()
        .filter(teamRole ->
                    teamRole.getTeam().getTeamType() == TeamType.ORGANISATION
                    && ScopeType.ORGANISATION_GROUP.name().equals(teamRole.getTeam().getScopeType())
                    && EDITOR_SUBMITTER_ROLES.contains(teamRole.getRole())
        )
        .map(teamRole -> Integer.valueOf(teamRole.getTeam().getScopeId()))
        .collect(Collectors.toSet());
  }
}

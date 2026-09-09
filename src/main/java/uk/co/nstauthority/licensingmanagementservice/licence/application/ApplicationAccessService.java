package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ApplicationAccessService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final TeamQueryService teamQueryService;
  private final Set<Role> editorSubmitterRoles = Set.of(Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER);
  public static final Set<Role> STEWARD_ROLES = EnumSet.of(
      Role.STEWARD_OFFSHORE,
      Role.STEWARD_CARBON_STORAGE,
      Role.STEWARD_ONSHORE
  );
  public static final Set<Role> CONTINUATION_REVIEWER_ROLES = EnumSet.of(
      Role.CONTINUATION_REVIEWER_OFFSHORE,
      Role.CONTINUATION_REVIEWER_ONSHORE
  );
  public static final Set<Role> DECISION_ISSUER_ROLES = EnumSet.of(
      Role.DECISION_ISSUER_OFFSHORE,
      Role.DECISION_ISSUER_CARBON_STORAGE,
      Role.DECISION_ISSUER_ONSHORE
  );

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
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> orgUnitIdToGroupId,
      Long wuaId
  ) {
    var applicationId = applicationDetail.getLicenceApplication().getId();
    var applicationType = applicationDetail.getLicenceApplication().getApplicationType();
    var appHasBeenSubmitted = applicationDetail.getSubmittedDatetime() != null;

    var teamRoles = teamQueryService.getTeamRolesForUser(wuaId);

    var allowedDrafterRoles = Set.of(Role.EXTERNAL_APPLICATION_EDITOR, Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER);

    if (!appHasBeenSubmitted) {
      var responsibleOrgGroupIds = Optional.ofNullable(applicationDetail.getResponsibleOrganisationUnitId())
          .map(orgUnitIdToGroupId::get)
          .map(groupId -> Set.of(String.valueOf(groupId)))
          .orElse(Set.of());

      return teamRoles.stream()
          .filter(teamRole -> allowedDrafterRoles.contains(teamRole.getRole()))
          .map(TeamRole::getTeam)
          .anyMatch(team ->
              isExternalContributor(team, applicationId.toString(), applicationType)
              || isLicenseeOrgGroupMember(team, responsibleOrgGroupIds)
          );
    }

    var allowedSubmittedRoles = StreamUtil.unionSets(
        allowedDrafterRoles,
        STEWARD_ROLES,
        CaseManagerRoles.ROLES,
        CONTINUATION_REVIEWER_ROLES,
        DECISION_ISSUER_ROLES
    );

    var organisationGroupIds = orgUnitIdToGroupId.values().stream()
        .map(String::valueOf)
        .collect(Collectors.toSet());

    return teamRoles.stream()
        .filter(teamRole -> allowedSubmittedRoles.contains(teamRole.getRole()))
        .anyMatch(teamRole ->
            isExternalContributor(teamRole.getTeam(), applicationId.toString(), applicationType)
            || isLicenseeOrgGroupMember(teamRole.getTeam(), organisationGroupIds)
            || isCaseManagerOrSteward(teamRole.getRole(), applicationDetail.getLicence().getType())
            || isContinuationReviewer(teamRole.getRole(), applicationType)
            || isDecisionIssuer(teamRole.getRole())
        );
  }

  private boolean isLicenseeOrgGroupMember(Team team, Set<String> organisationGroupIds) {
    return team.getTeamType() == TeamType.ORGANISATION
        && ScopeType.ORGANISATION_GROUP.name().equals(team.getScopeType())
        && organisationGroupIds.contains(team.getScopeId());
  }

  private boolean isExternalContributor(Team team, String applicationId, ApplicationType applicationType) {
    return team.getTeamType() == TeamType.EXTERNAL_CONTRIBUTORS
        && applicationId.equals(team.getScopeId())
        && team.getScopeType().equals(applicationType.name());
  }

  private boolean isCaseManagerOrSteward(
      Role role,
      LicenceType licenceType
  ) {
    var isSteward = STEWARD_ROLES.contains(role);
    var isCaseManager = CaseManagerRoles.getRequiredRoleForLicenceType(licenceType)
        .map(requiredRole -> requiredRole == role)
        .orElse(false);

    return isCaseManager || isSteward;
  }

  private boolean isContinuationReviewer(Role role, ApplicationType applicationType) {
    return CONTINUATION_REVIEWER_ROLES.contains(role)
           && applicationType == ApplicationType.CONTINUATION_APPLICATION;
  }

  private boolean isDecisionIssuer(Role role) {
    return DECISION_ISSUER_ROLES.contains(role);
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
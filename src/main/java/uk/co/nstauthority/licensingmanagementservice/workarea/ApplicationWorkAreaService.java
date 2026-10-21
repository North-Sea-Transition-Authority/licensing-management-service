package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class ApplicationWorkAreaService {

  private final RegulatorRoleService regulatorRoleService;

  public ApplicationWorkAreaService(RegulatorRoleService regulatorRoleService) {
    this.regulatorRoleService = regulatorRoleService;
  }

  public boolean isRegulatorMyWork(LicenceApplicationDetail applicationDetail, Long userWuaId, Set<TeamRole> userTeamRoles) {
    return switch (applicationDetail) {
      case ScheduleWorkProgrammeApplicationDetail scheduleDetail ->
          isScheduleApplicationMyWork(scheduleDetail, userWuaId, userTeamRoles);
      // TODO LMS1-701: continuation reviewer and issuer rules
      default -> false;
    };
  }

  public boolean isRegulatorAllWork(LicenceApplicationDetail applicationDetail, Long userWuaId, Set<TeamRole> userTeamRoles) {
    return switch (applicationDetail) {
      case ScheduleWorkProgrammeApplicationDetail scheduleDetail ->
          isScheduleApplicationAllWork(scheduleDetail, userWuaId, userTeamRoles);
      // TODO LMS1-701: continuation reviewer and issuer rules
      default -> false;
    };
  }

  public boolean isIndustryMyWork(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> licenseeIdToGroupId,
      Long userWuaId,
      Set<TeamRole> userTeamRoles
  ) {
    var createdByUser = Objects.equals(applicationDetail.getCreatedByWuaId(), userWuaId);
    var isAppEditorOrSubmitter = hasRoleInLicenseeGroups(
        applicationDetail, licenseeIdToGroupId, userTeamRoles, IndustryRoleService.EDITOR_SUBMITTER_ROLES);
    if (isAppEditorOrSubmitter && createdByUser) {
      return true;
    }

    if (isExternalEditorOnApplication(applicationDetail, userTeamRoles)) {
      return true;
    }

    var submittedByUser = Objects.equals(applicationDetail.getSubmittedByWuaId(), userWuaId);
    var isAppSubmitter = hasRoleInLicenseeGroups(
        applicationDetail, licenseeIdToGroupId, userTeamRoles, Set.of(Role.APPLICATION_SUBMITTER));
    return submittedByUser && isAppSubmitter;
  }

  private boolean isScheduleApplicationMyWork(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Long userWuaId,
      Set<TeamRole> userTeamRoles
  ) {
    var licenceType = applicationDetail.getLicence().getType();
    var status = applicationDetail.getStatus();
    var stewardWuaId = applicationDetail.getScheduleWorkProgrammeApplication().getStewardWuaId();

    if (regulatorRoleService.isCaseManagerFor(userTeamRoles, licenceType)
        && (status == ApplicationStatus.DSP_UPLOADED || (status == ApplicationStatus.SUBMITTED && stewardWuaId == null))) {
      return true;
    }

    if (regulatorRoleService.isStewardFor(userTeamRoles, licenceType)
        && status == ApplicationStatus.SUBMITTED
        && Objects.equals(stewardWuaId, userWuaId)) {
      return true;
    }

    return regulatorRoleService.isDecisionIssuer(userTeamRoles) && status == ApplicationStatus.ISSUE_DECISION;
  }

  private boolean isScheduleApplicationAllWork(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Long userWuaId,
      Set<TeamRole> userTeamRoles
  ) {
    var licenceType = applicationDetail.getLicence().getType();
    var status = applicationDetail.getStatus();
    var stewardWuaId = applicationDetail.getScheduleWorkProgrammeApplication().getStewardWuaId();

    if (regulatorRoleService.isCaseManagerFor(userTeamRoles, licenceType) && applicationDetail.hasBeenSubmitted()) {
      return true;
    }

    if (regulatorRoleService.isStewardFor(userTeamRoles, licenceType)) {
      var isAllocatedSteward = Objects.equals(stewardWuaId, userWuaId);
      if (status == ApplicationStatus.SUBMITTED || (status == ApplicationStatus.DSP_UPLOADED && isAllocatedSteward)) {
        return true;
      }
    }

    return regulatorRoleService.isDecisionIssuer(userTeamRoles) && status == ApplicationStatus.ISSUE_DECISION;
  }

  private boolean isExternalEditorOnApplication(LicenceApplicationDetail applicationDetail, Set<TeamRole> userTeamRoles) {
    var licenceApplication = applicationDetail.getLicenceApplication();

    return userTeamRoles.stream()
        .filter(teamRole -> teamRole.getRole() == Role.EXTERNAL_APPLICATION_EDITOR)
        .anyMatch(teamRole -> isExternalContributor(teamRole.getTeam(), licenceApplication));
  }

  private boolean hasRoleInLicenseeGroups(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> licenseeIdToGroupId,
      Set<TeamRole> userTeamRoles,
      Set<Role> roles
  ) {
    var licenseeGroupIds = getApplicationLicenseeGroupIds(applicationDetail, licenseeIdToGroupId);

    return userTeamRoles.stream()
        .filter(teamRole -> roles.contains(teamRole.getRole()))
        .anyMatch(teamRole -> isLicenseeOrgGroupMember(teamRole.getTeam(), licenseeGroupIds));
  }

  private Set<String> getApplicationLicenseeGroupIds(
      LicenceApplicationDetail applicationDetail,
      Map<Integer, Integer> licenseeIdToGroupId
  ) {
    if (applicationDetail.hasBeenSubmitted()) {
      return licenseeIdToGroupId.values().stream()
          .map(String::valueOf)
          .collect(Collectors.toSet());
    }

    return Optional.ofNullable(applicationDetail.getResponsibleOrganisationUnitId())
        .map(licenseeIdToGroupId::get)
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

package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

class ApplicationWorkAreaServiceTest {

  private static final Long USER_WUA_ID = 1L;
  private static final UUID APP_ID = UUID.randomUUID();
  private static final int ORG_UNIT_ID = 100;
  private static final int ORG_GROUP_ID = 999;

  private final ApplicationWorkAreaService applicationWorkAreaService = new ApplicationWorkAreaService();

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {"APPLICATION_EDITOR", "APPLICATION_SUBMITTER"})
  void isMyWork_whenCreatedByUserAndEditorOrSubmitterInResponsibleOrgGroup_returnsTrue(Role role) {
    var teamRoles = Set.of(buildTeamRole(role, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isTrue();
  }

  @Test
  void isMyWork_whenCreatedByUserButOnlyLicenceViewerInResponsibleOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.VIEW_ORGANISATION_LICENCES, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isMyWork_whenCreatedByUserButEditorInDifferentOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID + 1)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isMyWork_whenCreatedBySomeoneElse_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(USER_WUA_ID + 1, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isMyWork_whenSubmittedByUserAndSubmitterInAnyLicenseeOrgGroup_returnsTrue() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_SUBMITTER, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(null, USER_WUA_ID, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID, ORG_UNIT_ID + 1, ORG_GROUP_ID + 1),
        USER_WUA_ID,
        teamRoles
    )).isTrue();
  }

  @Test
  void isMyWork_whenSubmittedByUserButOnlyEditorInLicenseeOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isMyWork(
        mockDetail(null, USER_WUA_ID, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isMyWork_whenExternalEditorOnThisApplication_returnsTrue() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_EDITOR,
        buildExternalTeam(APP_ID, ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isTrue();
  }

  @Test
  void isMyWork_whenExternalViewerOnly_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_VIEWER,
        buildExternalTeam(APP_ID, ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isFalse();
  }

  @Test
  void isMyWork_whenExternalEditorOnDifferentApplication_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_EDITOR,
        buildExternalTeam(UUID.randomUUID(), ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isFalse();
  }

  private LicenceApplicationDetail mockDetail(
      Long createdByWuaId,
      Long submittedByWuaId,
      Integer responsibleOrganisationUnitId,
      Instant submittedDatetime
  ) {
    var licenceApplication = mock(LicenceApplication.class);
    lenient().when(licenceApplication.getId()).thenReturn(APP_ID);
    lenient().when(licenceApplication.getApplicationType()).thenReturn(ApplicationType.CONTINUATION_APPLICATION);

    var detail = mock(LicenceApplicationDetail.class);
    lenient().when(detail.getLicenceApplication()).thenReturn(licenceApplication);
    lenient().when(detail.getCreatedByWuaId()).thenReturn(createdByWuaId);
    lenient().when(detail.getSubmittedByWuaId()).thenReturn(submittedByWuaId);
    lenient().when(detail.getResponsibleOrganisationUnitId()).thenReturn(responsibleOrganisationUnitId);
    lenient().when(detail.hasBeenSubmitted()).thenReturn(submittedDatetime != null);

    return detail;
  }

  private Team buildOrgGroupTeam(int orgGroupId) {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.ORGANISATION);
    team.setScopeId(String.valueOf(orgGroupId));
    team.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    return team;
  }

  private Team buildExternalTeam(UUID applicationId, ApplicationType applicationType) {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    team.setScopeId(applicationId.toString());
    team.setScopeType(applicationType.name());
    return team;
  }

  private TeamRole buildTeamRole(Role role, Team team) {
    return TeamRoleTestUtil.newBuilder().withRole(role).withTeam(team).build();
  }
}

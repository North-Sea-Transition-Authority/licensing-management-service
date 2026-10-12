package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private ApplicationAccessService applicationAccessService;

  private static final Long USER_1_WUA_ID = 1L;
  private static final UUID APP_ID = UUID.randomUUID();
  private static final int ORG_UNIT_ID = 100;
  private static final int ORG_GROUP_ID = 999;

  @Test
  void userHasAccessToApplication_whenExternalContributor_Continuation_returnsTrue() {
    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(APP_ID.toString());
    externalTeam.setScopeType(ApplicationType.CONTINUATION_APPLICATION.name());

    var role = buildTeamRole(Role.EXTERNAL_APPLICATION_EDITOR, externalTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.CONTINUATION_APPLICATION), Map.of(), USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenExternalContributor_Continuation_returnsFalse() {
    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(APP_ID.toString());
    externalTeam.setScopeType(ApplicationType.CONTINUATION_APPLICATION.name());

    var role = buildTeamRole(Role.VIEW_ANY_LICENCE, externalTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.CONTINUATION_APPLICATION), Map.of(), USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenExternalContributor_returnsTrue() {
    Team externalTeam = new Team(UUID.randomUUID());
    externalTeam.setTeamType(TeamType.EXTERNAL_CONTRIBUTORS);
    externalTeam.setScopeId(APP_ID.toString());
    externalTeam.setScopeType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name());

    var role = buildTeamRole(Role.EXTERNAL_APPLICATION_EDITOR, externalTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION), Map.of(), USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenOrganisationGroupMember_returnsTrue() {

    Team orgTeam = new Team(UUID.randomUUID());
    orgTeam.setTeamType(TeamType.ORGANISATION);
    orgTeam.setScopeId(String.valueOf(ORG_GROUP_ID));
    orgTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());

    var role = buildTeamRole(Role.APPLICATION_SUBMITTER, orgTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenSubmitted_andUserIsInLicenseeOrgGroup_returnsTrue() {

    Team orgTeam = new Team(UUID.randomUUID());
    orgTeam.setTeamType(TeamType.ORGANISATION);
    orgTeam.setScopeId(String.valueOf(ORG_GROUP_ID));
    orgTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());

    var role = buildTeamRole(Role.APPLICATION_SUBMITTER, orgTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenSubmitted_andUserIsNotInLicenseeOrgGroup_returnsFalse() {

    var otherTeam = new Team(UUID.randomUUID());
    otherTeam.setTeamType(TeamType.ORGANISATION);
    otherTeam.setScopeId("888");
    otherTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());

    var role = buildTeamRole(Role.APPLICATION_SUBMITTER, otherTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenDraft_andUserIsInNonResponsibleOrgGroup_returnsFalse() {

    Team nonResponsibleTeam = new Team(UUID.randomUUID());
    nonResponsibleTeam.setTeamType(TeamType.ORGANISATION);
    nonResponsibleTeam.setScopeId("888");
    nonResponsibleTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());

    var role = buildTeamRole(Role.APPLICATION_SUBMITTER, nonResponsibleTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenNoRelevantRole_returnsFalse() {
    Team team = buildTeam(TeamType.EXTERNAL_CONTRIBUTORS);
    team.setScopeId(APP_ID.toString());

    var role = buildTeamRole(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, team);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION), Map.of(), USER_1_WUA_ID)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(value = TeamType.class, names = {
      "LICENCE_MANAGEMENT",
      "OFFSHORE_PRODUCTION_LICENSING",
      "ONSHORE_PRODUCTION_LICENSING",
      "CARBON_STORAGE_LICENSING",
      "REGULATIONS_LICENSING"
  })
  void userHasAccessToApplication_whenRegulatorAndSubmitted_returnsTrue(TeamType regulatorTeamType) {
    var role = buildTeamRole(Role.VIEW_ANY_LICENCE, buildTeam(regulatorTeamType));
    var detail = mock(LicenceApplicationDetail.class);
    when(detail.hasBeenSubmitted()).thenReturn(true);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(detail, Map.of(), USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenRegulatorAndDraft_returnsFalse() {
    var role = buildTeamRole(Role.STEWARD_OFFSHORE, buildTeam(TeamType.LICENCE_MANAGEMENT));

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION), Map.of(), USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenRegulatorWithIndustryRoleAndDraft_returnsTrue() {
    var regulatorRole = buildTeamRole(Role.STEWARD_OFFSHORE, buildTeam(TeamType.LICENCE_MANAGEMENT));

    var orgTeam = buildTeam(TeamType.ORGANISATION);
    orgTeam.setScopeId(String.valueOf(ORG_GROUP_ID));
    orgTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    var industryRole = buildTeamRole(Role.APPLICATION_EDITOR, orgTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(regulatorRole, industryRole));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isTrue();
  }

  @Test
  void userHasIndustryAccessToApplication_whenRegulatorOnlyAndSubmitted_returnsFalse() {
    var role = buildTeamRole(Role.STEWARD_OFFSHORE, buildTeam(TeamType.LICENCE_MANAGEMENT));

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasIndustryAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isFalse();
  }

  @Test
  void userHasIndustryAccessToApplication_whenSubmitted_andUserIsInLicenseeOrgGroup_returnsTrue() {
    var orgTeam = buildTeam(TeamType.ORGANISATION);
    orgTeam.setScopeId(String.valueOf(ORG_GROUP_ID));
    orgTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    var role = buildTeamRole(Role.APPLICATION_SUBMITTER, orgTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasIndustryAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_1_WUA_ID
    )).isTrue();
  }

  private LicenceApplicationDetail mockApplicationDetail(ApplicationType applicationType) {
    return mockApplicationDetail(applicationType, null, null);
  }

  private LicenceApplicationDetail mockApplicationDetail(
      ApplicationType applicationType,
      Integer responsibleOrganisationUnitId,
      Instant submittedDatetime
  ) {
    var licenceApplication = mock(LicenceApplication.class);
    lenient().when(licenceApplication.getId()).thenReturn(APP_ID);
    lenient().when(licenceApplication.getApplicationType()).thenReturn(applicationType);

    var detail = mock(LicenceApplicationDetail.class);
    when(detail.getLicenceApplication()).thenReturn(licenceApplication);

    if (responsibleOrganisationUnitId != null) {
      when(detail.getResponsibleOrganisationUnitId()).thenReturn(responsibleOrganisationUnitId);
    }

    when(detail.hasBeenSubmitted()).thenReturn(submittedDatetime != null);

    return detail;
  }

  private Team buildTeam(TeamType teamType) {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(teamType);
    return team;
  }

  private TeamRole buildTeamRole(Role role, Team team) {
    return TeamRoleTestUtil.newBuilder().withRole(role).withTeam(team).build();
  }
}

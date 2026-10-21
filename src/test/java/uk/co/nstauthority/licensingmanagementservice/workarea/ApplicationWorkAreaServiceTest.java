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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

class ApplicationWorkAreaServiceTest {

  private static final Long USER_WUA_ID = 1L;
  private static final Long OTHER_WUA_ID = 2L;
  private static final UUID APP_ID = UUID.randomUUID();
  private static final int ORG_UNIT_ID = 100;
  private static final int ORG_GROUP_ID = 999;

  private final ApplicationWorkAreaService applicationWorkAreaService =
      new ApplicationWorkAreaService(new RegulatorRoleService(mock(TeamQueryService.class)));

  @ParameterizedTest
  @CsvSource({
      "CASE_MANAGER_OFFSHORE, SUBMITTED, NONE, true",
      "CASE_MANAGER_OFFSHORE, SUBMITTED, OTHER, false",
      "CASE_MANAGER_OFFSHORE, DSP_UPLOADED, OTHER, true",
      "CASE_MANAGER_OFFSHORE, ISSUE_DECISION, NONE, false",
      "CASE_MANAGER_OFFSHORE, DRAFT, NONE, false",
      "CASE_MANAGER_ONSHORE, SUBMITTED, NONE, false",
      "CASE_MANAGER_ONSHORE, DSP_UPLOADED, NONE, false",
      "STEWARD_OFFSHORE, SUBMITTED, ME, true",
      "STEWARD_OFFSHORE, SUBMITTED, OTHER, false",
      "STEWARD_OFFSHORE, SUBMITTED, NONE, false",
      "STEWARD_OFFSHORE, DSP_UPLOADED, ME, false",
      "STEWARD_ONSHORE, SUBMITTED, ME, false",
      "DECISION_ISSUER_OFFSHORE, ISSUE_DECISION, NONE, true",
      "DECISION_ISSUER_OFFSHORE, DSP_UPLOADED, NONE, false",
      "DECISION_ISSUER_OFFSHORE, SUBMITTED, NONE, false"
  })
  void isRegulatorMyWork_scheduleApplication_returnsWhetherRoleStatusAndStewardMatch(
      Role role,
      ApplicationStatus status,
      StewardAllocation stewardAllocation,
      boolean expected
  ) {
    var applicationDetail = createScheduleApplicationDetail(status, stewardAllocation);

    var result = applicationWorkAreaService.isRegulatorMyWork(
        applicationDetail,
        USER_WUA_ID,
        Set.of(TeamRoleTestUtil.newBuilder().withRole(role).build())
    );

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void isRegulatorMyWork_scheduleApplication_whenUserHasNoRegulatorRoles_returnsFalse() {
    var applicationDetail = createScheduleApplicationDetail(ApplicationStatus.SUBMITTED, StewardAllocation.NONE);

    assertThat(applicationWorkAreaService.isRegulatorMyWork(applicationDetail, USER_WUA_ID, Set.of())).isFalse();
  }

  @Test
  void isRegulatorMyWork_scheduleApplication_whenUserHasSeveralRoles_returnsTrueIfAnyRoleMatches() {
    var applicationDetail = createScheduleApplicationDetail(ApplicationStatus.ISSUE_DECISION, StewardAllocation.NONE);
    var teamRoles = Set.of(
        TeamRoleTestUtil.newBuilder().withRole(Role.CASE_MANAGER_OFFSHORE).build(),
        TeamRoleTestUtil.newBuilder().withRole(Role.DECISION_ISSUER_ONSHORE).build()
    );

    assertThat(applicationWorkAreaService.isRegulatorMyWork(applicationDetail, USER_WUA_ID, teamRoles)).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "CASE_MANAGER_OFFSHORE, SUBMITTED, OTHER, true",
      "CASE_MANAGER_OFFSHORE, DSP_UPLOADED, OTHER, true",
      "CASE_MANAGER_OFFSHORE, ISSUE_DECISION, OTHER, true",
      "CASE_MANAGER_OFFSHORE, DRAFT, OTHER, false",
      "CASE_MANAGER_ONSHORE, SUBMITTED, OTHER, false",
      "STEWARD_OFFSHORE, SUBMITTED, OTHER, true",
      "STEWARD_OFFSHORE, SUBMITTED, NONE, true",
      "STEWARD_OFFSHORE, DSP_UPLOADED, ME, true",
      "STEWARD_OFFSHORE, DSP_UPLOADED, OTHER, false",
      "STEWARD_OFFSHORE, DSP_UPLOADED, NONE, false",
      "STEWARD_OFFSHORE, ISSUE_DECISION, ME, false",
      "STEWARD_ONSHORE, SUBMITTED, OTHER, false",
      "STEWARD_ONSHORE, DSP_UPLOADED, ME, false",
      "DECISION_ISSUER_OFFSHORE, ISSUE_DECISION, OTHER, true",
      "DECISION_ISSUER_OFFSHORE, SUBMITTED, OTHER, false",
      "DECISION_ISSUER_OFFSHORE, DSP_UPLOADED, OTHER, false"
  })
  void isRegulatorAllWork_scheduleApplication_returnsWhetherRoleStatusAndStewardMatch(
      Role role,
      ApplicationStatus status,
      StewardAllocation stewardAllocation,
      boolean expected
  ) {
    var applicationDetail = createScheduleApplicationDetail(status, stewardAllocation);

    var result = applicationWorkAreaService.isRegulatorAllWork(
        applicationDetail,
        USER_WUA_ID,
        Set.of(TeamRoleTestUtil.newBuilder().withRole(role).build())
    );

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void isRegulatorAllWork_scheduleApplication_whenUserHasSeveralRoles_returnsTrueIfAnyRoleMatches() {
    var applicationDetail = createScheduleApplicationDetail(ApplicationStatus.ISSUE_DECISION, StewardAllocation.NONE);
    var teamRoles = Set.of(
        TeamRoleTestUtil.newBuilder().withRole(Role.STEWARD_OFFSHORE).build(),
        TeamRoleTestUtil.newBuilder().withRole(Role.DECISION_ISSUER_CARBON_STORAGE).build()
    );

    assertThat(applicationWorkAreaService.isRegulatorAllWork(applicationDetail, USER_WUA_ID, teamRoles)).isTrue();
  }

  @Test
  void isRegulatorAllWork_scheduleApplication_whenUserHasNoRegulatorRoles_returnsFalse() {
    var applicationDetail = createScheduleApplicationDetail(ApplicationStatus.SUBMITTED, StewardAllocation.NONE);

    assertThat(applicationWorkAreaService.isRegulatorAllWork(applicationDetail, USER_WUA_ID, Set.of())).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
      "CONTINUATION_REVIEWER_OFFSHORE, SUBMITTED, true",
      "CONTINUATION_REVIEWER_ONSHORE, SUBMITTED, true",
      "CONTINUATION_REVIEWER_OFFSHORE, ISSUE_DECISION, false",
      "CONTINUATION_REVIEWER_OFFSHORE, DRAFT, false",
      "CONTINUATION_ISSUER, ISSUE_DECISION, true",
      "CONTINUATION_ISSUER, SUBMITTED, false",
      "CONTINUATION_ISSUER, COMPLETE, false",
      "DECISION_ISSUER_OFFSHORE, ISSUE_DECISION, false",
      "DECISION_ISSUER_OFFSHORE, SUBMITTED, false",
      "CASE_MANAGER_OFFSHORE, SUBMITTED, false"
  })
  void isRegulatorMyWork_continuationApplication_returnsWhetherRoleAndStatusMatch(
      Role role,
      ApplicationStatus status,
      boolean expected
  ) {
    var applicationDetail = createContinuationApplicationDetail(status);

    var result = applicationWorkAreaService.isRegulatorMyWork(
        applicationDetail,
        USER_WUA_ID,
        Set.of(TeamRoleTestUtil.newBuilder().withRole(role).build())
    );

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void isRegulatorMyWork_continuationApplication_whenUserHasNoRegulatorRoles_returnsFalse() {
    var applicationDetail = createContinuationApplicationDetail(ApplicationStatus.SUBMITTED);

    assertThat(applicationWorkAreaService.isRegulatorMyWork(applicationDetail, USER_WUA_ID, Set.of())).isFalse();
  }

  @Test
  void isRegulatorMyWork_continuationApplication_whenReviewerAndIssuer_returnsTrueIfAnyRoleMatches() {
    var applicationDetail = createContinuationApplicationDetail(ApplicationStatus.ISSUE_DECISION);
    var teamRoles = Set.of(
        TeamRoleTestUtil.newBuilder().withRole(Role.CONTINUATION_REVIEWER_OFFSHORE).build(),
        TeamRoleTestUtil.newBuilder().withRole(Role.CONTINUATION_ISSUER).build()
    );

    assertThat(applicationWorkAreaService.isRegulatorMyWork(applicationDetail, USER_WUA_ID, teamRoles)).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "CONTINUATION_REVIEWER_OFFSHORE, SUBMITTED, true",
      "CONTINUATION_REVIEWER_ONSHORE, ISSUE_DECISION, true",
      "CONTINUATION_REVIEWER_OFFSHORE, COMPLETE, true",
      "CONTINUATION_REVIEWER_OFFSHORE, WITHDRAWN, true",
      "CONTINUATION_REVIEWER_OFFSHORE, DRAFT, false",
      "CONTINUATION_ISSUER, SUBMITTED, false",
      "CONTINUATION_ISSUER, ISSUE_DECISION, true",
      "CONTINUATION_ISSUER, COMPLETE, true",
      "CONTINUATION_ISSUER, DRAFT, false",
      "DECISION_ISSUER_OFFSHORE, ISSUE_DECISION, false",
      "CASE_MANAGER_OFFSHORE, SUBMITTED, false"
  })
  void isRegulatorAllWork_continuationApplication_returnsWhetherRoleAndStatusMatch(
      Role role,
      ApplicationStatus status,
      boolean expected
  ) {
    var applicationDetail = createContinuationApplicationDetail(status);

    var result = applicationWorkAreaService.isRegulatorAllWork(
        applicationDetail,
        USER_WUA_ID,
        Set.of(TeamRoleTestUtil.newBuilder().withRole(role).build())
    );

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void isRegulatorAllWork_continuationApplication_whenUserHasNoRegulatorRoles_returnsFalse() {
    var applicationDetail = createContinuationApplicationDetail(ApplicationStatus.SUBMITTED);

    assertThat(applicationWorkAreaService.isRegulatorAllWork(applicationDetail, USER_WUA_ID, Set.of())).isFalse();
  }

  private LicenceContinuationApplicationDetail createContinuationApplicationDetail(ApplicationStatus status) {
    return LicenceContinuationApplicationTestUtil.builder()
        .withStatus(status)
        .withSubmittedDatetime(status == ApplicationStatus.DRAFT ? null : Instant.now())
        .build();
  }

  private ScheduleWorkProgrammeApplicationDetail createScheduleApplicationDetail(
      ApplicationStatus status,
      StewardAllocation stewardAllocation
  ) {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withStatus(status)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withSubmittedDatetime(status == ApplicationStatus.DRAFT ? null : Instant.now())
        .build();

    applicationDetail.getScheduleWorkProgrammeApplication().setStewardWuaId(switch (stewardAllocation) {
      case NONE -> null;
      case ME -> USER_WUA_ID;
      case OTHER -> OTHER_WUA_ID;
    });

    return applicationDetail;
  }

  private enum StewardAllocation {
    NONE, ME, OTHER
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {"APPLICATION_EDITOR", "APPLICATION_SUBMITTER"})
  void isIndustryMyWork_whenCreatedByUserAndEditorOrSubmitterInResponsibleOrgGroup_returnsTrue(Role role) {
    var teamRoles = Set.of(buildTeamRole(role, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isTrue();
  }

  @Test
  void isIndustryMyWork_whenCreatedByUserButOnlyLicenceViewerInResponsibleOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.VIEW_ORGANISATION_LICENCES, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isIndustryMyWork_whenCreatedByUserButEditorInDifferentOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID + 1)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(USER_WUA_ID, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isIndustryMyWork_whenCreatedBySomeoneElse_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(USER_WUA_ID + 1, null, ORG_UNIT_ID, null),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isIndustryMyWork_whenSubmittedByUserAndSubmitterInAnyLicenseeOrgGroup_returnsTrue() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_SUBMITTER, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(null, USER_WUA_ID, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID, ORG_UNIT_ID + 1, ORG_GROUP_ID + 1),
        USER_WUA_ID,
        teamRoles
    )).isTrue();
  }

  @Test
  void isIndustryMyWork_whenSubmittedByUserButOnlyEditorInLicenseeOrgGroup_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(Role.APPLICATION_EDITOR, buildOrgGroupTeam(ORG_GROUP_ID)));

    assertThat(applicationWorkAreaService.isIndustryMyWork(
        mockDetail(null, USER_WUA_ID, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID),
        USER_WUA_ID,
        teamRoles
    )).isFalse();
  }

  @Test
  void isIndustryMyWork_whenExternalEditorOnThisApplication_returnsTrue() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_EDITOR,
        buildExternalTeam(APP_ID, ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isIndustryMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isTrue();
  }

  @Test
  void isIndustryMyWork_whenExternalViewerOnly_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_VIEWER,
        buildExternalTeam(APP_ID, ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isIndustryMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isFalse();
  }

  @Test
  void isIndustryMyWork_whenExternalEditorOnDifferentApplication_returnsFalse() {
    var teamRoles = Set.of(buildTeamRole(
        Role.EXTERNAL_APPLICATION_EDITOR,
        buildExternalTeam(UUID.randomUUID(), ApplicationType.CONTINUATION_APPLICATION)
    ));

    assertThat(applicationWorkAreaService.isIndustryMyWork(mockDetail(null, null, null, null), Map.of(), USER_WUA_ID, teamRoles)).isFalse();
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

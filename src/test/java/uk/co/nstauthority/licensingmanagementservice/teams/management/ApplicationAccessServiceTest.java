package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private ApplicationAccessService applicationAccessService;

  private static final Long USER_1_WUA_ID = 1L;
  private static final UUID APP_ID = UUID.randomUUID();
  private static final int ORG_UNIT_ID = 100;
  private static final int ORG_GROUP_ID = 999;
  private ServiceUserDetail organisationUser;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(USER_1_WUA_ID)
        .build();
  }

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

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsTrue() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.ORGANISATION)
        .withScopeType(ScopeType.ORGANISATION_GROUP.name())
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId()))
        .thenReturn(Set.of(teamRole));

    boolean userHasEditorOrSubmitterRoleInOrganisationGroup = applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(
        organisationUser);

    assertTrue(userHasEditorOrSubmitterRoleInOrganisationGroup);
  }

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsFalse() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.LICENCE_MANAGEMENT)
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId()))
        .thenReturn(Set.of(teamRole));

    boolean userHasEditorOrSubmitterRoleInOrganisationGroup = applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(organisationUser);

    assertFalse(userHasEditorOrSubmitterRoleInOrganisationGroup);
  }

  @Test
  void getOrganisationUnitIds_returnsCorrectUnitIds() {
    int unitId1 = 5001;
    int unitId2 = 5002;
    var unitJson1 = new OrganisationUnitJson(unitId1, "Unit 1");
    var unitJson2 = new OrganisationUnitJson(unitId2, "Unit 2");

    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(any())).thenReturn(List.of(unitJson1, unitJson2));

    var result = applicationAccessService.getOrganisationUnitIds(organisationUser);
    assertThat(result).containsExactlyInAnyOrder(unitId1, unitId2);
  }

  @Test
  void userIsSubmitterForOrganisationUnit_whenUserIsSubmitterInOrgGroup_returnsTrue() {
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID))
        .thenReturn(Optional.of(ORG_GROUP_ID));

    when(teamQueryService.userHasScopedRole(
        eq(USER_1_WUA_ID),
        eq(TeamType.ORGANISATION),
        any(TeamScopeReference.class),
        eq(Role.APPLICATION_SUBMITTER)
    )).thenReturn(true);

    assertThat(applicationAccessService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userIsSubmitterForOrganisationUnit_whenUserIsNotSubmitter_returnsFalse() {
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID))
        .thenReturn(Optional.of(ORG_GROUP_ID));

    when(teamQueryService.userHasScopedRole(
        eq(USER_1_WUA_ID),
        eq(TeamType.ORGANISATION),
        any(TeamScopeReference.class),
        eq(Role.APPLICATION_SUBMITTER)
    )).thenReturn(false);

    assertThat(applicationAccessService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void getAuthorizedOrganisationUnitIds_whenNoGroups_returnsEmptySet() {
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of())).thenReturn(List.of());

    var result = applicationAccessService.getOrganisationUnitIds(organisationUser);

    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {
      "STEWARD_NEW_VENTURES",
      "STEWARD_OPERATIONS",
      "STEWARD_CS_NEW_VENTURES",
      "STEWARD_CS_CTS"
  })
  void userHasAccessToApplication_whenUserIsSteward_returnsTrue(Role stewardRole) {
    var irrelevantTeam = buildTeam(TeamType.LICENCE_MANAGEMENT);
    var role = buildTeamRole(stewardRole, irrelevantTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID), USER_1_WUA_ID)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {
      "CASE_MANAGER_NEW_VENTURES",
      "CASE_MANAGER_CS_NEW_VENTURES",
      "CASE_MANAGER_OPERATIONS",
      "CASE_MANAGER_CS_CTS",
      "CASE_MANAGER_ONSHORE"
  })
  void userHasAccessToApplication_whenUserIsCaseManager_returnsTrue(Role caseManagerRole) {
    var irrelevantTeam = buildTeam(TeamType.ORGANISATION);
    irrelevantTeam.setScopeId(String.valueOf(ORG_GROUP_ID));

    var role = buildTeamRole(caseManagerRole, irrelevantTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.CONTINUATION_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID), USER_1_WUA_ID)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {
      "CONTINUATION_REVIEWER_OPERATIONS",
      "CONTINUATION_REVIEWER_NEW_VENTURES"
  })
  void userHasAccessToApplication_whenUserIsContinuationReviewer_returnsTrue(Role continuationReviewerRole) {
    var irrelevantTeam = buildTeam(TeamType.LICENCE_MANAGEMENT);
    var role = buildTeamRole(continuationReviewerRole, irrelevantTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.CONTINUATION_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID), USER_1_WUA_ID)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {
      "DECISION_ISSUER_NEW_VENTURES",
      "DECISION_ISSUER_OPERATIONS",
      "DECISION_ISSUER_CS_NEW_VENTURES",
      "DECISION_ISSUER_CS_CTS",
      "DECISION_ISSUER_ONSHORE"
  })
  void userHasAccessToApplication_whenUserIsDecisionIssuer_returnsTrue(Role decisionIssuerRole) {
    var irrelevantTeam = buildTeam(TeamType.OFFSHORE_PRODUCTION_LICENSING);
    var role = buildTeamRole(decisionIssuerRole, irrelevantTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, null, Instant.now()),
        Map.of(ORG_UNIT_ID, ORG_GROUP_ID), USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userHasAccessToApplication_whenDraft_andUserIsSteward_returnsFalse() {
    var stewardTeam = buildTeam(TeamType.LICENCE_MANAGEMENT);
    var role = buildTeamRole(Role.STEWARD_NEW_VENTURES, stewardTeam);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION),
        Map.of(), USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasAccessToApplication_whenUserIsContinuationReviewer_andAppIsNotContinuation_returnsFalse() {
    var team = buildTeam(TeamType.LICENCE_MANAGEMENT);
    var role = buildTeamRole(Role.CONTINUATION_REVIEWER_OPERATIONS, team);

    when(teamQueryService.getTeamRolesForUser(USER_1_WUA_ID)).thenReturn(Set.of(role));

    assertThat(applicationAccessService.userHasAccessToApplication(
        mockApplicationDetail(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION), Map.of(), USER_1_WUA_ID)).isFalse();
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
    when(licenceApplication.getId()).thenReturn(APP_ID);
    when(licenceApplication.getApplicationType()).thenReturn(applicationType);

    var detail = mock(LicenceApplicationDetail.class);
    when(detail.getLicenceApplication()).thenReturn(licenceApplication);

    if (responsibleOrganisationUnitId != null) {
      when(detail.getResponsibleOrganisationUnitId()).thenReturn(responsibleOrganisationUnitId);
    }

    when(detail.getSubmittedDatetime()).thenReturn(submittedDatetime);

    return detail;
  }

  private Team buildTeam(TeamType teamType) {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(teamType);
    return team;
  }

  private TeamRole buildTeamRole(Role role, Team team) {
    var teamRole = new TeamRole();
    teamRole.setTeam(team);
    teamRole.setRole(role);
    return teamRole;
  }
}

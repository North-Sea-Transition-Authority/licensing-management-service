package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationActionServiceTest {

  private static final Long USER_WUA_ID = 2L;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationActionService scheduleWorkProgrammeApplicationActionService;

  private ServiceUserDetail serviceUserDetail;

  @BeforeEach
  void setUp() {
    serviceUserDetail = ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(USER_WUA_ID)
        .build();
  }

  @Test
  void getAvailableUserActionItems_allocateSteward_availableWhenSubmittedAndRoleAllowed() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_allocateSteward_notAvailableWhenWrongStatus() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DRAFT)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_allocateSteward_notAvailableWhenRoleNotAllowed() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_allocateSteward_notAvailableWhenRoleIsForDifferentLicenceType() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_uploadDsp_availableWhenCaseManagerAndSubmitted() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail, false));
  }

  @Test
  void getAvailableUserActionItems_uploadDsp_availableWhenUserIsAllocatedStewardAndSubmitted() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    applicationDetail.getScheduleWorkProgrammeApplication().setStewardWuaId(USER_WUA_ID);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail, true));
  }

  @Test
  void getAvailableUserActionItems_uploadDsp_notAvailableWhenNotCaseManagerAndNotAllocatedSteward() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_uploadDsp_notAvailableWhenWrongStatus() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DRAFT)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_uploadDsp_notAvailableWhenCaseManagerRoleIsForDifferentLicenceType() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_recordDecision_availableWhenCaseManagerAndDspUploaded() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DSP_UPLOADED)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION.toActionItemView(applicationDetail, true));
  }

  @Test
  void getAvailableUserActionItems_recordDecision_notAvailableWhenWrongStatus() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.ISSUE_DECISION)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_recordDecision_notAvailableWhenNotCaseManager() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DSP_UPLOADED)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_caseManagerAndStewardNotAssigned_allocateStewardIsPrimary() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    var availableActions = scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    assertThat(availableActions)
        .contains(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail, true))
        .contains(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail, false));
  }

  @Test
  void getAvailableUserActionItems_caseManagerAndStewardAlreadyAssigned_allocateStewardIsNotPrimary() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    applicationDetail.getScheduleWorkProgrammeApplication().setStewardWuaId(99L);

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CARBON_STORAGE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    var availableActions = scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    assertThat(availableActions)
        .contains(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail, false));
  }

  @Test
  void getAvailableUserActionItems_stewardAndStewardAssigned_recordFinalDecisionIsPrimary() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    applicationDetail.getScheduleWorkProgrammeApplication().setStewardWuaId(USER_WUA_ID);

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    var availableActions = scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    assertThat(availableActions)
        .contains(ScheduleWorkProgrammeApplicationActionItem.UPLOAD_DSP.toActionItemView(applicationDetail, true))
        .contains(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail, false));
  }

  @Test
  void getAvailableUserActionItems_stewardAndStewardNotAssigned_allocateStewardIsNotPrimary() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OFFSHORE)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    var availableActions = scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    assertThat(availableActions)
        .contains(ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD.toActionItemView(applicationDetail, false));
  }
}

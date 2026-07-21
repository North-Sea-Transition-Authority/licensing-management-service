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
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OPERATIONS)
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
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OPERATIONS)
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
  void getAvailableUserActionItems_recordFinalDecision_availableWhenCaseManagerAndSubmitted() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CS_CTS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION.toActionItemView(applicationDetail, true));
  }

  @Test
  void getAvailableUserActionItems_recordFinalDecision_availableWhenUserIsAllocatedStewardAndSubmitted() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .build();
    applicationDetail.getScheduleWorkProgrammeApplication().setStewardWuaId(USER_WUA_ID);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OPERATIONS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION.toActionItemView(applicationDetail, true));
  }

  @Test
  void getAvailableUserActionItems_recordFinalDecision_notAvailableWhenNotCaseManagerAndNotAllocatedSteward() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.STEWARD_OPERATIONS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_recordFinalDecision_notAvailableWhenWrongStatus() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DRAFT)
        .build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CS_CTS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_correctlyAssignsPrimaryAndSecondaryFlags() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CASE_MANAGER_CS_CTS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    var availableActions = scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail);

    var allocateAction = availableActions.get(0);
    var recordDecisionAction = availableActions.get(1);

    assertThat(allocateAction.primaryAction()).isTrue();
    assertThat(recordDecisionAction.primaryAction()).isFalse();
  }
}

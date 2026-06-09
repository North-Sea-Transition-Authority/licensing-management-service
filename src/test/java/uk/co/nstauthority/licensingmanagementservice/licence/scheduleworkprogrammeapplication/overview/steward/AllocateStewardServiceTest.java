package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ExtendWith(MockitoExtension.class)
class AllocateStewardServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Mock
  private ClearDownWorkAreaLogService clearDownWorkAreaLogService;

  @InjectMocks
  private AllocateStewardService allocateStewardService;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeApplication> applicationCaptor;

  @Test
  void getStewardOptions_returnsUsersByWuaId() {
    var wuaId = 100L;
    var teamRole = new TeamRole();
    teamRole.setWuaId(wuaId);
    teamRole.setRole(Role.STEWARD_NEW_VENTURES);

    var user = new EnergyPortalUserJson(wuaId, null, "Jane", "Doe", null, null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(ApplicationAccessService.STEWARD_ROLES))
        .thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(wuaId)),
        AllocateStewardService.STEWARD_OPTIONS_PURPOSE))
        .thenReturn(List.of(user));

    Map<String, String> result = allocateStewardService.getStewardOptions();

    assertThat(result).containsEntry(String.valueOf(wuaId), "Jane Doe");
  }

  @Test
  void getStewardOptions_deduplicatesUsersWithMultipleRoles() {
    var wuaId = 100L;

    var teamRole1 = new TeamRole();
    teamRole1.setWuaId(wuaId);
    teamRole1.setRole(Role.STEWARD_NEW_VENTURES);

    var teamRole2 = new TeamRole();
    teamRole2.setWuaId(wuaId);
    teamRole2.setRole(Role.STEWARD_OPERATIONS);

    var user = new EnergyPortalUserJson(wuaId, null, "Jane", "Doe", null, null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(ApplicationAccessService.STEWARD_ROLES))
        .thenReturn(List.of(teamRole1, teamRole2));
    when(energyPortalUserService.findByWuaIds(
        eq(List.of((WebUserAccountId.from(wuaId)))),
        eq(AllocateStewardService.STEWARD_OPTIONS_PURPOSE)))
        .thenReturn(List.of(user));

    Map<String, String> result = allocateStewardService.getStewardOptions();

    assertThat(result).hasSize(1).containsEntry(String.valueOf(wuaId), "Jane Doe");
  }

  @Test
  void getFormForApplication_whenStewardSet_populatesForm() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    application.setStewardWuaId(42L);

    var form = allocateStewardService.getFormForApplication(application);

    assertThat(form.getStewardWuaId()).isEqualTo("42");
  }

  @Test
  void getFormForApplication_whenNoSteward_returnsEmptyForm() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());

    var form = allocateStewardService.getFormForApplication(application);

    assertThat(form.getStewardWuaId()).isNull();
  }

  @Test
  void saveSteward_setsWuaIdAndSaves() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    var stewardWuaId = 99L;

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.empty());

    allocateStewardService.saveSteward(application, stewardWuaId);

    verify(scheduleWorkProgrammeApplicationRepository).save(applicationCaptor.capture());
    assertThat(applicationCaptor.getValue().getStewardWuaId()).isEqualTo(stewardWuaId);
  }

  @Test
  void saveSteward_clearsWorkAreaViewRecordForNewlyAssignedStewardOnly() {
    var detailId = UUID.randomUUID();
    var newStewardWuaId = 42L;
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());

    var detail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(detailId)
        .build();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(detail));

    allocateStewardService.saveSteward(application, newStewardWuaId);

    verify(clearDownWorkAreaLogService).clearDownViewFor(
        newStewardWuaId,
        detailId,
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION
    );
  }
}

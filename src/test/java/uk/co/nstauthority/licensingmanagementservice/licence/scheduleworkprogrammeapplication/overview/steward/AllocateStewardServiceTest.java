package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.StewardAssignedNotificationService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ExtendWith(MockitoExtension.class)
class AllocateStewardServiceTest {

  private static final long USER_WUA_ID = 100L;
  public static final long STEWARD_WUA_ID = 101L;
  private static final long NEW_STEWARD_WUA_ID = 142L;

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

  @Mock
  private StewardAssignedNotificationService stewardAssignedNotificationService;

  @InjectMocks
  private AllocateStewardService allocateStewardService;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeApplication> applicationCaptor;

  @Test
  void getStewardOptions_returnsUsersByWuaId() {
    var teamRole = stubTeamRole(USER_WUA_ID, Role.STEWARD_OFFSHORE);

    var user = new EnergyPortalUserJson(USER_WUA_ID, null, "Jane", "Doe", null, null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.STEWARD_OFFSHORE)))
        .thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(USER_WUA_ID)),
        AllocateStewardService.STEWARD_OPTIONS_PURPOSE))
        .thenReturn(List.of(user));

    var result = allocateStewardService.getStewardOptions(LicenceType.SEAWARD_PRODUCTION);

    assertThat(result).containsEntry(String.valueOf(USER_WUA_ID), "Jane Doe");
  }

  @Test
  void getStewardOptions_whenLicenceTypeHasNoStewardRole_returnsEmptyAndSkipsLookup() {
    Map<String, String> result = allocateStewardService.getStewardOptions(LicenceType.GAS_STORAGE);

    assertThat(result).isEmpty();
    verifyNoInteractions(teamQueryService, energyPortalUserService);
  }

  @Test
  void getStewardOptions_deduplicatesUsersWithMultipleRoles() {
    var teamRole1 = stubTeamRole(USER_WUA_ID, Role.STEWARD_OFFSHORE);
    var teamRole2 = stubTeamRole(USER_WUA_ID, Role.STEWARD_ONSHORE);

    var user = new EnergyPortalUserJson(USER_WUA_ID, null, "Jane", "Doe", null, null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.STEWARD_OFFSHORE)))
        .thenReturn(List.of(teamRole1, teamRole2));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(USER_WUA_ID)),
        AllocateStewardService.STEWARD_OPTIONS_PURPOSE))
        .thenReturn(List.of(user));

    Map<String, String> result = allocateStewardService.getStewardOptions(LicenceType.SEAWARD_PRODUCTION);

    assertThat(result).hasSize(1).containsEntry(String.valueOf(USER_WUA_ID), "Jane Doe");
  }

  @Test
  void getFormForApplication_whenStewardSet_populatesForm() {
    var application = stubApplication();
    application.setStewardWuaId(NEW_STEWARD_WUA_ID);

    var form = allocateStewardService.getFormForApplication(application);

    assertThat(form.getStewardWuaId()).isEqualTo("142");
  }

  @Test
  void getFormForApplication_whenNoSteward_returnsEmptyForm() {
    var application = stubApplication();

    var form = allocateStewardService.getFormForApplication(application);

    assertThat(form.getStewardWuaId()).isNull();
  }

  @Test
  void saveSteward_setsWuaIdAndSaves() {
    var application = stubApplication();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.empty());

    allocateStewardService.saveSteward(application, STEWARD_WUA_ID);

    verify(scheduleWorkProgrammeApplicationRepository).save(applicationCaptor.capture());
    assertThat(applicationCaptor.getValue().getStewardWuaId()).isEqualTo(STEWARD_WUA_ID);
  }

  @Test
  void saveSteward_clearsWorkAreaViewRecordForNewlyAssignedStewardOnly() {
    var detailId = UUID.randomUUID();
    var application = stubApplication();

    var detail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(detailId)
        .build();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(detail));

    allocateStewardService.saveSteward(application, NEW_STEWARD_WUA_ID);

    verify(clearDownWorkAreaLogService).clearDownViewFor(
        NEW_STEWARD_WUA_ID,
        detailId,
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION
    );
  }

  @Test
  void saveSteward_sendsCaseAssignedNotificationToNewlyAssignedSteward() {
    var application = stubApplication();

    var detail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(detail));

    allocateStewardService.saveSteward(application, NEW_STEWARD_WUA_ID);

    verify(stewardAssignedNotificationService).sendCaseAssignedEmail(detail, NEW_STEWARD_WUA_ID);
  }

  @Test
  void saveSteward_whenStewardAlreadyAssigned_reassignsAndNotifiesNewStewardOnly() {
    var application = stubApplication();
    application.setStewardWuaId(STEWARD_WUA_ID);

    var detail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(detail));

    allocateStewardService.saveSteward(application, NEW_STEWARD_WUA_ID);

    verify(scheduleWorkProgrammeApplicationRepository).save(applicationCaptor.capture());
    assertThat(applicationCaptor.getValue().getStewardWuaId()).isEqualTo(NEW_STEWARD_WUA_ID);
    verify(stewardAssignedNotificationService).sendCaseAssignedEmail(detail, NEW_STEWARD_WUA_ID);
    verifyNoMoreInteractions(stewardAssignedNotificationService);
  }

  @Test
  void saveSteward_whenNoDetailFound_doesNotSendNotification() {
    var application = stubApplication();

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.empty());

    allocateStewardService.saveSteward(application, STEWARD_WUA_ID);

    verifyNoInteractions(stewardAssignedNotificationService);
  }

  private static ScheduleWorkProgrammeApplication stubApplication() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    return application;
  }

  private static TeamRole stubTeamRole(long userWuaId, Role role) {
    var teamRole = new TeamRole();
    teamRole.setWuaId(userWuaId);
    teamRole.setRole(role);
    return teamRole;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.search.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceActionServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @InjectMocks
  private LicenceActionService licenceActionService;

  private ServiceUserDetail serviceUserDetail;

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    serviceUserDetail =  ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @Test
  void getAvailableUserActionItems_licenceManagedByLms() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    Set<TeamRole> teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceNotManagedByLms() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleExists() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleExists_noScheduleRequirement() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    Set<TeamRole> teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    when(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).thenReturn(true);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).thenReturn(false);
    when(teamQueryService.getTeamRolesForUser(anyLong())).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist_licenceTypeNotSetupForSchedules() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.METHANE_DRAINAGE)
        .withStatus(LicenceStatus.EXTANT)
        .build();
    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(anyLong())).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }
}
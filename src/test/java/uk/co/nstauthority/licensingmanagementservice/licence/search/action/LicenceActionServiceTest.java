package uk.co.nstauthority.licensingmanagementservice.licence.search.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenceTimelinePositionTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceActionServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private Environment environment;

  @Mock
  private LicenceStatusService licenceStatusService;

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
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceNotManagedByLms() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_draftLicenceScheduleExists() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setStatus(LicenceScheduleDetailStatus.DRAFT);

    when(licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence)).thenReturn(List.of(licenceScheduleDetail));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContainAnyElementsOf(List.of(
            LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence),
            LicenceActionItem.UPDATE_LICENCE_SCHEDULE.toActionItemView(licence)
        ));
  }

  @Test
  void getAvailableUserActionItems_activeLicenceScheduleExists() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setStatus(LicenceScheduleDetailStatus.ACTIVE);

    when(licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence)).thenReturn(List.of(licenceScheduleDetail));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.UPDATE_LICENCE_SCHEDULE.toActionItemView(licence));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_deletedLicenceScheduleExists() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setStatus(LicenceScheduleDetailStatus.DELETED);

    when(licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence)).thenReturn(List.of(licenceScheduleDetail));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.UPDATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleExists_noScheduleRequirement() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var teamRoles = Set.of(teamRole);
    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(teamRoles);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence)).thenReturn(List.of());
    when(teamQueryService.getTeamRolesForUser(anyLong())).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.UPDATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist_licenceTypeNotSetupForSchedules() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.METHANE_DRAINAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);
    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(anyLong())).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_correctlyAssignsPrimaryAndSecondaryFlags() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(anyLong())).thenReturn(Set.of(teamRole));

    var availableActions = licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail);

    assertThat(availableActions).isNotEmpty();
    assertThat(availableActions).allMatch(action -> action.primaryAction() == false);
  }

  @Test
  void getAvailableUserActionItems_noOpenCorrection_includesStartCorrection() {
    when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(false);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.START_CORRECTION.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_whenLicenceTypeNotCorrectable_excludesStartCorrection() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_EXPLORATION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    lenient().when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);
    lenient().when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(false);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.START_CORRECTION.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_whenLicenceTypeCarbonStorage_includesStartCorrection() {
    when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(false);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .contains(LicenceActionItem.START_CORRECTION.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_withOpenCorrection_excludesStartCorrection() {
    when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(true);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail))
        .doesNotContain(LicenceActionItem.START_CORRECTION.toActionItemView(licence));
  }

  @Test
  void getTopLevelLicenceActionItems_assertOnlyActionsRegisteredAtTopLevelReturned() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getTopLevelLicenceActionItems(licence, serviceUserDetail))
        .containsExactly(
            LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(licence)
        );
  }

  @Test
  void getTopLevelLicenceActionItems_whenUserDoesNotHaveTheRequiredRoles_assertNoActions() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getTopLevelLicenceActionItems(licence, serviceUserDetail)).isEmpty();
  }

  @Test
  void getTopLevelLicenceActionItems_whenLicenceTypeNotManagedByLms_assertNoActions() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.OFFLINE_LICENCE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceActionService.getTopLevelLicenceActionItems(licence, serviceUserDetail)).isEmpty();
  }

  @Test
  void getLicenceActionItemsForTab_whenLicenceOverviewTab_assertScheduleActionsReturned() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence)).thenReturn(List.of());

    assertThat(licenceActionService.getLicenceActionItemsForTab(licence, serviceUserDetail, new LicenceScheduleTab()))
        .containsExactly(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getLicenceActionItemsForTab_whenLicenceTimelinePositionTab_assertCorrectionActionReturned() {
    when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(false);

    assertThat(licenceActionService.getLicenceActionItemsForTab(
        licence,
        serviceUserDetail,
        new LicenceTimelinePositionTab()
    )).containsExactly(LicenceActionItem.START_CORRECTION.toActionItemView(licence));
  }

  @Test
  void getLicenceActionItemsForTab_whenTabActionIsNotAvailableToTheUser_assertNoActions() {
    when(environment.acceptsProfiles(Profiles.of("enable-lms2"))).thenReturn(true);

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER) //TODO - LMS2-55: Define who can carry out corrections on a licence
        .build();

    when(teamQueryService.getTeamRolesForUser(ORGANISATION_USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(licenceCorrectionService.hasOpenCorrection(licence)).thenReturn(true);

    assertThat(licenceActionService.getLicenceActionItemsForTab(
        licence,
        serviceUserDetail,
        new LicenceTimelinePositionTab()
    )).isEmpty();
  }

  @Test
  void getLicenceActionItemsForTab_whenTabHasNoRegisteredActions_assertNoActionsAndNoLookups() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    assertThat(licenceActionService.getLicenceActionItemsForTab(licence, serviceUserDetail, new UnregisteredLicenceTab()))
        .isEmpty();

    verifyNoInteractions(teamQueryService, licenceScheduleDetailService, licenceCorrectionService, licenceStatusService);
  }

  private static class UnregisteredLicenceTab implements LicenceTab {

    @Override
    public String displayName() {
      return "Unregistered";
    }

    @Override
    public int displayOrder() {
      return 1;
    }

    @Override
    public ReleaseFeature getReleaseFeature() {
      return ReleaseFeature.VIEW_LICENCE_SCHEDULE;
    }

    @Override
    public String url(LicenceTabContext context) {
      return "/unregistered";
    }
  }
}
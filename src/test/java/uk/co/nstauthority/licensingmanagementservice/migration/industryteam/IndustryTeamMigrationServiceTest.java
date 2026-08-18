package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class IndustryTeamMigrationServiceTest {

  private static final int ORGANISATION_GROUP_ID = 700;
  private static final int OTHER_ORGANISATION_GROUP_ID = 800;
  private static final long CONTACT_WUA_ID = 7001L;
  private static final long OTHER_CONTACT_WUA_ID = 7002L;

  private static final ServiceUserDetail INSTIGATING_USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private PearsContactsMigrationExtractRepository pearsContactsMigrationExtractRepository;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private IndustryTeamMigrationService industryTeamMigrationService;

  private Team industryTeam;

  @BeforeEach
  void setUp() {
    industryTeam = industryTeam(ORGANISATION_GROUP_ID);
  }

  @Test
  void migrateIndustryTeamUsers_whenExtractIsEmpty_thenMigratesNoUsers() {
    when(pearsContactsMigrationExtractRepository.findAll()).thenReturn(List.of());

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isZero();
    assertThat(result.skipped()).isZero();

    verifyNoInteractions(teamManagementService, energyPortalUserService, userDetailService);
  }

  @Test
  void migrateIndustryTeamUsers_addsEachContactToTheirIndustryTeamAsTeamAdmin() {
    givenExtractContains(
        extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID),
        extractRow(ORGANISATION_GROUP_ID, OTHER_CONTACT_WUA_ID)
    );
    givenIndustryTeamsExist(industryTeam);
    givenEpaUsersCanBeUsed(CONTACT_WUA_ID, OTHER_CONTACT_WUA_ID);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isEqualTo(2);
    assertThat(result.skipped()).isZero();

    var expectedRoles = List.of(Role.MANAGE_TEAM);
    verify(teamManagementService).setUserTeamRoles(CONTACT_WUA_ID, industryTeam, expectedRoles, INSTIGATING_USER);
    verify(teamManagementService).setUserTeamRoles(OTHER_CONTACT_WUA_ID, industryTeam, expectedRoles, INSTIGATING_USER);
  }

  @Test
  void migrateIndustryTeamUsers_whenTheSameContactIsExtractedTwiceForAGroup_thenAddsThemOnce() {
    givenExtractContains(
        extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID),
        extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID)
    );
    givenIndustryTeamsExist(industryTeam);
    givenEpaUsersCanBeUsed(CONTACT_WUA_ID);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isEqualTo(1);
    // the repeated row is collapsed before migrating, so it is not reported as a skip
    assertThat(result.skipped()).isZero();

    verify(teamManagementService).setUserTeamRoles(eq(CONTACT_WUA_ID), eq(industryTeam), any(), any());
  }

  @Test
  void migrateIndustryTeamUsers_whenOrganisationGroupHasNoIndustryTeam_thenSkipsItsContacts() {
    givenExtractContains(
        extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID),
        extractRow(OTHER_ORGANISATION_GROUP_ID, OTHER_CONTACT_WUA_ID)
    );
    givenIndustryTeamsExist(industryTeam);
    givenEpaUsersCanBeUsed(CONTACT_WUA_ID, OTHER_CONTACT_WUA_ID);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(1);

    verify(teamManagementService).setUserTeamRoles(eq(CONTACT_WUA_ID), eq(industryTeam), any(), any());
    verify(teamManagementService, never()).setUserTeamRoles(eq(OTHER_CONTACT_WUA_ID), any(), any(), any());
  }

  @Test
  void migrateIndustryTeamUsers_whenContactIsAlreadyInTheTeam_thenLeavesTheirRolesAlone() {
    givenExtractContains(extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID));
    givenIndustryTeamsExist(industryTeam);
    givenEpaUsersCanBeUsed(CONTACT_WUA_ID);

    when(teamManagementService.isMemberOfTeam(industryTeam, CONTACT_WUA_ID)).thenReturn(true);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isZero();
    assertThat(result.skipped()).isEqualTo(1);

    verify(teamManagementService, never()).setUserTeamRoles(anyLong(), any(), any(), any());
  }

  @Test
  void migrateIndustryTeamUsers_whenContactHasNoEpaAccount_thenSkipsThatContact() {
    givenExtractContains(
        extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID),
        extractRow(ORGANISATION_GROUP_ID, OTHER_CONTACT_WUA_ID)
    );
    givenIndustryTeamsExist(industryTeam);
    givenEpaUsersCanBeUsed(CONTACT_WUA_ID);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(1);

    verify(teamManagementService).setUserTeamRoles(eq(CONTACT_WUA_ID), eq(industryTeam), any(), any());
    verify(teamManagementService, never()).setUserTeamRoles(eq(OTHER_CONTACT_WUA_ID), any(), any(), any());
  }

  @Test
  void migrateIndustryTeamUsers_whenContactAccountIsNotActive_thenSkipsThatContact() {
    givenExtractContains(extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID));
    givenIndustryTeamsExist(industryTeam);

    when(energyPortalUserService.getEnergyPortalUserMap(any(), any()))
        .thenReturn(Map.of(WebUserAccountId.from(CONTACT_WUA_ID), epaUser(CONTACT_WUA_ID, false, false)));
    when(userDetailService.getUserDetail()).thenReturn(INSTIGATING_USER);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isZero();
    assertThat(result.skipped()).isEqualTo(1);

    verify(teamManagementService, never()).setUserTeamRoles(anyLong(), any(), any(), any());
  }

  @Test
  void migrateIndustryTeamUsers_whenContactIsASharedAccount_thenSkipsThatContact() {
    givenExtractContains(extractRow(ORGANISATION_GROUP_ID, CONTACT_WUA_ID));
    givenIndustryTeamsExist(industryTeam);

    when(energyPortalUserService.getEnergyPortalUserMap(any(), any()))
        .thenReturn(Map.of(WebUserAccountId.from(CONTACT_WUA_ID), epaUser(CONTACT_WUA_ID, true, true)));
    when(userDetailService.getUserDetail()).thenReturn(INSTIGATING_USER);

    var result = industryTeamMigrationService.migrateIndustryTeamUsers();

    assertThat(result.migrated()).isZero();
    assertThat(result.skipped()).isEqualTo(1);

    verify(teamManagementService, never()).setUserTeamRoles(anyLong(), any(), any(), any());
  }

  private void givenExtractContains(PearsContactsMigrationExtract... extracts) {
    when(pearsContactsMigrationExtractRepository.findAll()).thenReturn(List.of(extracts));
  }

  private void givenIndustryTeamsExist(Team... teams) {
    when(teamManagementService.getScopedTeams(
        eq(TeamType.ORGANISATION),
        eq(ScopeType.ORGANISATION_GROUP.name()),
        any()
    )).thenReturn(List.of(teams));
  }

  private void givenEpaUsersCanBeUsed(long... wuaIds) {
    var users = Arrays.stream(wuaIds)
        .boxed()
        .collect(Collectors.toMap(
            WebUserAccountId::from,
            wuaId -> epaUser(wuaId, true, false)
        ));

    when(energyPortalUserService.getEnergyPortalUserMap(any(), any())).thenReturn(users);
    when(userDetailService.getUserDetail()).thenReturn(INSTIGATING_USER);
  }

  private static EnergyPortalUserJson epaUser(long wuaId, boolean canLogin, boolean sharedAccount) {
    return EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(wuaId)
        .canLogin(canLogin)
        .setSharedAccount(sharedAccount)
        .buildJson();
  }

  private static PearsContactsMigrationExtract extractRow(int organisationGroupId, long wuaId) {
    return new PearsContactsMigrationExtract(organisationGroupId, (int) wuaId);
  }

  private static Team industryTeam(int organisationGroupId) {
    var team = new Team();
    team.setName("Group %d".formatted(organisationGroupId));
    team.setTeamType(TeamType.ORGANISATION);
    team.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    team.setScopeId(String.valueOf(organisationGroupId));
    return team;
  }
}

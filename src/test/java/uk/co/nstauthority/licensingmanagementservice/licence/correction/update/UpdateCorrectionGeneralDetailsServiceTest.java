package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ExtendWith(MockitoExtension.class)
class UpdateCorrectionGeneralDetailsServiceTest {

  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private ClearDownWorkAreaLogService clearDownWorkAreaLogService;

  @InjectMocks
  private UpdateCorrectionGeneralDetailsService updateCorrectionGeneralDetailsService;

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"SEAWARD_PRODUCTION", "CARBON_STORAGE"})
  void getAllocatableUsers_returnsCorrectorsForTheLicenceType(LicenceType licenceType) {
    var licence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var expectedRole = LicenceType.CARBON_STORAGE == licenceType
        ? Role.CARBON_STORAGE_LICENCE_CORRECTOR
        : Role.PRODUCTION_LICENCE_CORRECTOR;

    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(expectedRole).build();
    var user = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(100L)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();

    when(teamQueryService.getAllTeamRolesWithRoles(List.of(expectedRole))).thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L)),
        UpdateCorrectionGeneralDetailsService.ALLOCATABLE_USERS_PURPOSE
    )).thenReturn(List.of(user));

    var result = updateCorrectionGeneralDetailsService.getAllocatableUsers(licence);

    assertThat(result).isEqualTo(Map.of("100", "Jane Doe"));
  }

  @Test
  void getAllocatableUsers_whenUserHoldsTheRoleInMoreThanOneTeam_thenLookedUpOnce() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();

    var teamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(100L)
        .withRole(Role.PRODUCTION_LICENCE_CORRECTOR)
        .build();
    var duplicateTeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(100L)
        .withRole(Role.PRODUCTION_LICENCE_CORRECTOR)
        .build();
    var user = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(100L)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();

    when(teamQueryService.getAllTeamRolesWithRoles(List.of(Role.PRODUCTION_LICENCE_CORRECTOR)))
        .thenReturn(List.of(teamRole, duplicateTeamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L)),
        UpdateCorrectionGeneralDetailsService.ALLOCATABLE_USERS_PURPOSE
    )).thenReturn(List.of(user));

    var result = updateCorrectionGeneralDetailsService.getAllocatableUsers(licence);

    assertThat(result).isEqualTo(Map.of("100", "Jane Doe"));
  }

  @Test
  void getAllocatableUsers_ordersUsersByDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();

    var zoeTeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(100L)
        .withRole(Role.PRODUCTION_LICENCE_CORRECTOR)
        .build();
    var alexTeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(200L)
        .withRole(Role.PRODUCTION_LICENCE_CORRECTOR)
        .build();

    var zoe = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(100L)
        .withForename("Zoe")
        .withSurname("Adams")
        .buildJson();
    var alex = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(200L)
        .withForename("Alex")
        .withSurname("Brown")
        .buildJson();

    when(teamQueryService.getAllTeamRolesWithRoles(List.of(Role.PRODUCTION_LICENCE_CORRECTOR)))
        .thenReturn(List.of(zoeTeamRole, alexTeamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L), WebUserAccountId.from(200L)),
        UpdateCorrectionGeneralDetailsService.ALLOCATABLE_USERS_PURPOSE
    )).thenReturn(List.of(zoe, alex));

    var result = updateCorrectionGeneralDetailsService.getAllocatableUsers(licence);

    assertThat(result).containsExactly(
        entry("200", "Alex Brown"),
        entry("100", "Zoe Adams")
    );
  }

  @Test
  void getAllocatableUsers_whenLicenceTypeHasNoCorrectorRole_thenEmpty() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.METHANE_DRAINAGE).build();

    var result = updateCorrectionGeneralDetailsService.getAllocatableUsers(licence);

    assertThat(result).isEmpty();
    verifyNoInteractions(teamQueryService, energyPortalUserService);
  }

  @Test
  void updateGeneralDetails_whenReallocatedToAnotherUser_thenClearsTheNewUsersWorkAreaViewLog() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withAllocatedToWuaId(1L).build();

    updateCorrectionGeneralDetailsService
        .updateGeneralDetails(correction, CORRECTION_REFERENCE, REASON, 42L);

    verify(licenceCorrectionService).updateGeneralDetails(correction, CORRECTION_REFERENCE, REASON, 42L);
    verify(clearDownWorkAreaLogService).clearDownViewFor(
        42L,
        correction.getId(),
        WorkAreaDataItemType.LICENCE_CORRECTION
    );
  }

  @Test
  void updateGeneralDetails_whenAllocatedUserUnchanged_thenWorkAreaViewLogKept() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withAllocatedToWuaId(42L).build();

    updateCorrectionGeneralDetailsService
        .updateGeneralDetails(correction, CORRECTION_REFERENCE, REASON, 42L);

    verify(licenceCorrectionService).updateGeneralDetails(correction, CORRECTION_REFERENCE, REASON, 42L);
    verifyNoInteractions(clearDownWorkAreaLogService);
  }
}
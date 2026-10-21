package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@ExtendWith(MockitoExtension.class)
class RegulatorRoleServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private RegulatorRoleService regulatorRoleService;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Test
  void isRegulator_whenUserIsInRegulatorTeam_returnsTrue() {
    when(teamQueryService.userIsInRegulatorTeam(USER.wuaId())).thenReturn(true);

    assertThat(regulatorRoleService.isRegulator(USER)).isTrue();
  }

  @Test
  void isRegulator_whenUserIsNotInRegulatorTeam_returnsFalse() {
    when(teamQueryService.userIsInRegulatorTeam(USER.wuaId())).thenReturn(false);

    assertThat(regulatorRoleService.isRegulator(USER)).isFalse();
  }

  @Test
  void isContinuationReviewer_whenUserHasOffshoreRole_returnsTrue() {
    var teamRole = new TeamRole();
    teamRole.setRole(Role.CONTINUATION_REVIEWER_OFFSHORE);
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(regulatorRoleService.isContinuationReviewer(USER)).isTrue();
  }

  @Test
  void isContinuationReviewer_whenUserHasOnshoreRole_returnsTrue() {
    var teamRole = new TeamRole();
    teamRole.setRole(Role.CONTINUATION_REVIEWER_ONSHORE);
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(regulatorRoleService.isContinuationReviewer(USER)).isTrue();
  }

  @Test
  void isContinuationReviewer_whenUserHasNoContinuationReviewerRole_returnsFalse() {
    var teamRole = new TeamRole();
    teamRole.setRole(Role.CASE_MANAGER_CARBON_STORAGE);
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(regulatorRoleService.isContinuationReviewer(USER)).isFalse();
  }

  @Test
  void isContinuationIssuer_whenUserHasRole_returnsTrue() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.REGULATIONS_LICENSING, Set.of(Role.CONTINUATION_ISSUER)))
        .thenReturn(true);

    assertThat(regulatorRoleService.isContinuationIssuer(USER)).isTrue();
  }

  @Test
  void isContinuationIssuer_whenUserDoesNotHaveRole_returnsFalse() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.REGULATIONS_LICENSING, Set.of(Role.CONTINUATION_ISSUER)))
        .thenReturn(false);

    assertThat(regulatorRoleService.isContinuationIssuer(USER)).isFalse();
  }

  @Test
  void isLicenceContactsManager_whenUserHasRole_returnsTrue() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.LICENCE_CONTACTS_MANAGER)))
        .thenReturn(true);

    assertThat(regulatorRoleService.isLicenceContactsManager(USER)).isTrue();
  }

  @Test
  void isLicenceContactsManager_whenUserDoesNotHaveRole_returnsFalse() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.LICENCE_CONTACTS_MANAGER)))
        .thenReturn(false);

    assertThat(regulatorRoleService.isLicenceContactsManager(USER)).isFalse();
  }

  @Test
  void isRegulator_withTeamRoles_whenInRegulatorTeam_returnsTrue() {
    var team = TeamTestUtil.newBuilder().withTeamType(TeamType.LICENCE_MANAGEMENT).build();
    var teamRole = TeamRoleTestUtil.newBuilder().withTeam(team).build();

    assertThat(regulatorRoleService.isRegulator(Set.of(teamRole))).isTrue();
  }

  @Test
  void isRegulator_withTeamRoles_whenNotInRegulatorTeam_returnsFalse() {
    var team = TeamTestUtil.newBuilder().withTeamType(TeamType.ORGANISATION).build();
    var teamRole = TeamRoleTestUtil.newBuilder().withTeam(team).build();

    assertThat(regulatorRoleService.isRegulator(Set.of(teamRole))).isFalse();
  }

  @Test
  void isContinuationReviewer_withTeamRoles_whenUserHasRole_returnsTrue() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.CONTINUATION_REVIEWER_OFFSHORE).build();

    assertThat(regulatorRoleService.isContinuationReviewer(Set.of(teamRole))).isTrue();
  }

  @Test
  void isDecisionIssuer_whenUserHasDecisionIssuerRole_returnsTrue() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.DECISION_ISSUER_ONSHORE).build();

    assertThat(regulatorRoleService.isDecisionIssuer(Set.of(teamRole))).isTrue();
  }

  @Test
  void isDecisionIssuer_whenUserDoesNotHaveDecisionIssuerRole_returnsFalse() {
    assertThat(regulatorRoleService.isDecisionIssuer(Set.of())).isFalse();
  }

  @Test
  void isCaseManagerFor_whenUserHasCaseManagerRoleForLicenceType_returnsTrue() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.CASE_MANAGER_ONSHORE).build();

    assertThat(regulatorRoleService.isCaseManagerFor(Set.of(teamRole), LicenceType.LANDWARD_PRODUCTION)).isTrue();
  }

  @Test
  void isCaseManagerFor_whenUserHasCaseManagerRoleForDifferentLicenceType_returnsFalse() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.CASE_MANAGER_ONSHORE).build();

    assertThat(regulatorRoleService.isCaseManagerFor(Set.of(teamRole), LicenceType.SEAWARD_PRODUCTION)).isFalse();
  }

  @Test
  void isCaseManagerFor_whenUserOnlyHasStewardRole_returnsFalse() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.STEWARD_ONSHORE).build();

    assertThat(regulatorRoleService.isCaseManagerFor(Set.of(teamRole), LicenceType.LANDWARD_PRODUCTION)).isFalse();
  }

  @Test
  void isStewardFor_whenUserHasStewardRoleForLicenceType_returnsTrue() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.STEWARD_OFFSHORE).build();

    assertThat(regulatorRoleService.isStewardFor(Set.of(teamRole), LicenceType.SEAWARD_PRODUCTION)).isTrue();
  }

  @Test
  void isStewardFor_whenUserHasStewardRoleForDifferentLicenceType_returnsFalse() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.STEWARD_OFFSHORE).build();

    assertThat(regulatorRoleService.isStewardFor(Set.of(teamRole), LicenceType.CARBON_STORAGE)).isFalse();
  }

  @Test
  void isStewardFor_whenUserOnlyHasCaseManagerRole_returnsFalse() {
    var teamRole = TeamRoleTestUtil.newBuilder().withRole(Role.CASE_MANAGER_OFFSHORE).build();

    assertThat(regulatorRoleService.isStewardFor(Set.of(teamRole), LicenceType.SEAWARD_PRODUCTION)).isFalse();
  }
}

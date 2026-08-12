package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService.CONTINUATION_REVIEWER_ROLES;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;

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
  void isContinuationReviewer_whenUserHasRole_returnsTrue() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.OFFSHORE_PRODUCTION_LICENSING, CONTINUATION_REVIEWER_ROLES))
        .thenReturn(true);

    assertThat(regulatorRoleService.isContinuationReviewer(USER)).isTrue();
  }

  @Test
  void isContinuationReviewer_whenUserDoesNotHaveRole_returnsFalse() {
    when(teamQueryService.userHasAtLeastOneStaticRole(USER.wuaId(), TeamType.OFFSHORE_PRODUCTION_LICENSING, CONTINUATION_REVIEWER_ROLES))
        .thenReturn(false);

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
  void isDecisionIssuer_whenUserHasDecisionIssuerRole_returnsTrue() {
    var teamRole = new TeamRole();
    teamRole.setRole(Role.DECISION_ISSUER_ONSHORE);
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(regulatorRoleService.isDecisionIssuer(USER)).isTrue();
  }

  @Test
  void isDecisionIssuer_whenUserDoesNotHaveDecisionIssuerRole_returnsFalse() {
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of());

    assertThat(regulatorRoleService.isDecisionIssuer(USER)).isFalse();
  }
}

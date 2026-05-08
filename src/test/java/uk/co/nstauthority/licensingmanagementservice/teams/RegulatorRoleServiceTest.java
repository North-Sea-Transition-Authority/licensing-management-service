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
}

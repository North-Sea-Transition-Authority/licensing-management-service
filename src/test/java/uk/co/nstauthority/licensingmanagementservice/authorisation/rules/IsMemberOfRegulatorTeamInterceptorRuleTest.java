package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

class IsMemberOfRegulatorTeamInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private IsMemberOfRegulatorTeamInterceptorRule rule;

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(IsMemberOfRegulatorTeam.class);
  }

  @Test
  void check_whenUserIsInRegulatorTeam_thenContinueAsNormal() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(true);

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenUserIsNotInRegulatorTeam_thenForbidden() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(false);

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "User with wuaId %s is not a member of a regulator team".formatted(user.wuaId())
    ));
  }

  private IsMemberOfRegulatorTeam getAnnotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("isMemberOfRegulatorTeam"),
        IsMemberOfRegulatorTeam.class
    );
  }
}
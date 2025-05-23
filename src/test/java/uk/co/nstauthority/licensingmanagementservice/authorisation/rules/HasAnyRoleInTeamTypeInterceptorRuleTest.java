package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

class HasAnyRoleInTeamTypeInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private HasAnyRoleInTeamTypeInterceptorRule rule;

  ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(1L)
      .build();

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType.class);
  }

  @Test
  void hasCorrectTeamType() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamManagementService.getTeamTypesUserIsMemberOf(serviceUserDetail.wuaId()))
        .thenReturn(Set.of(TeamType.REGULATOR));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasAnyRoleInTeamType"),
        HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType.class
    );
    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);

  }

  @Test
  void hasIncorrectTeamType() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamManagementService.getTeamTypesUserIsMemberOf(serviceUserDetail.wuaId()))
        .thenReturn(Set.of(TeamType.ORGANISATION));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasAnyRoleInTeamType"),
        HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType.class
    );
    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );

  }
}

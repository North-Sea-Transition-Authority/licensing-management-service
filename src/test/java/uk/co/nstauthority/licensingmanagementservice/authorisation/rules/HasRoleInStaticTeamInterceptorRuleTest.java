package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class HasRoleInStaticTeamInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private HasRoleInStaticTeamInterceptorRule rule;

  ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class);
  }

  @Test
  void check_hasCorrectRoles_rulePass() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasAtLeastOneStaticRole(
        1L,
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE))
    ).thenReturn(true);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
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
  void check_hasIncorrectRoles_ruleFail() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasAtLeastOneStaticRole(
        1L,
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE))
    ).thenReturn(false);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
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

  @Test
  void check_hasCorrectInOneOfTwoTeams_rulePass() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasAtLeastOneStaticRole(
        1L,
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE))
    ).thenReturn(true);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam_multipleTeams"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
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
  void check_hasIncorrectInBothTeams_ruleFail() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasAtLeastOneStaticRole(
        1L,
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE))
    ).thenReturn(false);
    when(teamQueryService.userHasAtLeastOneStaticRole(
        1L,
        TeamType.PRODUCTION,
        Set.of(Role.MANAGE_TEAM))
    ).thenReturn(false);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam_multipleTeams"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
    );
    var interceptorResult = rule.check(
        annotation,
        request,
        response
    );

    assertThat(interceptorResult.hasRulePassed()).isFalse();
    verifyNoInteractions(response);
  }

  @Test
  void check_hasScopedTeam_exception() throws NoSuchMethodException {
    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam_scopedTeam"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("The @HasRoleInStaticTeam annotation should not be provided a scoped TeamType")
        .matches(e -> ((ResponseStatusException) e).getStatusCode().is5xxServerError());
  }

  @Test
  void check_hasNoProvidedRoles_exception() throws NoSuchMethodException {
    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("hasRoleInStaticTeam_noProvidedRoles"),
        HasRoleInStaticTeamInterceptorRule.HasRoleInStaticTeam.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("No roles provided to security annotation")
        .matches(e -> ((ResponseStatusException) e).getStatusCode().is5xxServerError());
  }
}

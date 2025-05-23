package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class HasRolesInTeamTypeInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private HasRolesInTeamTypeInterceptorRule rule;

  private final ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(1L)
      .build();

  @Test
  void supports() {
    assertThat(rule.supports())
        .isEqualTo(HasRolesInTeamType.class);
  }

  @Test
  void check_hasCorrectRoles_rulePass() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasRoleWithTeamType"),
        HasRolesInTeamType.class
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
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(false);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasRoleWithTeamType"),
        HasRolesInTeamType.class
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
  void check_hasCorrectInOneOfTwoRoles_rulePass() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION, Role.VIEW_APPLICATION))
    ).thenReturn(true);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasMultipleRolesInTeamType"),
        HasRolesInTeamType.class
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
  void check_hasIncorrectInBothRoles_ruleFail() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION, Role.VIEW_APPLICATION))
    ).thenReturn(false);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasMultipleRolesInTeamType"),
        HasRolesInTeamType.class
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
  void check_hasCorrectRoleInEitherTeam_rulePass() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION, Role.VIEW_APPLICATION))
    ).thenReturn(false);
    when(teamQueryService.userHasRoleInTeamType(
        1L,
        TeamType.REGULATOR,
        Set.of(Role.MANAGE_TEAM))
    ).thenReturn(true);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasMultipleRolesInMultipleTeamTypes"),
        HasRolesInTeamType.class
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
  void check_hasIncorrectRoleInAllTeamTypes_ruleFail() throws NoSuchMethodException {
    when(userDetailService.getUserDetail()).thenReturn(serviceUserDetail);
    when(teamQueryService.userHasRoleInTeamType(
        eq(1L),
        any(TeamType.class),
        anySet())
    ).thenReturn(false);

    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("hasMultipleRolesInTeamType"),
        HasRolesInTeamType.class
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
  void check_whenNoRolesProvided_ruleFail() throws NoSuchMethodException {
    var annotation = getAnnotation(
        RoleAndTeamTypeEndpoints.class.getDeclaredMethod("noRolesProvided"),
        HasRolesInTeamType.class
    );
    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("No role and team type provided to security annotation")
        .matches(e -> ((ResponseStatusException) e).getStatusCode().is5xxServerError());
  }
}

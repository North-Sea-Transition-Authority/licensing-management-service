package uk.co.nstauthority.template.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.template.authentication.UserDetailService;
import uk.co.nstauthority.template.authorisation.SecurityRuleResult;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamQueryService;
import uk.co.nstauthority.template.teams.TeamType;

@Component
@Order(2)
public class HasRoleInStaticTeamInterceptorRule implements AccessInterceptorRule {

  private final TeamQueryService teamQueryService;
  private final UserDetailService userDetailService;

  public HasRoleInStaticTeamInterceptorRule(TeamQueryService teamQueryService,
                                            UserDetailService userDetailService) {
    this.teamQueryService = teamQueryService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return HasRoleInStaticTeam.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var hasRoleInStaticTeam = (HasRoleInStaticTeam) annotation;

    for (TeamRoles teamRoles : hasRoleInStaticTeam.value()) {
      if (teamRoles.teamType().isScoped()) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "The @HasRoleInStaticTeam annotation should not be provided a scoped TeamType"
        );
      }
      if (Set.of(teamRoles.roles()).isEmpty()) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No roles provided to security annotation");
      }
    }

    var wuaId = userDetailService.getUserDetail().wuaId();

    for (TeamRoles teamRoles : hasRoleInStaticTeam.value()) {
      var teamType = teamRoles.teamType();
      var roles = Set.of(teamRoles.roles());
      if (teamQueryService.userHasAtLeastOneStaticRole(wuaId, teamType, roles)) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "User does not have one of the required roles to make this request"
    );
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface HasRoleInStaticTeam {
    TeamRoles[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface TeamRoles {
    TeamType teamType();
    Role[] roles();
  }
}

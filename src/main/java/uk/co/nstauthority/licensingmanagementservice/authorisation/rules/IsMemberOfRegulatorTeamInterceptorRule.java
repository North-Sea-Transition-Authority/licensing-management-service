package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@Component
@Order(1)
public class IsMemberOfRegulatorTeamInterceptorRule implements AccessInterceptorRule {

  private final TeamQueryService teamQueryService;
  private final UserDetailService userDetailService;

  public IsMemberOfRegulatorTeamInterceptorRule(
      TeamQueryService teamQueryService,
      UserDetailService userDetailService
  ) {
    this.teamQueryService = teamQueryService;
    this.userDetailService = userDetailService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return IsMemberOfRegulatorTeam.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var wuaId = userDetailService.getUserDetail().wuaId();

    if (teamQueryService.userIsInRegulatorTeam(wuaId)) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "User with wuaId %s is not a member of a regulator team".formatted(wuaId)
    );
  }
}
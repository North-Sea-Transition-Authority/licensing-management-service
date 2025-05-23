package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Component
@Order(1)
public class HasAnyRoleInTeamTypeInterceptorRule implements AccessInterceptorRule {

  private final TeamManagementService teamManagementService;
  private final UserDetailService userDetailService;

  @Autowired
  public HasAnyRoleInTeamTypeInterceptorRule(
      TeamManagementService teamManagementService,
      UserDetailService userDetailService
  ) {
    this.teamManagementService = teamManagementService;
    this.userDetailService = userDetailService;
  }


  @Override
  public Class<? extends Annotation> supports() {
    return HasAnyRoleInTeamType.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
    var expectedTeamType = ((HasAnyRoleInTeamType) annotation).value();
    var wuaId = userDetailService.getUserDetail().wuaId();

    if (teamManagementService.getTeamTypesUserIsMemberOf(wuaId).contains(expectedTeamType)) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "Current user id %s does not belong to the team type: %s"
            .formatted(wuaId, expectedTeamType)
    );
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface HasAnyRoleInTeamType {
    TeamType value();
  }
}

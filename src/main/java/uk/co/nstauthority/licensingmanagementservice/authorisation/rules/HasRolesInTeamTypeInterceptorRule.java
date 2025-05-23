package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@Component
@Order(4)
public class HasRolesInTeamTypeInterceptorRule implements AccessInterceptorRule {

  private final UserDetailService userDetailService;
  private final TeamQueryService teamQueryService;

  @Autowired
  public HasRolesInTeamTypeInterceptorRule(UserDetailService userDetailService, TeamQueryService teamQueryService) {
    this.userDetailService = userDetailService;
    this.teamQueryService = teamQueryService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return HasRolesInTeamType.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var hasRolesInTeamType = (HasRolesInTeamType) annotation;
    List<RolesAndTeamType> rolesAndTeamTypeList = Arrays.asList(hasRolesInTeamType.value());
    if (rolesAndTeamTypeList.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No role and team type provided to security annotation"
      );
    }

    var user = userDetailService.getUserDetail();

    var userHasRole = rolesAndTeamTypeList.stream()
        .anyMatch(rolesAndTeamType -> teamQueryService.userHasRoleInTeamType(
            user.wuaId(),
            rolesAndTeamType.teamType(),
            Sets.newHashSet(rolesAndTeamType.roles()))
        );

    if (userHasRole) {
      return SecurityRuleResult.continueAsNormal();
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "User with ID %s doesn't have any of the required roles".formatted(user.wuaId()));
  }
}

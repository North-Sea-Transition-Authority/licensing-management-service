package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static java.util.stream.Collectors.toSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Component
@Order(8)
public class InvokingUserHasCorrectorRoleForCorrectionInterceptorRule implements AccessInterceptorRule {

  private final LicenceCorrectionService licenceCorrectionService;
  private final UserDetailService userDetailService;
  private final TeamQueryService teamQueryService;

  public InvokingUserHasCorrectorRoleForCorrectionInterceptorRule(
      LicenceCorrectionService licenceCorrectionService,
      UserDetailService userDetailService,
      TeamQueryService teamQueryService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.userDetailService = userDetailService;
    this.teamQueryService = teamQueryService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return InvokingUserHasCorrectorRoleForCorrection.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var correctionId = getPathVariableEntityIdFromRequest(request, "correctionId");

    var correctionOptional = licenceCorrectionService.findById(correctionId);
    if (correctionOptional.isEmpty()) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND,
          "Licence correction %s not found".formatted(correctionId)
      );
    }
    var correction = correctionOptional.get();

    var user = userDetailService.getUserDetail();
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(toSet());

    if (!LicenceCorrectionRoles.hasCorrectorRole(correction.getLicence().getType(), userRoles)) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.FORBIDDEN,
          "User with wuaId %s does not have the corrector role for licence correction %s"
              .formatted(user.wuaId(), correctionId)
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }
}
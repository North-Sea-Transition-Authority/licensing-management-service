package uk.co.nstauthority.licensingmanagementservice.teams.management.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Component
public class TeamManagementHandlerInterceptor implements HandlerInterceptor {

  private final TeamManagementService teamManagementService;
  private final UserDetailService userDetailService;
  private final TeamQueryService teamQueryService;
  private final ApplicationAccessService applicationAccessService;

  public TeamManagementHandlerInterceptor(TeamManagementService teamManagementService,
                                          UserDetailService userDetailService, TeamQueryService teamQueryService,
                                          ApplicationAccessService applicationAccessService
  ) {
    this.teamManagementService = teamManagementService;
    this.userDetailService = userDetailService;
    this.teamQueryService = teamQueryService;
    this.applicationAccessService = applicationAccessService;
  }

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
    if (handler instanceof ResourceHttpRequestHandler) {
      return true;
    }

    if (handler instanceof HandlerMethod handlerMethod) {
      var wuaId = userDetailService.getUserDetail().wuaId();

      if (hasAnnotation(handlerMethod, InvokingUserCanManageTeam.class)) {
        return handleInvokingUserCanManageTeamCheck(request, wuaId);
      }

      if (hasAnnotation(handlerMethod, InvokingUserHasStaticRole.class)) {
        var annotation = getAnnotation(handlerMethod, InvokingUserHasStaticRole.class);
        return handleInvokingUserHasRoleCheck(wuaId, annotation.teamType(), annotation.role());
      }

      if (hasAnnotation(handlerMethod, InvokingUserCanViewTeam.class)) {
        return handleInvokingUserCanViewTeam(request, wuaId);
      }

      return true;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected handler class %s".formatted(handler.getClass()));
  }

  private boolean handleInvokingUserCanManageTeamCheck(HttpServletRequest request, Long wuaId) {

    var team = getTeamFromRequest(request);

    var isScoped = team.getTeamType().isScoped();
    boolean canManageTeam;
    if (isScoped) {
      canManageTeam = teamManagementService.getScopedTeamsOfTypeUserCanManage(team.getTeamType(), wuaId).contains(team);
    } else {
      canManageTeam = teamManagementService.getStaticTeamOfTypeUserCanManage(team.getTeamType(), wuaId).map(
          t -> t.equals(team)).isPresent();
    }

    if (canManageTeam) {
      return true;
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "wuaId %s does not have manage team role for teamId %s".formatted(wuaId, team.getId()));
    }
  }

  private boolean handleInvokingUserHasRoleCheck(Long wuaId, TeamType teamType, Role role) {
    if (teamQueryService.userHasStaticRole(wuaId, teamType, role)) {
      return true;
    } else {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "wuaId %s does not have static role %s for teamType %s".formatted(wuaId, role, teamType)
      );
    }
  }

  private boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotation) {
    return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), annotation) != null
        || AnnotationUtils.findAnnotation(handlerMethod.getMethod().getDeclaringClass(), annotation) != null;
  }

  private  <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> annotation) {
    return Objects.requireNonNullElse(
        AnnotationUtils.findAnnotation(handlerMethod.getMethod(), annotation),
        AnnotationUtils.findAnnotation(handlerMethod.getMethod().getDeclaringClass(), annotation)
    );
  }

  private boolean handleInvokingUserCanViewTeam(HttpServletRequest request, Long wuaId) {

    var team = getTeamFromRequest(request);

    if (teamManagementService.isMemberOfTeam(team, wuaId) || (TeamType.ORGANISATION.equals(team.getTeamType())
        && teamManagementService.userCanManageAnyOrganisationTeam(wuaId))) {
      return true;
    } else if (team.getTeamType().isApplicationScoped()
               && applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(userDetailService.getUserDetail())) {
      return true;
    } else {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "wuaId %s is not a member of team %s".formatted(wuaId, team.getId())
      );
    }
  }

  @SuppressWarnings("unchecked")
  private Team getTeamFromRequest(HttpServletRequest request) {

    var pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    var teamIdString = pathVariables.get("teamId");

    if (teamIdString == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId path variable not found in request");
    }

    UUID teamId;

    try {
      teamId = UUID.fromString(teamIdString);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID parse error", e);
    }

    return teamManagementService.getTeam(teamId);
  }
}

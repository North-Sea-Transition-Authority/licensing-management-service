package uk.co.nstauthority.licensingmanagementservice.teams.management.access;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class TeamManagementHandlerInterceptorTest {

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private TeamManagementHandlerInterceptor teamManagementHandlerInterceptor;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HandlerMethod handlerMethod;

  private final ServiceUserDetail invokingUser = ServiceUserDetailTestUtil
      .newBuilder()
      .withWuaId(1L)
      .withPersonId(2L)
      .withForename("Test")
      .withSurname("User")
      .withEmailAddress("test@example.com")
      .build();

  @BeforeEach
  void setUp() {
    when(userDetailService.getUserDetail())
        .thenReturn(invokingUser);
  }

  @Test
  void preHandle_invokingUserHasStaticRole() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserHasStaticRole", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Role.MANAGE_TEAM))
        .thenReturn(true);

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .isTrue();
  }

  @Test
  void preHandle_invokingUserHasStaticRole_noAccess() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserHasStaticRole", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(teamQueryService.userHasStaticRole(invokingUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Role.MANAGE_TEAM))
        .thenReturn(false);

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_staticTeam() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", regTeam.getId().toString()));

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.of(regTeam));

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .isTrue();
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_staticTeam_noAccess() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(TeamType.LICENCE_MANAGEMENT);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", regTeam.getId().toString()));

    when(teamManagementService.getTeam(regTeam.getId()))
        .thenReturn(regTeam);

    when(teamManagementService.getStaticTeamOfTypeUserCanManage(regTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_scopedTeam() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var orgTeam = new Team(UUID.randomUUID());
    orgTeam.setTeamType(TeamType.ORGANISATION);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", orgTeam.getId().toString()));

    when(teamManagementService.getTeam(orgTeam.getId()))
        .thenReturn(orgTeam);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(orgTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of(orgTeam));

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .isTrue();
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_scopedTeam_noAccess() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var orgTeam = new Team(UUID.randomUUID());
    orgTeam.setTeamType(TeamType.ORGANISATION);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", orgTeam.getId().toString()));

    when(teamManagementService.getTeam(orgTeam.getId()))
        .thenReturn(orgTeam);

    when(teamManagementService.getScopedTeamsOfTypeUserCanManage(orgTeam.getTeamType(), invokingUser.wuaId()))
        .thenReturn(List.of());

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_noTeamIdInPath() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of());

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void preHandle_InvokingUserCanManageTeam_malformedTeamIdUuid() throws Exception {
    var method = TestController.class.getDeclaredMethod("invokingUserCanManageTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", "not-a-uid"));

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenNoTeamIdInPath_thenBadRequest() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of());

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenMalformedTeamIdUuid_thenBadRequest() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", "not-a-uid"));

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenIsMemberOfTeam_thenOk() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var team = new Team(UUID.randomUUID());

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", team.getId().toString()));

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(true);

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod)).isTrue();
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenNotMemberOfTeam_thenForbidden() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.ORGANISATION);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", team.getId().toString()));

    when(teamManagementService.getTeam(team.getId()))
        .thenReturn(team);

    when(teamManagementService.isMemberOfTeam(team, invokingUser.wuaId()))
        .thenReturn(false);

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenOrganisationTeam_andNotMemberOfTeam_andNotManageAnyOrganisationTeamRole_thenForbidden() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var organisationTeam = new Team(UUID.randomUUID());
    organisationTeam.setTeamType(TeamType.ORGANISATION);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", organisationTeam.getId().toString()));

    when(teamManagementService.getTeam(organisationTeam.getId()))
        .thenReturn(organisationTeam);

    when(teamManagementService.isMemberOfTeam(organisationTeam, invokingUser.wuaId()))
        .thenReturn(false);

    when(teamManagementService.userCanManageAnyOrganisationTeam(invokingUser.wuaId()))
        .thenReturn(false);

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .extracting(ResponseStatusException::getStatusCode)
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void preHandle_invokingUserCanViewTeam_whenOrganisationTeam_andNotMemberOfTeam_andHasManageAnyOrganisationTeamRole_thenOk() throws Exception {

    var method = TestController.class.getDeclaredMethod("invokingUserCanViewTeam", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    var organisationTeam = new Team(UUID.randomUUID());
    organisationTeam.setTeamType(TeamType.ORGANISATION);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("teamId", organisationTeam.getId().toString()));

    when(teamManagementService.getTeam(organisationTeam.getId()))
        .thenReturn(organisationTeam);

    when(teamManagementService.isMemberOfTeam(organisationTeam, invokingUser.wuaId()))
        .thenReturn(false);

    when(teamManagementService.userCanManageAnyOrganisationTeam(invokingUser.wuaId()))
        .thenReturn(true);

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod)).isTrue();
  }

  @Test
  void preHandle_noAnnotation() throws Exception {
    var method = TestController.class.getDeclaredMethod("noAnnotation", UUID.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    assertThat(teamManagementHandlerInterceptor.preHandle(request, response, handlerMethod))
        .isTrue();
  }

  @Controller
  static class TestController {

    @GetMapping("/{teamId}/foo")
    @InvokingUserHasStaticRole(teamType = TeamType.LICENCE_MANAGEMENT, role = Role.MANAGE_TEAM)
    String invokingUserHasStaticRole(@PathVariable UUID teamId) {
      return "ok";
    }

    @GetMapping("/{teamId}/bar")
    @InvokingUserCanManageTeam
    String invokingUserCanManageTeam(@PathVariable UUID teamId) {
      return "ok";
    }

    @GetMapping("/{teamId}/can-view-team")
    @InvokingUserCanViewTeam
    String invokingUserCanViewTeam(@PathVariable UUID teamId) {
      return "ok";
    }

    @GetMapping("/{teamId}/baz")
    String noAnnotation(@PathVariable UUID teamId) {
      return "ok";
    }
  }
}

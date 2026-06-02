package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.AllowedDomainService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.InvokingUserCanManageTeam;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.InvokingUserCanViewTeam;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.AddMemberForm;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.AddMemberFormValidator;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.MemberRolesForm;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.MemberRolesFormValidator;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamTypeView;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamView;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@RestController
@RequestMapping("/team-management")
public class TeamManagementController {

  private final TeamManagementService teamManagementService;
  private final TeamQueryService teamQueryService;
  private final MemberRolesFormValidator memberRolesFormValidator;
  private final AddMemberFormValidator addMemberFormValidator;
  private final EnergyPortalConfiguration energyPortalConfiguration;
  private final EnergyPortalUserService energyPortalUserService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceContinuationService licenceContinuationService;
  private final AllowedDomainService  allowedDomainService;

  public TeamManagementController(
      TeamManagementService teamManagementService,
      TeamQueryService teamQueryService,
      MemberRolesFormValidator memberRolesFormValidator,
      AddMemberFormValidator addMemberFormValidator,
      EnergyPortalConfiguration energyPortalConfiguration,
      EnergyPortalUserService energyPortalUserService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceContinuationService licenceContinuationService, AllowedDomainService allowedDomainService
  ) {
    this.teamManagementService = teamManagementService;
    this.teamQueryService = teamQueryService;
    this.memberRolesFormValidator = memberRolesFormValidator;
    this.addMemberFormValidator = addMemberFormValidator;
    this.energyPortalConfiguration = energyPortalConfiguration;
    this.energyPortalUserService = energyPortalUserService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceContinuationService = licenceContinuationService;
    this.allowedDomainService = allowedDomainService;
  }

  @GetMapping
  public ModelAndView renderTeamTypeList(ServiceUserDetail user) {
    var teamTypes = teamManagementService
        .getTeamTypesUserIsMemberOf(user.wuaId())
        .stream()
        .filter(teamType -> !teamType.isApplicationScoped())
        .collect(Collectors.toCollection(HashSet::new));

    if (teamQueryService.userHasStaticRole(user.wuaId(), TeamType.LICENCE_MANAGEMENT,
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)) {
      // regulator with priv can manage org teams
      teamTypes.add(TeamType.ORGANISATION);
    }

    if (teamTypes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "No manageable teams for wuaId %d".formatted(user.wuaId()));
    }

    var singleTeamType = teamTypes.stream().findFirst();
    if (teamTypes.size() == 1 && singleTeamType.isPresent()) {
      // redirect to manage that team type directly
      return ReverseRouter.redirect(on(TeamManagementController.class)
          .renderTeamsOfType(singleTeamType.get().getUrlSlug(), user));
    }

    var teamTypeViews = teamTypes.stream()
        .map(teamType -> new TeamTypeView(
            teamType.getDisplayName(),
            ReverseRouter.route(on(TeamManagementController.class).renderTeamsOfType(teamType.getUrlSlug(), user)))
        )
        .sorted(Comparator.comparing(teamTypeView -> teamTypeView.teamTypeName().toLowerCase()))
        .toList();

    return new ModelAndView("lms/teamManagement/teamTypes")
        .addObject("teamTypeViews", teamTypeViews);
  }

  @GetMapping("/{teamTypeSlug}")
  public ModelAndView renderTeamsOfType(@PathVariable String teamTypeSlug, ServiceUserDetail user) {
    var teamType = TeamType.fromUrlSlug(teamTypeSlug)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No team type for url slug %s".formatted(teamTypeSlug)));

    if (teamType.isScoped()) {
      // if it's a scoped team, show the list of instances

      boolean userCanCreateOrgs = teamQueryService.userHasStaticRole(
          user.wuaId(),
          TeamType.LICENCE_MANAGEMENT,
          Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM
      );

      Set<Team> teams = new HashSet<>(teamManagementService.getScopedTeamsOfTypeUserIsMemberOf(teamType, user.wuaId()));

      if (teams.isEmpty() && !userCanCreateOrgs) {
        // If user can create orgs, don't error as they need to be able to create new teams.
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No manageable teams of type %s for wuaId %d".formatted(teamType, user.wuaId()));
      }

      var singleTeam = teams.stream().findFirst();
      if (teams.size() == 1 && !userCanCreateOrgs && singleTeam.isPresent()) {
        return ReverseRouter.redirect(on(TeamManagementController.class)
            .renderTeamMemberList(singleTeam.get().getId(), null));
      }

      var teamsViews = teams.stream()
          .map(team -> new TeamView(
              team.getName(),
              ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)))
          )
          .sorted(Comparator.comparing(teamView -> teamView.teamName().toLowerCase()))
          .toList();

      var modelAndView = new ModelAndView("lms/teamManagement/teamInstances")
          .addObject("teamViews", teamsViews);

      if (userCanCreateOrgs) {
        modelAndView.addObject("createNewInstanceUrl", teamType.getCreateNewInstanceRoute());
      }

      return modelAndView;

    } else {
      // if it's a static team, redirect to the single instance
      var team = teamManagementService.getStaticTeamOfTypeUserIsMemberOf(teamType, user.wuaId())
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "No manageable team of type %s for wuaId %d".formatted(teamType, user.wuaId())));

      return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
    }
  }

  @GetMapping("/team/{teamId}")
  @InvokingUserCanViewTeam
  public ModelAndView renderTeamMemberList(@PathVariable UUID teamId, ServiceUserDetail user) {
    return buildTeamListView(teamManagementService.getTeam(teamId), user.wuaId());
  }

  @GetMapping("/team/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView renderAddMemberToTeam(@PathVariable UUID teamId,
                                            @ModelAttribute("form") AddMemberForm form) {
    return buildAddMemberView(teamManagementService.getTeam(teamId));
  }

  @PostMapping("/team/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView handleAddMemberToTeam(@PathVariable UUID teamId,
                                            @ModelAttribute("form") AddMemberForm form,
                                            BindingResult bindingResult) {
    if (!addMemberFormValidator.isValid(form, bindingResult)) {
      return buildAddMemberView(teamManagementService.getTeam(teamId));
    }
    Long wuaId = findUserOrThrow(form.getEmailAddress());
    return ReverseRouter.redirect(on(TeamManagementController.class).renderUserTeamRoles(teamId, wuaId, null));
  }

  @GetMapping("/team/{teamId}/member/{wuaId}/")
  @InvokingUserCanManageTeam
  public ModelAndView renderUserTeamRoles(@PathVariable UUID teamId,
                                          @PathVariable Long wuaId,
                                          @ModelAttribute("form") MemberRolesForm form) {
    return buildEditRolesView(teamManagementService.getTeam(teamId), wuaId, form);
  }

  @PostMapping("/team/{teamId}/member/{wuaId}/")
  @InvokingUserCanManageTeam
  public ModelAndView updateUserTeamRoles(@PathVariable UUID teamId,
                                          @PathVariable Long wuaId,
                                          @ModelAttribute("form") MemberRolesForm form,
                                          BindingResult bindingResult,
                                          ServiceUserDetail userDetail) {
    var team = teamManagementService.getTeam(teamId);
    memberRolesFormValidator.validate(form, wuaId, team, bindingResult);
    if (bindingResult.hasErrors()) {
      return buildEditRolesView(team, wuaId, form);
    }
    updateRoles(wuaId, team, form, userDetail);
    return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
  }

  @GetMapping("/team/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView renderRemoveTeamMember(@PathVariable UUID teamId, @PathVariable Long wuaId) {
    return buildRemoveView(teamManagementService.getTeam(teamId), wuaId);
  }

  @PostMapping("/team/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView handleRemoveTeamMember(@PathVariable UUID teamId, @PathVariable Long wuaId) {
    var team = teamManagementService.getTeam(teamId);
    teamManagementService.removeUserFromTeam(wuaId, team);
    return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
  }

  @GetMapping("/externalContributors/{teamId}")
  @InvokingUserCanViewTeam
  public ModelAndView renderExternalContributorsTeamList(
      @PathVariable UUID teamId,
      ServiceUserDetail user
  ) {
    return buildTeamListView(teamManagementService.getTeam(teamId), user.wuaId());
  }

  @GetMapping("/externalContributors/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView renderAddMemberToScheduleExternalContributorsTeam(
      @PathVariable UUID teamId,
      @ModelAttribute("form") AddMemberForm form
  ) {
    return buildAddMemberView(teamManagementService.getTeam(teamId));
  }

  @PostMapping("/externalContributors/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView handleAddMemberToScheduleExternalContributorsTeam(
      @PathVariable UUID teamId,
      @ModelAttribute("form") AddMemberForm form,
      BindingResult bindingResult
  ) {
    if (!addMemberFormValidator.isValid(form, bindingResult)) {
      return buildAddMemberView(teamManagementService.getTeam(teamId));
    }
    Long wuaId = findUserOrThrow(form.getEmailAddress());
    return ReverseRouter.redirect(on(TeamManagementController.class)
        .renderUserScheduleExternalContributorsTeamRoles(teamId, wuaId, null));
  }

  @GetMapping("/externalContributors/{teamId}/member/{wuaId}/")
  @InvokingUserCanManageTeam
  public ModelAndView renderUserScheduleExternalContributorsTeamRoles(
      @PathVariable UUID teamId,
      @PathVariable Long wuaId,
      @ModelAttribute("form") MemberRolesForm form
  ) {
    return buildEditRolesView(teamManagementService.getTeam(teamId), wuaId, form);
  }

  @PostMapping("/externalContributors/{teamId}/member/{wuaId}/")
  @InvokingUserCanManageTeam
  public ModelAndView updateUserScheduleExternalContributorsTeamRoles(
      @PathVariable UUID teamId,
      @PathVariable Long wuaId,
      @ModelAttribute("form") MemberRolesForm form,
      BindingResult bindingResult,
      ServiceUserDetail userDetail
  ) {
    var team = teamManagementService.getTeam(teamId);
    memberRolesFormValidator.validate(form, wuaId, team, bindingResult);

    if (bindingResult.hasErrors()) {
      return buildEditRolesView(team, wuaId, form);
    }

    updateRoles(wuaId, team, form, userDetail);
    return ReverseRouter.redirect(on(TeamManagementController.class)
        .renderExternalContributorsTeamList(team.getId(), null));
  }

  @GetMapping("/externalContributors/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView renderRemoveScheduleExternalContributorsTeamMember(
      @PathVariable UUID teamId,
      @PathVariable Long wuaId
  ) {
    return buildRemoveView(teamManagementService.getTeam(teamId), wuaId);
  }

  @PostMapping("/externalContributors/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView handleRemoveScheduleExternalContributorsTeamMember(
      @PathVariable UUID teamId,
      @PathVariable Long wuaId
  ) {
    var team = teamManagementService.getTeam(teamId);
    teamManagementService.removeUserFromTeam(wuaId, team);
    return ReverseRouter.redirect(on(TeamManagementController.class)
        .renderExternalContributorsTeamList(team.getId(), null));
  }

  private ModelAndView buildTeamListView(Team team, Long wuaId) {
    var modelAndView = new ModelAndView("lms/teamManagement/teamMembers")
        .addObject("teamName", team.getName())
        .addObject("rolesInTeam", team.getTeamType().getAllowedRoles())
        .addObject("canManageTeam", teamManagementService.canManageTeam(team, wuaId))
        .addObject("backUrl", getBackUrl(team.getScopeId(), team))
        .addObject("teamMemberViews", teamManagementService.getTeamMemberViewsForTeam(team));

    if (team.getTeamType().isApplicationScoped()) {
      modelAndView
          .addObject(
              "currentEndPoint",
              ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                  .getTaskList(null, null, null)))
          .addObject("addMemberUrl",
              ReverseRouter.route(on(TeamManagementController.class)
                  .renderAddMemberToScheduleExternalContributorsTeam(team.getId(), null)));
    } else {
      modelAndView
          .addObject("addMemberUrl",
              ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToTeam(team.getId(), null)));
    }

    return modelAndView;
  }

  private ModelAndView buildAddMemberView(Team team) {
    ModelAndView modelAndView = new ModelAndView("lms/teamManagement/addMember")
        .addObject("registerUrl", energyPortalConfiguration.registrationUrl());

    return addNavigationAttributes(team, modelAndView);
  }

  private ModelAndView buildEditRolesView(Team team, Long wuaId, MemberRolesForm form) {
    TeamMemberView teamMemberView;

    teamMemberView = teamManagementService.getTeamMemberView(team, wuaId);

    if (form.getRoles() == null || form.getRoles().isEmpty()) {
      form.setRoles(teamMemberView.roles().stream().map(Role::name).toList());
    }

    var availableRoles = team.getTeamType().getAllowedRoles();
    Map<String, String> rolesNamesMap = availableRoles.stream()
                                                      .collect(StreamUtil.toLinkedHashMap(Enum::name, Role::getName));

    boolean userHasAllowedEmail = allowedDomainService.isAllowedDomain(teamMemberView.email(), team);

    ModelAndView modelAndView = new ModelAndView("lms/teamManagement/editMemberRoles")
        .addObject("rolesNamesMap", rolesNamesMap)
        .addObject("rolesInTeam", availableRoles)
        .addObject("teamMemberView", teamMemberView)
        .addObject("userHasAllowedEmail", userHasAllowedEmail);

    return addNavigationAttributes(team, modelAndView);
  }

  private ModelAndView buildRemoveView(Team team, Long wuaId) {
    boolean canRemoveTeamMember = teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(team, wuaId);

    ModelAndView modelAndView = new ModelAndView("lms/teamManagement/removeMember")
        .addObject("teamName", team.getName())
        .addObject("canRemoveTeamMember", canRemoveTeamMember)
        .addObject("teamMemberView", teamManagementService.getTeamMemberView(team, wuaId));

    return addNavigationAttributes(team, modelAndView);
  }

  @NotNull
  private ModelAndView addNavigationAttributes(Team team, ModelAndView modelAndView) {

    if (team.getTeamType().isApplicationScoped()) {
      modelAndView
          .addObject(
              "currentEndPoint",
              ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(null, null, null)))
          .addObject(
              "cancelUrl",
              ReverseRouter.route(on(TeamManagementController.class)
                  .renderExternalContributorsTeamList(team.getId(), null)));
    } else {
      modelAndView
          .addObject(
              "cancelUrl",
              ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null)));
    }
    return modelAndView;
  }

  private String getBackUrl(String scopeId, Team team) {
    if (!team.getTeamType().isApplicationScoped()) {
      return ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));
    }

    if (team.getScopeType().equals(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())) {
      var scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplicationService
          .getScheduleWorkProgrammeApplicationById(UUID.fromString(scopeId));

      var scheduleWorkProgrammeApplicationDetail = scheduleWorkProgrammeApplicationService
          .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(scheduleWorkProgrammeApplication);

      return ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
          scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    } else {
      var detailByIdOrThrow = licenceContinuationService
          .getDetailByIdOrThrow(UUID.fromString(scopeId));

      return ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
          detailByIdOrThrow.getId(), null, null));
    }
  }

  private Long findUserOrThrow(String email) {
    return energyPortalUserService.findUsersByEmail(email, "Find user to add to team").stream()
                                  .filter(user -> !user.sharedAccount() && user.canLogin())
                                  .map(EnergyPortalUserJson::webUserAccountId)
                                  .findFirst()
                                  .orElseThrow(() -> new ResponseStatusException(
                                      HttpStatus.BAD_REQUEST,
                                      "user with email address %s does not exist".formatted(email))
                                  );
  }

  private void updateRoles(Long wuaId, Team team, MemberRolesForm form, ServiceUserDetail userDetail) {
    var roles = form.getRoles().stream()
                    .map(Role::valueOf)
                    .toList();
    teamManagementService.setUserTeamRoles(wuaId, team, roles, userDetail);
  }
}
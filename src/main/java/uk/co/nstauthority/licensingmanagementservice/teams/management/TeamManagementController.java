package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
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
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamTypeView;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamView;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@RestController
@RequestMapping("/team-management")
public class TeamManagementController {

  private final TeamManagementService teamManagementService;
  private final TeamQueryService teamQueryService;
  private final MemberRolesFormValidator memberRolesFormValidator;
  private final AddMemberFormValidator addMemberFormValidator;
  private final EnergyPortalConfiguration energyPortalConfiguration;
  private final EnergyPortalUserService energyPortalUserService;
  private static final String CANCEL_URL_ATTRIBUTE_NAME = "cancelUrl";

  public TeamManagementController(TeamManagementService teamManagementService, TeamQueryService teamQueryService,
                                  MemberRolesFormValidator memberRolesFormValidator,
                                  AddMemberFormValidator addMemberFormValidator,
                                  EnergyPortalConfiguration energyPortalConfiguration,
                                  EnergyPortalUserService energyPortalUserService) {
    this.teamManagementService = teamManagementService;
    this.teamQueryService = teamQueryService;
    this.memberRolesFormValidator = memberRolesFormValidator;
    this.addMemberFormValidator = addMemberFormValidator;
    this.energyPortalConfiguration = energyPortalConfiguration;
    this.energyPortalUserService = energyPortalUserService;
  }

  @GetMapping
  public ModelAndView renderTeamTypeList(ServiceUserDetail user) {

    var teamTypes = new HashSet<>(teamManagementService.getTeamTypesUserIsMemberOf(user.wuaId()));

    if (teamQueryService.userHasStaticRole(user.wuaId(), TeamType.REGULATOR,
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
          TeamType.REGULATOR,
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

    var team = teamManagementService.getTeam(teamId);
    var teamMemberViews = teamManagementService.getTeamMemberViewsForTeam(team);

    return new ModelAndView("lms/teamManagement/teamMembers")
        .addObject("teamName", team.getName())
        .addObject("teamMemberViews", teamMemberViews)
        .addObject("rolesInTeam", team.getTeamType().getAllowedRoles())
        .addObject("canManageTeam", teamManagementService.canManageTeam(team, user.wuaId()))
        .addObject(
            "addMemberUrl",
            ReverseRouter.route(on(TeamManagementController.class).renderAddMemberToTeam(team.getId(), null))
        );
  }

  @GetMapping("/team/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView renderAddMemberToTeam(@PathVariable UUID teamId,
                                            @ModelAttribute("form") AddMemberForm form) {
    var team = teamManagementService.getTeam(teamId);
    return new ModelAndView("lms/teamManagement/addMember")
        .addObject(
            CANCEL_URL_ATTRIBUTE_NAME,
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null))
        )
        .addObject("registerUrl", energyPortalConfiguration.registrationUrl());
  }

  @PostMapping("/team/{teamId}/add-member")
  @InvokingUserCanManageTeam
  public ModelAndView handleAddMemberToTeam(@PathVariable UUID teamId,
                                            @ModelAttribute("form") AddMemberForm form,
                                            BindingResult bindingResult) {
    if (!addMemberFormValidator.isValid(form, bindingResult)) {
      return new ModelAndView("lms/teamManagement/addMember")
          .addObject(
              CANCEL_URL_ATTRIBUTE_NAME,
              ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(teamId, null))
          )
          .addObject("registerUrl", energyPortalConfiguration.registrationUrl());
    }

    var wuaId = energyPortalUserService.findUsersByEmail(form.getEmailAddress(), "Find user to add to team").stream()
        .filter(user -> !user.sharedAccount() && user.canLogin())
        .map(EnergyPortalUserJson::webUserAccountId)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "user with email address %s does not exist".formatted(form.getEmailAddress()))
        );


    return ReverseRouter.redirect(on(TeamManagementController.class).renderUserTeamRoles(teamId, wuaId, null));
  }

  @GetMapping("/team/{teamId}/member/{wuaId}/")
  @InvokingUserCanManageTeam
  public ModelAndView renderUserTeamRoles(@PathVariable UUID teamId,
                                          @PathVariable Long wuaId,
                                          @ModelAttribute("form") MemberRolesForm form) {
    var team = teamManagementService.getTeam(teamId);
    var teamMemberView = teamManagementService.getTeamMemberView(team, wuaId);

    form.setRoles(teamMemberView.roles().stream().map(Role::name).toList());

    return getUserTeamRolesModelAndView(team, wuaId);
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
      return getUserTeamRolesModelAndView(team, wuaId);
    }

    var roles = form.getRoles().stream()
        .map(Role::valueOf)
        .toList();

    teamManagementService.setUserTeamRoles(wuaId, team, roles, userDetail);
    return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
  }

  @GetMapping("/team/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView renderRemoveTeamMember(@PathVariable UUID teamId, @PathVariable Long wuaId) {
    var team = teamManagementService.getTeam(teamId);

    var teamMemberView = teamManagementService.getTeamMemberView(team, wuaId);
    var canRemoveTeamMember = teamManagementService.willManageTeamRoleBePresentAfterMemberRemoval(team, wuaId);

    return new ModelAndView("lms/teamManagement/removeMember")
        .addObject("teamMemberView", teamMemberView)
        .addObject("teamName", team.getName())
        .addObject("canRemoveTeamMember", canRemoveTeamMember)
        .addObject(
            CANCEL_URL_ATTRIBUTE_NAME,
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null))
        );
  }

  @PostMapping("/team/{teamId}/member/{wuaId}/remove")
  @InvokingUserCanManageTeam
  public ModelAndView handleRemoveTeamMember(@PathVariable UUID teamId,
                                             @PathVariable Long wuaId,
                                             ServiceUserDetail userDetail) {
    var team = teamManagementService.getTeam(teamId);
    teamManagementService.removeUserFromTeam(wuaId, team, userDetail);
    return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
  }

  private ModelAndView getUserTeamRolesModelAndView(Team team, Long wuaId) {
    var teamMemberView = teamManagementService.getTeamMemberView(team, wuaId);
    var availableRoles = team.getTeamType().getAllowedRoles();

    Map<String, String> rolesNamesMap = availableRoles.stream()
        .collect(StreamUtil.toLinkedHashMap(Enum::name, Role::getName));

    return new ModelAndView("lms/teamManagement/editMemberRoles")
        .addObject("rolesNamesMap", rolesNamesMap)
        .addObject("rolesInTeam", availableRoles)
        .addObject("teamMemberView", teamMemberView)
        .addObject(
            CANCEL_URL_ATTRIBUTE_NAME,
            ReverseRouter.route(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null))
        );
  }
}

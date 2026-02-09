package uk.co.nstauthority.licensingmanagementservice.teams.management;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalServiceProviderTeamService;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.client.OrganisationGroupProjectionRoot;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.InvokingUserHasStaticRole;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.NewOrganisationTeamForm;
import uk.co.nstauthority.licensingmanagementservice.teams.management.form.NewOrganisationTeamFormValidator;

@RestController
public class ScopedTeamManagementController {

  private final TeamManagementService teamManagementService;
  private final OrganisationApi organisationApi;
  private final NewOrganisationTeamFormValidator newOrganisationTeamFormValidator;
  private final EnergyPortalServiceProviderTeamService energyPortalServiceProviderTeamService;

  public ScopedTeamManagementController(TeamManagementService teamManagementService,
                                        OrganisationApi organisationApi,
                                        NewOrganisationTeamFormValidator newOrganisationTeamFormValidator,
                                        EnergyPortalServiceProviderTeamService energyPortalServiceProviderTeamService) {
    this.teamManagementService = teamManagementService;
    this.organisationApi = organisationApi;
    this.newOrganisationTeamFormValidator = newOrganisationTeamFormValidator;
    this.energyPortalServiceProviderTeamService = energyPortalServiceProviderTeamService;
  }

  // Add one of these get/post handlers for every scoped team time you want users to be able to create themselves.
  // only this creation logic needs to be added, once team is created normal TeamManagementController can be used.
  @GetMapping("/team-management/organisation/new")
  @InvokingUserHasStaticRole(teamType = TeamType.LICENCE_MANAGEMENT, role = Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)
  public ModelAndView renderCreateNewOrgTeam(@ModelAttribute("form") NewOrganisationTeamForm form) {
    return getModelAndView();
  }

  @PostMapping("/team-management/organisation/new")
  @InvokingUserHasStaticRole(teamType = TeamType.LICENCE_MANAGEMENT, role = Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)
  public ModelAndView handleCreateNewOrgTeam(@ModelAttribute("form") NewOrganisationTeamForm form, BindingResult bindingResult) {
    if (!newOrganisationTeamFormValidator.validate(form, bindingResult)) {
      return getModelAndView();
    }

    var projection = new OrganisationGroupProjectionRoot()
        .organisationGroupId()
        .name();

    var organisationGroup = organisationApi.findOrganisationGroup(
          Integer.parseInt(form.getOrgGroupId()),
          projection,
          new RequestPurpose("Find org group to create team"))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Org group with id %s not found".formatted(form.getOrgGroupId())
        ));

    var scopeRef = TeamScopeReference.from(
        organisationGroup.getOrganisationGroupId().toString(),
        ScopeType.ORGANISATION_GROUP.name()
    );
    var team = teamManagementService.createScopedTeam(organisationGroup.getName(), TeamType.ORGANISATION, scopeRef);

    var serviceProviderTeam = new ServiceProviderTeamDto(
        team.getId().toString(),
        team.getScopeId(),
        ScopeType.ORGANISATION_GROUP,
        team.getTeamType().name()
    );
    energyPortalServiceProviderTeamService.publishTeam(serviceProviderTeam);

    return ReverseRouter.redirect(on(TeamManagementController.class).renderTeamMemberList(team.getId(), null));
  }

  private ModelAndView getModelAndView() {
    return new ModelAndView("lms/teamManagement/scoped/createOrganisationTeam")
        .addObject(
            "organisationSearchUrl",
            SearchSelectorService.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)));
  }
}

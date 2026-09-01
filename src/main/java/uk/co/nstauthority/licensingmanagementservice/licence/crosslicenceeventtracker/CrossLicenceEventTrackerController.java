package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("event-tracker")
public class CrossLicenceEventTrackerController {

  private final CrossLicenceEventTrackerService crossLicenceEventTrackerService;
  private final RegulatorRoleService regulatorRoleService;

  public CrossLicenceEventTrackerController(
      CrossLicenceEventTrackerService crossLicenceEventTrackerService,
      RegulatorRoleService regulatorRoleService
  ) {
    this.crossLicenceEventTrackerService = crossLicenceEventTrackerService;
    this.regulatorRoleService = regulatorRoleService;
  }

  @GetMapping
  public ModelAndView renderEventTracker(ServiceUserDetail user) {
    return new ModelAndView("lms/licence/crosslicenceeventtracker/eventTracker")
        .addObject("eventTrackerTableJson", crossLicenceEventTrackerService.getEventTrackerTable().toString())
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("isRegulatorUser", regulatorRoleService.isRegulator(user))
        .addObject("licenseeGroupOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
        .addObject("preSelectedLicenseeGroupOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class));
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("event-tracker")
@SessionAttributes("eventTrackerFilterSession")
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
  public ModelAndView renderEventTracker(
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var form = filterSession.getFilterForm();

    return new ModelAndView("lms/licence/crosslicenceeventtracker/eventTracker")
        .addObject("eventTrackerTableJson", crossLicenceEventTrackerService.getEventTrackerTable(form).toString())
        .addObject("form", form)
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("isRegulatorUser", regulatorRoleService.isRegulator(user))
        .addObject("licenseeGroupOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
        .addObject("preSelectedLicenseeGroupOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class))
        .addObject("clearFilterUrl",
            ReverseRouter.route(on(CrossLicenceEventTrackerController.class).clearEventTrackerFilters(null, null)));
  }

  @PostMapping
  public ModelAndView filterEventTracker(
      @ModelAttribute("form") EventTrackerForm form,
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(CrossLicenceEventTrackerController.class).renderEventTracker(null, null));
  }

  @GetMapping("clear-filters")
  public ModelAndView clearEventTrackerFilters(
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(CrossLicenceEventTrackerController.class).renderEventTracker(null, null));
  }

  @ModelAttribute("eventTrackerFilterSession")
  private EventTrackerFilterSession getFilterSession() {
    return new EventTrackerFilterSession(new EventTrackerForm());
  }
}

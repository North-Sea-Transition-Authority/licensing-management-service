package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
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
  private final EventTrackerFormValidator eventTrackerFormValidator;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public CrossLicenceEventTrackerController(
      CrossLicenceEventTrackerService crossLicenceEventTrackerService,
      RegulatorRoleService regulatorRoleService,
      EventTrackerFormValidator eventTrackerFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService
  ) {
    this.crossLicenceEventTrackerService = crossLicenceEventTrackerService;
    this.regulatorRoleService = regulatorRoleService;
    this.eventTrackerFormValidator = eventTrackerFormValidator;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  @GetMapping
  public ModelAndView renderEventTracker(
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession,
      ServiceUserDetail user
  ) {
    return eventTrackerModelAndView(filterSession.getFilterForm(), user);
  }

  @PostMapping
  public ModelAndView filterEventTracker(
      @ModelAttribute("form") EventTrackerForm form,
      BindingResult bindingResult,
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession,
      ServiceUserDetail user
  ) {
    eventTrackerFormValidator.isValid(form, bindingResult);

    if (bindingResult.hasErrors()) {
      return eventTrackerModelAndView(form, user);
    }

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

  private ModelAndView eventTrackerModelAndView(EventTrackerForm form, ServiceUserDetail user) {
    var isRegulator = regulatorRoleService.isRegulator(user);

    return new ModelAndView("lms/licence/crosslicenceeventtracker/eventTracker")
        .addObject("eventTrackerTableJson", crossLicenceEventTrackerService.getEventTrackerTable(form, user).toString())
        .addObject("form", form)
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", organisationUnitQueryService.getOrganisationUnitSelectOption(
            form.getLicenseeOrgUnitId() == null ? null : form.getLicenseeOrgUnitId().toString()))
        .addObject("isRegulatorUser", isRegulator)
        .addObject("licenseeGroupOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
        .addObject("preSelectedLicenseeOrgGroup",
            organisationGroupQueryService.getOrganisationGroupSelectOption(form.getLicenseeOrgGroupId()))
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", isRegulator
            ? DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class)
            : EventTrackerApplicationStatus.getIndustryStatuses())
        .addObject("clearFilterUrl",
            ReverseRouter.route(on(CrossLicenceEventTrackerController.class).clearEventTrackerFilters(null, null)));
  }
}

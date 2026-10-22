package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Clock;
import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("event-tracker")
@SessionAttributes("eventTrackerFilterSession")
public class CrossLicenceEventTrackerController {

  private static final int DEFAULT_FILTER_YEARS = 2;

  private final CrossLicenceEventTrackerService crossLicenceEventTrackerService;
  private final RegulatorRoleService regulatorRoleService;
  private final EventTrackerFormValidator eventTrackerFormValidator;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final Clock clock;

  public CrossLicenceEventTrackerController(
      CrossLicenceEventTrackerService crossLicenceEventTrackerService,
      RegulatorRoleService regulatorRoleService,
      EventTrackerFormValidator eventTrackerFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService,
      Clock clock
  ) {
    this.crossLicenceEventTrackerService = crossLicenceEventTrackerService;
    this.regulatorRoleService = regulatorRoleService;
    this.eventTrackerFormValidator = eventTrackerFormValidator;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.clock = clock;
  }

  @GetMapping
  public ModelAndView renderEventTracker(
      @ModelAttribute("eventTrackerFilterSession") EventTrackerFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var modelAndView = eventTrackerModelAndView(filterSession.getFilterForm(), user);

    if (filterSession.isDefaultFiltersApplied()) {
      NotificationBanner.newInfoBanner()
          .withHeadingContent("Showing events due in the next %d years".formatted(DEFAULT_FILTER_YEARS))
          .withOtherContent(
              "Only events from %s to %s are shown. Change the event dates in the filters to see other events."
                  .formatted(filterSession.getFilterForm().getFromDate(), filterSession.getFilterForm().getToDate())
          )
          .applyTo(modelAndView);
    }

    return modelAndView;
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
  public EventTrackerFilterSession getFilterSession() {
    var today = LocalDate.now(clock);
    var defaultForm = new EventTrackerForm();
    defaultForm.setFromDate(today.format(DateUtil.STRICT_DATE_FORMAT));
    defaultForm.setToDate(today.plusYears(DEFAULT_FILTER_YEARS).format(DateUtil.STRICT_DATE_FORMAT));
    return new EventTrackerFilterSession(defaultForm, true);
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

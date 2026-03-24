package uk.co.nstauthority.licensingmanagementservice.mockups.eventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/mockups/event-tracker")
@Profile("internal-only")
public class EventTrackerController {

  @GetMapping("/multi-table-design")
  ModelAndView renderMultiTablePage() {
    return new ModelAndView("lms/mockups/eventtracker/eventTrackerMultiTableDesign")
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class));
  }

  @GetMapping("/single-table-design-regulator")
  ModelAndView renderSingleTableRegulatorPage() {
    return new ModelAndView("lms/mockups/eventtracker/eventTrackerSingleTableRegulatorDesign")
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerEventStatus.class));
  }

  @GetMapping("/single-table-design-industry")
  ModelAndView renderSingleTableIndustryPage() {
    return new ModelAndView("lms/mockups/eventtracker/eventTrackerSingleTableIndustryDesign")
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", EventTrackerEventStatus.getIndustryStatuses());
  }

  @GetMapping("/accordion-design")
  ModelAndView renderAccordionPage() {
    return new ModelAndView("lms/mockups/eventtracker/eventTrackerAccordionDesign")
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class));
  }

}

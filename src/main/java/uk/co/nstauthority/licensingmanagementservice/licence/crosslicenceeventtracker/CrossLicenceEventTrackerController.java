package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
public class CrossLicenceEventTrackerController {

  @GetMapping
  public ModelAndView renderEventTracker() {
    return new ModelAndView("lms/licence/crosslicenceeventtracker/eventTracker")
        .addObject("form", new EventTrackerForm())
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", Collections.emptyMap())
        .addObject("requestTypes", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class))
        .addObject("eventStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class));
  }
}

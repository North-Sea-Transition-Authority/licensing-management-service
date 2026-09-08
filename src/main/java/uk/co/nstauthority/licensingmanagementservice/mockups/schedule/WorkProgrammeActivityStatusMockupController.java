package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivitySummaryView;

/**
 * Mockup of the work programme activity status screen in which the "Transferred" status is replaced by "Alternative
 * work programme". Selecting that status reveals an FDS add to list of the licences the alternative work programme will
 * be delivered on, plus a comment explaining why. Backed by hardcoded activity data so nothing is persisted.
 */
@Controller
@RequestMapping(WorkProgrammeActivityStatusMockupController.PAGE_URL)
@Profile("mockups")
public class WorkProgrammeActivityStatusMockupController {

  public static final String PAGE_URL = "/mockups/work-programme-activity-status";

  private static final String PAGE_TITLE = "Update the work programme activity status";

  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;

  private static final String LICENCE_REFERENCE = "P2500";

  /**
   * Matches the format {@code LicenceService.getLicencePageCaption} produces for the real screen.
   */
  private static final String PAGE_CAPTION = LICENCE_TYPE.getDisplayName() + " - " + LICENCE_REFERENCE;

  private static final WorkProgrammeActivitySummaryView SUMMARY_VIEW = new WorkProgrammeActivitySummaryView(
      "Well",
      "Drill one exploration well to a minimum depth of 3,000m below mean sea level",
      "Firm",
      "27 July 2027"
  );

  /**
   * Licences already added to the alternative work programme, so the add to list renders populated rather than showing
   * only its empty state.
   */
  private static final List<MockAddToListLicence> ALTERNATIVE_WORK_PROGRAMME_LICENCES = List.of(
      new MockAddToListLicence("1", "P2411"),
      new MockAddToListLicence("2", "P2498")
  );

  @GetMapping
  ModelAndView renderStatusUpdateMockup() {
    return getStatusUpdateModelAndView(new WorkProgrammeActivityStatusMockupForm());
  }

  /**
   * Renders the page again rather than saving, so the mockup can be submitted without leaving the screen.
   */
  @PostMapping
  ModelAndView submitStatusUpdateMockup(@ModelAttribute("form") WorkProgrammeActivityStatusMockupForm form) {
    return new ModelAndView(new RedirectView("/lms" + PAGE_URL));
  }

  private ModelAndView getStatusUpdateModelAndView(WorkProgrammeActivityStatusMockupForm form) {
    return new ModelAndView("lms/mockups/schedule/updateWorkProgrammeActivityStatus")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", PAGE_CAPTION)
        .addObject("summaryView", SUMMARY_VIEW)
        .addObject("statusRadioOptions", MockWorkProgrammeStatus.getRadioOptions())
        .addObject("alternativeWorkProgrammeLicences", ALTERNATIVE_WORK_PROGRAMME_LICENCES)
        .addObject("licenceSearchUrl", SearchSelectorService.route(on(LicenceInternalApiRestController.class)
            .searchLicencesByReferenceAndType(LICENCE_TYPE.getUrlSlug(), null))
        )
        .addObject("cancelUrl", PAGE_URL);
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleTimelineFilterForm;

/**
 * Renders the licence schedule timeline with hardcoded data covering an initial, second and third term. Uses the real
 * schedule timeline macros so the page is a faithful copy of the production screen.
 */
@Controller
@RequestMapping(LicenceScheduleMockupController.PAGE_URL)
@Profile("mockups")
public class LicenceScheduleMockupController {

  public static final String PAGE_URL = "/mockups/licence-schedule";

  private static final String PAGE_TITLE = "P2500 - Licence schedule and work programme";

  @GetMapping
  ModelAndView renderLicenceScheduleMockup() {
    var form = new ScheduleTimelineFilterForm();
    form.clearFilter();

    return new ModelAndView("lms/mockups/schedule/licenceScheduleTimeline")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("timelineSummaryCardView", LicenceScheduleMockupData.getSummaryCardView())
        .addObject("scheduleComments", LicenceScheduleMockupData.getScheduleComments())
        .addObject("actions", LicenceScheduleMockupData.getActions())
        .addObject("scheduleEventViews", LicenceScheduleMockupData.getTermViews())
        .addObject("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions())
        .addObject("updateLicenceStartDateUrl", PAGE_URL)
        .addObject("updateExpiryDateUrl", PAGE_URL)
        .addObject("clearFilterUrl", PAGE_URL)
        .addObject("reviewAndApplyUrl", PAGE_URL)
        .addObject("deleteScheduleUrl", PAGE_URL);
  }
}

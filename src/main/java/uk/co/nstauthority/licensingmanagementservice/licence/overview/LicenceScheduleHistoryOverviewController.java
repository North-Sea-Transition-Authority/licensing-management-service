package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleTimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleTimelineSession;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}/overview")
@SessionAttributes("scheduleTimelineSession")
public class LicenceScheduleHistoryOverviewController {

  private final LicenceScheduleTab licenceScheduleTab;
  private final TabbedLicencePageService tabbedLicencePageService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTimelineService licenceScheduleTimelineService;
  private final LicenceScheduleOverviewService licenceScheduleOverviewService;

  public LicenceScheduleHistoryOverviewController(
      LicenceScheduleTab licenceScheduleTab,
      TabbedLicencePageService tabbedLicencePageService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTimelineService licenceScheduleTimelineService,
      LicenceScheduleOverviewService licenceScheduleOverviewService
  ) {
    this.licenceScheduleTab = licenceScheduleTab;
    this.tabbedLicencePageService = tabbedLicencePageService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
    this.licenceScheduleOverviewService = licenceScheduleOverviewService;
  }

  @GetMapping
  public ModelAndView renderLicenceOverview(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession,
      ServiceUserDetail user
  ) {
    var form = filterSession.getTimelineFilterForm();

    if (!filterSession.hasFilterBeenInvoked()) {
      form.clearFilter();
    }

    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    var modelAndView = new ModelAndView("lms/licence/schedule/licencetab/licenceScheduleTab");
    tabbedLicencePageService.hydrateModel(modelAndView, licence, licenceScheduleTab, user);

    return modelAndView
        .addObject("form", form)
        .addObject("scheduleExists", true)
        .addObject("historyForm", licenceScheduleOverviewService.getScheduleHistoryForm(licenceScheduleDetail))
        .addObject("scheduleHistoryOptions", licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence))
        .addObject("viewScheduleHistoryUrl", ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
            .viewScheduleHistory(licenceScheduleDetailId, null))
        )
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions())
        .addObject("scheduleEventViews",
            licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(licenceScheduleDetail, form, user)
        )
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
            .clearFilters(licenceScheduleDetailId, null, null))
        );
  }

  @PostMapping
  public ModelAndView filterTimeline(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("form") ScheduleTimelineFilterForm form,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(licenceScheduleDetailId, null, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearFilters(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(licenceScheduleDetailId, null, null, null));
  }

  @PostMapping("/schedule-history")
  public ModelAndView viewScheduleHistory(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("historyForm") LicenceScheduleHistoryForm form
  ) {
    if (!StringUtils.hasText(form.getLicenceScheduleDetailId())) {
      return ReverseRouter.redirect(on(LicenceScheduleHistoryOverviewController.class)
          .renderLicenceOverview(licenceScheduleDetailId, null, null, null));
    }

    return ReverseRouter.redirect(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(UUID.fromString(form.getLicenceScheduleDetailId()), null, null, null));
  }

  @ModelAttribute("scheduleTimelineSession")
  private ScheduleTimelineSession getFilterSession(
      @ModelAttribute("form") ScheduleTimelineFilterForm form
  ) {
    return new ScheduleTimelineSession(form);
  }
}

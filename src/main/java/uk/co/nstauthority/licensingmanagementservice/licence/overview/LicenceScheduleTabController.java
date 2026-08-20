package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleTimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleTimelineSession;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/schedule")
@SessionAttributes("scheduleTimelineSession")
public class LicenceScheduleTabController {

  private final LicenceScheduleTab licenceScheduleTab;
  private final TabbedLicencePageService tabbedLicencePageService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTimelineService licenceScheduleTimelineService;

  LicenceScheduleTabController(
      LicenceScheduleTab licenceScheduleTab,
      TabbedLicencePageService tabbedLicencePageService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTimelineService licenceScheduleTimelineService
  ) {
    this.licenceScheduleTab = licenceScheduleTab;
    this.tabbedLicencePageService = tabbedLicencePageService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
  }

  @GetMapping
  public ModelAndView renderLicenceOverview(
      @PathVariable Integer licenceId,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession,
      Licence licence,
      ServiceUserDetail user
  ) {
    var form = filterSession.getTimelineFilterForm();

    if (!filterSession.hasFilterBeenInvoked()) {
      form.clearFilter();
    }

    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatus(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var modelAndView = new ModelAndView("lms/licence/schedule/licencetab/licenceScheduleTab");
    tabbedLicencePageService.hydrateModel(modelAndView, licence, licenceScheduleTab, user);

    modelAndView.addObject("form", form)
        .addObject("historyForm", new LicenceScheduleHistoryForm())
        .addObject("scheduleExists", licenceScheduleDetail.isPresent())
        .addObject("scheduleHistoryOptions", licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence))
        .addObject("viewScheduleHistoryUrl", ReverseRouter.route(on(LicenceScheduleTabController.class)
            .viewScheduleHistory(licenceId, null))
        );

    licenceScheduleDetail.ifPresent(scheduleDetail -> modelAndView
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(scheduleDetail))
        .addObject("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions())
        .addObject("scheduleEventViews",
            licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(scheduleDetail, form, user)
        )
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceScheduleTabController.class)
            .clearFilters(licenceId, null, null))
        )
    );

    return modelAndView;
  }

  @PostMapping
  public ModelAndView filterTimeline(
      @PathVariable Integer licenceId,
      @ModelAttribute("form") ScheduleTimelineFilterForm form,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearFilters(
      @PathVariable Integer licenceId,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @GetMapping("/work-programmes-only")
  public ModelAndView renderWorkProgrammesOnlyTimeline(
      @PathVariable Integer licenceId,
      @ModelAttribute("scheduleTimelineSession") ScheduleTimelineSession filterSession
  ) {
    var form = new ScheduleTimelineFilterForm();
    form.setEventTypes(List.of(ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name()));
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @PostMapping("/schedule-history")
  public ModelAndView viewScheduleHistory(
      @PathVariable Integer licenceId,
      @ModelAttribute("historyForm") LicenceScheduleHistoryForm form
  ) {
    if (!StringUtils.hasText(form.getLicenceScheduleDetailId())) {
      return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
          .renderLicenceOverview(licenceId, null, null, null));
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

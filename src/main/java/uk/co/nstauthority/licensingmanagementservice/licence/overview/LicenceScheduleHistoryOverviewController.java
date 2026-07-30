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
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineSession;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}/overview")
@SessionAttributes("timelineSession")
public class LicenceScheduleHistoryOverviewController {

  private final LicenceActionService licenceActionService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTimelineService licenceScheduleTimelineService;

  public LicenceScheduleHistoryOverviewController(
      LicenceActionService licenceActionService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTimelineService licenceScheduleTimelineService
  ) {
    this.licenceActionService = licenceActionService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
  }

  @GetMapping
  public ModelAndView renderLicenceOverview(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
      ServiceUserDetail user
  ) {
    var form = filterSession.getTimelineFilterForm();

    if (!filterSession.hasFilterBeenInvoked()) {
      form.clearFilter();
    }

    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/licenceOverview")
        .addObject("form", form)
        .addObject("licenceReference", licence.getLicenceReference())
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("licenceActions", licenceActionService.getAvailableUserActionItems(licence, user))
        .addObject("historyForm", new LicenceScheduleHistoryForm())
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
      @ModelAttribute("form") TimelineFilterForm form,
      @ModelAttribute("timelineSession") TimelineSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(licenceScheduleDetailId, null, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearFilters(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
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

  @ModelAttribute("timelineSession")
  private TimelineSession getFilterSession(
      @ModelAttribute("form") TimelineFilterForm form
  ) {
    return new TimelineSession(form);
  }
}

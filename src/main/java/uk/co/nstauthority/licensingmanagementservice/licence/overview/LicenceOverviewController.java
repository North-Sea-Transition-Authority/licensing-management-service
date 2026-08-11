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
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineSession;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/overview")
@SessionAttributes("timelineSession")
public class LicenceOverviewController {

  private final LicenceActionService licenceActionService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTimelineService licenceScheduleTimelineService;
  private final LicenceScheduleOverviewService licenceScheduleOverviewService;

  public LicenceOverviewController(
      LicenceActionService licenceActionService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTimelineService licenceScheduleTimelineService,
      LicenceScheduleOverviewService licenceScheduleOverviewService
  ) {
    this.licenceActionService = licenceActionService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
    this.licenceScheduleOverviewService = licenceScheduleOverviewService;
  }

  @GetMapping
  public ModelAndView renderLicenceOverview(
      @PathVariable Integer licenceId,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
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

    var modelAndView = new ModelAndView("lms/licence/licenceOverview")
        .addObject("form", form)
        .addObject("licenceReference", licence.getLicenceReference())
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("licenceActions", licenceActionService.getAvailableUserActionItems(licence, user))
        .addObject("historyForm", new LicenceScheduleHistoryForm())
        .addObject("scheduleHistoryOptions", licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence))
        .addObject("viewScheduleHistoryUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .viewScheduleHistory(licenceId, null))
        ).addObject("csRegisterUrl", licenceScheduleOverviewService.getCsRegisterlink(licence));

    licenceScheduleDetail.ifPresent(scheduleDetail -> modelAndView
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(scheduleDetail))
        .addObject("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions())
        .addObject("scheduleEventViews",
            licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(scheduleDetail, form, user)
        )
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .clearFilters(licenceId, null, null))
        )
    );

    return modelAndView;
  }

  @PostMapping
  public ModelAndView filterTimeline(
      @PathVariable Integer licenceId,
      @ModelAttribute("form") TimelineFilterForm form,
      @ModelAttribute("timelineSession") TimelineSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearFilters(
      @PathVariable Integer licenceId,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @GetMapping("/work-programmes-only")
  public ModelAndView renderWorkProgrammesOnlyTimeline(
      @PathVariable Integer licenceId,
      @ModelAttribute("timelineSession") TimelineSession filterSession
  ) {
    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name()));
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  @PostMapping("/schedule-history")
  public ModelAndView viewScheduleHistory(
      @PathVariable Integer licenceId,
      @ModelAttribute("historyForm") LicenceScheduleHistoryForm form
  ) {
    if (!StringUtils.hasText(form.getLicenceScheduleDetailId())) {
      return ReverseRouter.redirect(on(LicenceOverviewController.class)
          .renderLicenceOverview(licenceId, null, null, null));
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

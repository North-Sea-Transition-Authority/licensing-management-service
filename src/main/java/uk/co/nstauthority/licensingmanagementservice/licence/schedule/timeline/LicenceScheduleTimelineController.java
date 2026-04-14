package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.DeleteDraftScheduleController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.ReviewAndApplyScheduleController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}")
@SessionAttributes("timelineSession")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class LicenceScheduleTimelineController {

  public static final String PAGE_TITLE = "%s - Licence schedule and work programme";

  private final LicenceScheduleTimelineService licenceScheduleTimelineService;

  public LicenceScheduleTimelineController(
      LicenceScheduleTimelineService licenceScheduleTimelineService
  ) {
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
  }
  
  @GetMapping
  public ModelAndView renderLicenceScheduleTimeline(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
      ServiceUserDetail serviceUserDetail
  ) {
    var form = filterSession.getTimelineFilterForm();

    if (!filterSession.hasFilterBeenInvoked()) {
      form.clearFilter();
    }

    return getScheduleTimelineModelAndView(
        licenceScheduleDetail.getLicenceSchedule().getLicence(),
        licenceScheduleDetail,
        form,
        serviceUserDetail
    );
  }

  @PostMapping
  public ModelAndView filterTimeline(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("form") TimelineFilterForm form,
      @ModelAttribute("timelineSession") TimelineSession filterSession
  ) {
    filterSession.update(form);
    return ReverseRouter.redirect(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetailId, null, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearFilters(
      @PathVariable UUID licenceScheduleDetailId,
      @ModelAttribute("timelineSession") TimelineSession filterSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetailId, null, null, null));
  }

  @ModelAttribute("timelineSession")
  private TimelineSession getFilterSession(
      @ModelAttribute("form") TimelineFilterForm form
  ) {
    return new TimelineSession(form);
  }
  
  private ModelAndView getScheduleTimelineModelAndView(
      Licence licence,
      LicenceScheduleDetail licenceScheduleDetail,
      TimelineFilterForm timelineFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    return new ModelAndView("lms/licence/schedule/timeline/scheduleTimeline")
        .addObject("form", timelineFilterForm)
        .addObject("pageTitle", PAGE_TITLE.formatted(licence.getLicenceReference()))
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("actions",
            licenceScheduleTimelineService.getLicenceScheduleTimelineActions(
                licenceScheduleDetail,
                serviceUserDetail
            )
        )
        .addObject("scheduleEventViews",
            licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(
                licenceScheduleDetail,
                timelineFilterForm,
                serviceUserDetail
            )
        )
        .addObject("invalidScheduleEvents",
            licenceScheduleTimelineService.getEventsBeyondFinalTerm(licenceScheduleDetail, serviceUserDetail)
        )
        .addObject("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions())
        .addObject("updateLicenceStartDateUrl", ReverseRouter.route(on(LicenceStartDateController.class)
            .renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null))
        )
        .addObject("updateExpiryDateUrl", ReverseRouter.route(on(LicenceScheduleExpiryController.class)
            .renderAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null))
        )
        .addObject("reviewAndApplyUrl", ReverseRouter.route(on(ReviewAndApplyScheduleController.class)
            .renderReviewAndApplyPage(licenceScheduleDetail.getId(), null))
        )
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .clearFilters(licenceScheduleDetail.getId(), null, null))
        )
        .addObject("deleteScheduleUrl", ReverseRouter.route(on(DeleteDraftScheduleController.class)
            .renderDeleteDraftPage(licenceScheduleDetail.getId(), null))
        );
  }

}

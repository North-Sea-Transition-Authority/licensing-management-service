package uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}/review")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class ReviewAndApplyScheduleController {

  private final LicenceService licenceService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTimelineService licenceScheduleTimelineService;
  private final EventCommentService eventCommentService;
  private final ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  public ReviewAndApplyScheduleController(
      LicenceService licenceService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTimelineService licenceScheduleTimelineService,
      EventCommentService eventCommentService,
      ScheduleRelativeDateValidationService scheduleRelativeDateValidationService
  ) {
    this.licenceService = licenceService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
    this.eventCommentService = eventCommentService;
    this.scheduleRelativeDateValidationService = scheduleRelativeDateValidationService;
  }

  @GetMapping
  public ModelAndView renderReviewAndApplyPage(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/reviewAndApply")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("summaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("initialTermPhaseValidationError",
            !scheduleRelativeDateValidationService.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl());
  }

  @PostMapping
  public ModelAndView applyDraftSchedule(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      RedirectAttributes redirectAttributes
  ) {
    eventCommentService.publishPendingCommentsForSchedule(licenceScheduleDetail.getLicenceSchedule());
    licenceScheduleDetailService.applyAndReplaceActiveScheduleDetail(licenceScheduleDetail);

    var licenceReference = licenceScheduleDetail.getLicenceSchedule().getLicence().getLicenceReference();

    NotificationBanner.newSuccessBanner()
        .withHeadingContent(String.format("Changes to the schedule for %s have been applied", licenceReference))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }
}

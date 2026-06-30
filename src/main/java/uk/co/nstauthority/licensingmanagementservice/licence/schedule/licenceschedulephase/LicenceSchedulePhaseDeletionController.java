package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/phase/{licenceSchedulePhaseId}/delete")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceSchedulePhaseDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete the %s?";

  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final EventCommentService eventCommentService;
  private final LicenceService licenceService;

  public LicenceSchedulePhaseDeletionController(
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      EventCommentService eventCommentService,
      LicenceService licenceService
  ) {
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.eventCommentService = eventCommentService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderDeletePhasePage(
      @PathVariable UUID licenceSchedulePhaseId
  ) {
    var phase = licenceSchedulePhaseService.getPhaseByIdOrThrow(licenceSchedulePhaseId);
    var pendingComment = phase.getEventReference() != null
        ? eventCommentService.findPendingCommentForEventReference(phase.getEventReference())
            .map(EventComment::getComment)
            .orElse("")
        : "";

    return new ModelAndView("lms/licence/schedule/deleteSchedulePhase")
        .addObject("pageTitle", PAGE_TITLE.formatted(phase.getPhaseType().getDisplayName()))
        .addObject("licenceSchedulePhaseSummaryView", LicenceSchedulePhaseSummaryView.fromPhase(phase))
        .addObject("pendingComment", pendingComment)
        .addObject("cancelUrl", phase.getLicenceScheduleDetail().getScheduleTimelineRouteUrl())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(phase.getLicenceScheduleDetail().getLicenceSchedule().getLicence()));
  }

  @PostMapping
  public ModelAndView submitDeletePhasePage(
      @PathVariable UUID licenceSchedulePhaseId,
      RedirectAttributes redirectAttributes
  ) {
    var phase = licenceSchedulePhaseService.getPhaseByIdOrThrow(licenceSchedulePhaseId);
    var licenceScheduleDetail = phase.getLicenceScheduleDetail();
    licenceSchedulePhaseService.deletePhase(phase);

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    NotificationBanner.newSuccessBannerWithHeader(
        String.format("%s has been deleted", phase.getPhaseType().getDisplayName()), redirectAttributes
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }
}
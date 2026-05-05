package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/other-schedule-event/{otherScheduleEventId}/delete")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class OtherScheduleEventDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete the %s event?";

  private final OtherScheduleEventService otherScheduleEventService;
  private final LicenceService licenceService;

  public OtherScheduleEventDeletionController(
      OtherScheduleEventService otherScheduleEventService,
      LicenceService licenceService
  ) {
    this.otherScheduleEventService = otherScheduleEventService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderDeleteEventPage(
      @PathVariable UUID otherScheduleEventId
  ) {
    var event = otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEventId);

    return new ModelAndView("lms/licence/schedule/deleteOtherScheduleEvent")
        .addObject("pageTitle", PAGE_TITLE.formatted(event.getCategoryString()))
        .addObject("summaryView", OtherScheduleEventSummaryView.fromOtherScheduleEvent(event))
        .addObject("cancelUrl", event.getLicenceScheduleDetail().getScheduleTimelineRouteUrl())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(event.getLicenceScheduleDetail().getLicenceSchedule().getLicence()));
  }

  @PostMapping
  ModelAndView submitDeleteEventPage(
      @PathVariable UUID otherScheduleEventId,
      RedirectAttributes redirectAttributes
  ) {
    var event = otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEventId);

    var licenceScheduleDetail = event.getLicenceScheduleDetail();
    otherScheduleEventService.deleteOtherScheduleEvent(event);

    NotificationBanner.newSuccessBannerWithHeader(
        String.format("%s has been deleted", event.getCategoryString()), redirectAttributes
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

}

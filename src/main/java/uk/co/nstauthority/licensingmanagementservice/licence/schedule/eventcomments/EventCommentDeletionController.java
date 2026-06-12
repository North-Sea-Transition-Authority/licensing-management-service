package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/event-comment/{eventCommentId}/delete")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class EventCommentDeletionController {

  private final EventCommentService eventCommentService;
  private final LicenceService licenceService;

  public EventCommentDeletionController(
      EventCommentService eventCommentService,
      LicenceService licenceService
  ) {
    this.eventCommentService = eventCommentService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderDeleteCommentPage(
      @PathVariable UUID eventCommentId,
      ServiceUserDetail serviceUserDetail
  ) {
    var eventComment = eventCommentService.getEventCommentByIdOrThrow(eventCommentId);
    eventCommentService.checkCommenterHasPermissionsOrThrow(
        eventComment.getEventReference().getEventType(),
        serviceUserDetail
    );

    var licence = eventComment.getEventReference().getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/deleteEventComment")
        .addObject("commentView", eventCommentService.getEventCommentViewFor(eventComment))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)));
  }

  @PostMapping
  public ModelAndView submitDeleteCommentPage(
      @PathVariable UUID eventCommentId,
      RedirectAttributes redirectAttributes,
      ServiceUserDetail serviceUserDetail
  ) {
    var eventComment = eventCommentService.getEventCommentByIdOrThrow(eventCommentId);
    eventCommentService.checkCommenterHasPermissionsOrThrow(
        eventComment.getEventReference().getEventType(),
        serviceUserDetail
    );

    var licenceId = eventComment.getEventReference().getLicenceSchedule().getLicence().getId();

    eventCommentService.deleteEventComment(eventComment);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Comment deleted")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }
}

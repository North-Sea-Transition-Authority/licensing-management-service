package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

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
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/schedule-comment/{scheduleCommentId}/remove")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class ScheduleCommentRemovalController {

  private final ScheduleCommentService scheduleCommentService;
  private final LicenceService licenceService;

  public ScheduleCommentRemovalController(
      ScheduleCommentService scheduleCommentService,
      LicenceService licenceService
  ) {
    this.scheduleCommentService = scheduleCommentService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderRemoveCommentPage(@PathVariable UUID scheduleCommentId) {
    var scheduleComment = scheduleCommentService.getCommentOrThrow(scheduleCommentId);
    var licence = scheduleComment.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/removeScheduleComment")
        .addObject("commentView", scheduleCommentService.getCommentView(scheduleComment))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceScheduleTabController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)));
  }

  @PostMapping
  public ModelAndView removeComment(
      @PathVariable UUID scheduleCommentId,
      RedirectAttributes redirectAttributes
  ) {
    var scheduleComment = scheduleCommentService.getCommentOrThrow(scheduleCommentId);
    var licenceId = scheduleComment.getLicenceSchedule().getLicence().getId();

    scheduleCommentService.removeComment(scheduleComment);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Comment removed")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }
}

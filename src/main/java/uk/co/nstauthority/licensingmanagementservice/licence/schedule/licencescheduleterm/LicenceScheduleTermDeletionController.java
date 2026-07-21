package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/term/{licenceScheduleTermId}/delete")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceScheduleTermDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete the %s?";

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final EventCommentService eventCommentService;
  private final LicenceService licenceService;
  private final ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  public LicenceScheduleTermDeletionController(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      EventCommentService eventCommentService,
      LicenceService licenceService,
      ScheduleRelativeDateValidationService scheduleRelativeDateValidationService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.eventCommentService = eventCommentService;
    this.licenceService = licenceService;
    this.scheduleRelativeDateValidationService = scheduleRelativeDateValidationService;
  }

  @GetMapping
  public ModelAndView renderDeleteTermPage(
      @PathVariable UUID licenceScheduleTermId
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);
    var pendingComment = term.getLicenceSchedule() != null
        ? eventCommentService.findPendingCommentForScheduleEvent(term)
            .map(EventComment::getComment)
            .orElse("")
        : "";

    var canDeleteErrorMessage = "";
    if (!licenceScheduleTermService.canDeleteTerm(term)) {
      canDeleteErrorMessage = "This cannot be deleted as it is referenced by other schedule events.";
    } else if (!scheduleRelativeDateValidationService.isTermRemovalValid(term)) {
      canDeleteErrorMessage = "This cannot be deleted as it would cause events to end after the final term.";
    }

    return new ModelAndView("lms/licence/schedule/deleteScheduleTerm")
        .addObject("pageTitle", PAGE_TITLE.formatted(term.getTermType().getDisplayName()))
        .addObject("licenceScheduleTermSummaryView", LicenceScheduleTermSummaryView.fromTerm(term))
        .addObject("pendingComment", pendingComment)
        .addObject("cancelUrl", term.getLicenceScheduleDetail().getScheduleTimelineRouteUrl())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(term.getLicenceScheduleDetail().getLicenceSchedule().getLicence()))
        .addObject("canDeleteErrorMessage", canDeleteErrorMessage);
  }

  @PostMapping
  public ModelAndView submitDeleteTermPage(
      @PathVariable UUID licenceScheduleTermId,
      RedirectAttributes redirectAttributes
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);
    var licenceScheduleDetail = term.getLicenceScheduleDetail();

    if (!licenceScheduleTermService.canDeleteTerm(term)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot delete term id: %s as it is referenced by other schedule events".formatted(term.getId())
      );
    }

    if (!scheduleRelativeDateValidationService.isTermRemovalValid(term)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot delete term id: %s as it would cause events to occur after the end of the final term".formatted(term.getId())
      );
    }

    licenceScheduleTermService.deleteTerm(term);

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    NotificationBanner.newSuccessBannerWithHeader(
        String.format("%s has been deleted", term.getTermType().getDisplayName()), redirectAttributes
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

}
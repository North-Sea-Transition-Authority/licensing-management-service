package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/{eventTypeUrlSlug}/{eventReferenceId}/add-comment")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class EventCommentController {

  private final EventCommentService eventCommentService;
  private final EventCommentValidator eventCommentValidator;
  private final EventReferenceService eventReferenceService;
  private final LicenceService licenceService;

  public EventCommentController(
      EventCommentService eventCommentService,
      EventCommentValidator eventCommentValidator,
      EventReferenceService eventReferenceService,
      LicenceService licenceService
  ) {
    this.eventCommentService = eventCommentService;
    this.eventCommentValidator = eventCommentValidator;
    this.eventReferenceService = eventReferenceService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderAddCommentForm(
      @PathVariable String eventTypeUrlSlug,
      @PathVariable UUID eventReferenceId
  ) {
    var eventReference = eventReferenceService.getEventReferenceByIdOrThrow(eventReferenceId);
    return getCommentModelAndView(new EventCommentForm(), eventReference, eventTypeUrlSlug);
  }

  @PostMapping
  public ModelAndView submitAddCommentForm(
      @PathVariable String eventTypeUrlSlug,
      @PathVariable UUID eventReferenceId,
      ServiceUserDetail serviceUserDetail,
      @ModelAttribute("form") EventCommentForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var eventReference = eventReferenceService.getEventReferenceByIdOrThrow(eventReferenceId);

    if (!eventCommentValidator.isValid(bindingResult)) {
      return getCommentModelAndView(form, eventReference, eventTypeUrlSlug);
    }

    eventCommentService.addNewComment(form, eventReference, serviceUserDetail);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Comment added")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(eventReference.getLicenceSchedule().getLicence().getId(), null, null, null));
  }

  private ModelAndView getCommentModelAndView(
      EventCommentForm form,
      EventReference eventReference,
      String eventTypeUrlSlug
  ) {
    var licence = eventReference.getLicenceSchedule().getLicence();

    var caption = "%s - %s".formatted(
        licenceService.getLicencePageCaption(licence),
        eventReferenceService.getEventReferenceEventCaption(
            eventReference,
            ScheduleEventType.getFromSlugOrThrow(eventTypeUrlSlug)
        )
    );

    return new ModelAndView("lms/licence/schedule/createEventComment")
        .addObject("form", form)
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)))
        .addObject("pageCaption", caption);
  }
}

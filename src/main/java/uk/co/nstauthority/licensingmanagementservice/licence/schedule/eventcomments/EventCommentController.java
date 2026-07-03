package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/{eventReferenceId}/add-comment")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class EventCommentController {

  private final EventCommentService eventCommentService;
  private final EventCommentValidator eventCommentValidator;
  private final ScheduleEventRepository scheduleEventRepository;
  private final LicenceService licenceService;

  public EventCommentController(
      EventCommentService eventCommentService,
      EventCommentValidator eventCommentValidator,
      ScheduleEventRepository scheduleEventRepository,
      LicenceService licenceService
  ) {
    this.eventCommentService = eventCommentService;
    this.eventCommentValidator = eventCommentValidator;
    this.scheduleEventRepository = scheduleEventRepository;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderAddCommentForm(
      @PathVariable UUID eventReferenceId,
      ServiceUserDetail serviceUserDetail
  ) {
    var scheduleEvent = getScheduleEventOrThrow(eventReferenceId);
    eventCommentService.checkCommenterHasPermissionsOrThrow(
        scheduleEvent.getEventType(),
        serviceUserDetail
    );

    return getCommentModelAndView(new EventCommentForm(), scheduleEvent);
  }

  @PostMapping
  public ModelAndView submitAddCommentForm(
      @PathVariable UUID eventReferenceId,
      ServiceUserDetail serviceUserDetail,
      @ModelAttribute("form") EventCommentForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var scheduleEvent = getScheduleEventOrThrow(eventReferenceId);
    eventCommentService.checkCommenterHasPermissionsOrThrow(
        scheduleEvent.getEventType(),
        serviceUserDetail
    );

    if (!eventCommentValidator.isValid(bindingResult)) {
      return getCommentModelAndView(form, scheduleEvent);
    }

    eventCommentService.addNewComment(form, scheduleEvent, serviceUserDetail);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Comment added")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(scheduleEvent.getLicenceSchedule().getLicence().getId(), null, null, null));
  }

  private ModelAndView getCommentModelAndView(EventCommentForm form, ScheduleEvent scheduleEvent) {
    var licence = scheduleEvent.getLicenceSchedule().getLicence();

    var caption = "%s - %s".formatted(
        licenceService.getLicencePageCaption(licence),
        scheduleEvent.getEventCaption()
    );

    return new ModelAndView("lms/licence/schedule/createEventComment")
        .addObject("form", form)
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)))
        .addObject("pageCaption", caption);
  }

  private ScheduleEvent getScheduleEventOrThrow(UUID id) {
    return scheduleEventRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}

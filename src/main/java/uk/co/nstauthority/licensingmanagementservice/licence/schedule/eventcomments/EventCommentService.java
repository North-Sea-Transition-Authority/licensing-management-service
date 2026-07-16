package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class EventCommentService {

  static final String COMMENT_AUTHOR_PURPOSE = "Fetch author names for licence schedule timeline comments";

  private final EventCommentRepository eventCommentRepository;
  private final EnergyPortalUserService energyPortalUserService;
  private final Clock clock;
  private final TeamQueryService teamQueryService;

  public EventCommentService(
      EventCommentRepository eventCommentRepository,
      EnergyPortalUserService energyPortalUserService,
      Clock clock,
      TeamQueryService teamQueryService
  ) {
    this.eventCommentRepository = eventCommentRepository;
    this.energyPortalUserService = energyPortalUserService;
    this.clock = clock;
    this.teamQueryService = teamQueryService;
  }

  @Transactional
  public void addNewComment(
      EventCommentForm form,
      ScheduleEvent scheduleEvent,
      ServiceUserDetail author
  ) {
    var eventComment = new EventComment();
    eventComment.setScheduleEvent(scheduleEvent);
    eventComment.setComment(form.getComment());
    eventComment.setStatus(EventCommentStatus.PUBLISHED);
    eventComment.setTimestamp(clock.instant());
    eventComment.setAuthorWuaId(author.wuaId());
    eventCommentRepository.save(eventComment);
  }

  @Transactional
  public void addOrUpdatePendingComment(
      String comment,
      ScheduleEvent scheduleEvent,
      ServiceUserDetail author
  ) {
    var existingPendingComment = eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING);

    if (comment == null || comment.isBlank()) {
      existingPendingComment.ifPresent(eventCommentRepository::delete);
      return;
    }

    var eventComment = existingPendingComment.orElseGet(EventComment::new);
    eventComment.setScheduleEvent(scheduleEvent);
    eventComment.setComment(comment);
    eventComment.setStatus(EventCommentStatus.PENDING);
    eventComment.setTimestamp(clock.instant());
    eventComment.setAuthorWuaId(author.wuaId());
    eventCommentRepository.save(eventComment);
  }

  public Optional<EventComment> findPendingCommentForScheduleEvent(ScheduleEvent scheduleEvent) {
    return eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING);
  }

  public Map<UUID, List<EventCommentView>> getEventCommentViewsForSchedule(LicenceSchedule licenceSchedule) {
    var comments = eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PUBLISHED);

    if (comments.isEmpty()) {
      return Map.of();
    }

    var authorWuaIds = comments.stream()
        .map(EventComment::getAuthorWuaId)
        .filter(Objects::nonNull)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();

    var wuaIdNameMap = energyPortalUserService.findByWuaIds(authorWuaIds, COMMENT_AUTHOR_PURPOSE).stream()
        .collect(StreamUtil.toLinkedHashMap(
            EnergyPortalUserJson::webUserAccountId,
            EnergyPortalUserJson::displayName
            )
        );

    return comments.stream()
        .sorted(Comparator.comparing(EventComment::getTimestamp))
        .collect(Collectors.groupingBy(
            comment -> comment.getScheduleEvent().getOriginalEventId(),
            Collectors.mapping(eventComment -> createViewFrom(eventComment, wuaIdNameMap), Collectors.toList())
        ));
  }

  @Transactional
  public void deletePendingCommentsForSchedule(LicenceSchedule licenceSchedule) {
    eventCommentRepository.deleteAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PENDING);
  }

  @Transactional
  public void publishPendingCommentsForSchedule(LicenceSchedule licenceSchedule) {
    var pendingComments = eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PENDING);
    pendingComments.forEach(comment -> comment.setStatus(EventCommentStatus.PUBLISHED));
    eventCommentRepository.saveAll(pendingComments);
  }

  public EventComment getEventCommentByIdOrThrow(UUID id) {
    return eventCommentRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("EventComment", id));
  }

  public EventCommentView getEventCommentViewFor(EventComment eventComment) {
    var authorName = energyPortalUserService.findByWuaId(
        WebUserAccountId.from(eventComment.getAuthorWuaId()),
        COMMENT_AUTHOR_PURPOSE
    ).orElseThrow(() -> new LmsEntityNotFoundException("User", Math.toIntExact(eventComment.getAuthorWuaId())))
        .displayName();

    return createViewFrom(eventComment, authorName);
  }

  @Transactional
  public void deletePendingCommentForScheduleEvent(ScheduleEvent scheduleEvent) {
    eventCommentRepository.deleteByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING);
  }

  @Transactional
  public void deleteEventComment(EventComment eventComment) {
    eventCommentRepository.delete(eventComment);
  }

  private EventCommentView createViewFrom(
      EventComment eventComment,
      Map<Long, String> wuaIdNameMap
  ) {
    return createViewFrom(eventComment, wuaIdNameMap.get(eventComment.getAuthorWuaId()));
  }

  private EventCommentView createViewFrom(
      EventComment eventComment,
      String authorName
  ) {
    var deleteUrl = ReverseRouter.route(on(EventCommentDeletionController.class)
        .renderDeleteCommentPage(eventComment.getId(), null));

    return new EventCommentView(
        eventComment.getComment(),
        authorName != null ? authorName : "Unknown",
        DateFormatUtil.convertToDisplayTextWithTime(eventComment.getTimestamp()),
        deleteUrl
    );
  }

  void checkCommenterHasPermissionsOrThrow(
      ScheduleEventType scheduleEventType,
      ServiceUserDetail serviceUserDetail
  ) {
    if (scheduleEventType.equals(ScheduleEventType.WORK_PROGRAMME_ACTIVITY)) {
      if (!teamQueryService.userHasAtLeastOneRoleIn(serviceUserDetail.wuaId(),
          Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR))) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "User does not have permission edit comments on work programme activities"
        );
      }
      return;
    }

    if (!teamQueryService.userHasAtLeastOneRoleIn(serviceUserDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR))) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "User does not have permission edit comments on schedule events"
      );
    }
  }
}

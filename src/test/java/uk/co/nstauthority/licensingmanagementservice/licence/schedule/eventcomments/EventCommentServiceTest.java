package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceTest {

  @Mock
  private EventCommentRepository eventCommentRepository;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private Clock clock;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private EventCommentService eventCommentService;

  @Captor
  private ArgumentCaptor<EventComment> eventCommentArgumentCaptor;

  @Test
  void addNewComment() {
    var scheduleEvent = new WorkProgrammeActivity();
    var form = new EventCommentForm();
    form.setComment("Test comment");
    var author = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(42L)
        .build();

    var fixedInstant = Instant.now();
    when(clock.instant()).thenReturn(fixedInstant);

    eventCommentService.addNewComment(form, scheduleEvent, author);

    verify(eventCommentRepository).save(eventCommentArgumentCaptor.capture());

    assertThat(eventCommentArgumentCaptor.getValue())
        .extracting(
            EventComment::getScheduleEvent,
            EventComment::getComment,
            EventComment::getStatus,
            EventComment::getAuthorWuaId,
            EventComment::getTimestamp
        )
        .containsExactly(
            scheduleEvent,
            "Test comment",
            EventCommentStatus.PUBLISHED,
            42L,
            fixedInstant
        );
  }

  @Test
  void addOrUpdatePendingComment_whenCommentIsNull_andPendingCommentExists_deletesExisting() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());
    var author = ServiceUserDetailTestUtil.newBuilder().withWuaId(42L).build();
    var existingComment = new EventComment();

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.of(existingComment));

    eventCommentService.addOrUpdatePendingComment(null, scheduleEvent, author);

    verify(eventCommentRepository).delete(existingComment);
  }

  @Test
  void addOrUpdatePendingComment_whenCommentIsBlank_andNoPendingComment_doesNothing() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());
    var author = ServiceUserDetailTestUtil.newBuilder().withWuaId(42L).build();

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.empty());

    eventCommentService.addOrUpdatePendingComment("  ", scheduleEvent, author);

    verify(eventCommentRepository, never()).save(any(EventComment.class));
    verify(eventCommentRepository, never()).delete(any(EventComment.class));
  }

  @Test
  void addOrUpdatePendingComment_whenCommentProvided_andNoPendingComment_savesNewComment() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());
    var author = ServiceUserDetailTestUtil.newBuilder().withWuaId(42L).build();
    var fixedInstant = Instant.parse("2025-01-01T10:00:00Z");

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.empty());
    when(clock.instant()).thenReturn(fixedInstant);

    eventCommentService.addOrUpdatePendingComment("New comment", scheduleEvent, author);

    verify(eventCommentRepository).save(eventCommentArgumentCaptor.capture());

    assertThat(eventCommentArgumentCaptor.getValue())
        .extracting(
            EventComment::getScheduleEvent,
            EventComment::getComment,
            EventComment::getStatus,
            EventComment::getAuthorWuaId,
            EventComment::getTimestamp
        )
        .containsExactly(scheduleEvent, "New comment", EventCommentStatus.PENDING, 42L, fixedInstant);
  }

  @Test
  void addOrUpdatePendingComment_whenCommentProvided_andPendingCommentExists_updatesExisting() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());
    var author = ServiceUserDetailTestUtil.newBuilder().withWuaId(42L).build();
    var existingComment = new EventComment();
    existingComment.setComment("Old comment");
    var fixedInstant = Instant.parse("2025-06-01T12:00:00Z");

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.of(existingComment));
    when(clock.instant()).thenReturn(fixedInstant);

    eventCommentService.addOrUpdatePendingComment("Updated comment", scheduleEvent, author);

    verify(eventCommentRepository).save(eventCommentArgumentCaptor.capture());

    assertThat(eventCommentArgumentCaptor.getValue())
        .isSameAs(existingComment)
        .extracting(
            EventComment::getScheduleEvent,
            EventComment::getComment,
            EventComment::getStatus,
            EventComment::getAuthorWuaId,
            EventComment::getTimestamp
        )
        .containsExactly(scheduleEvent, "Updated comment", EventCommentStatus.PENDING, 42L, fixedInstant);
  }

  @Test
  void getEventCommentViewsForSchedule_returnsCommentsMappedByEventReferenceId() {
    var licenceSchedule = new LicenceSchedule();

    var scheduleEvent1 = new WorkProgrammeActivity();
    scheduleEvent1.setId(UUID.randomUUID());
    scheduleEvent1.setOriginalEventId(scheduleEvent1.getId());

    var scheduleEvent2 = new WorkProgrammeActivity();
    scheduleEvent2.setId(UUID.randomUUID());
    scheduleEvent2.setOriginalEventId(scheduleEvent2.getId());

    var authorWuaId = 99L;

    var comment1 = new EventComment();
    comment1.setId(UUID.randomUUID());
    comment1.setScheduleEvent(scheduleEvent1);
    comment1.setComment("First comment");
    comment1.setAuthorWuaId(authorWuaId);
    comment1.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));

    var comment2 = new EventComment();
    comment2.setId(UUID.randomUUID());
    comment2.setScheduleEvent(scheduleEvent2);
    comment2.setComment("Second comment");
    comment2.setAuthorWuaId(authorWuaId);
    comment2.setTimestamp(Instant.parse("2025-01-02T12:00:00Z"));

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(licenceSchedule, EventCommentStatus.PUBLISHED))
        .thenReturn(List.of(comment1, comment2));

    var userJson = new EnergyPortalUserJson(authorWuaId, null, "Jane", "Smith", null, null, true, null, false);

    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(authorWuaId)),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of(userJson));

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).hasSize(2);

    assertThat(result.get(scheduleEvent1.getOriginalEventId()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new EventCommentView(
            "First comment",
            userJson.displayName(),
            DateFormatUtil.convertToDisplayTextWithTime(comment1.getTimestamp()),
            ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(comment1.getId(), null))
        )));

    assertThat(result.get(scheduleEvent2.getOriginalEventId()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new EventCommentView(
            "Second comment",
            userJson.displayName(),
            DateFormatUtil.convertToDisplayTextWithTime(comment2.getTimestamp()),
            ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(comment2.getId(), null))
        )));
  }

  @Test
  void getEventCommentViewsForSchedule_sortsByTimestamp() {
    var licenceSchedule = new LicenceSchedule();

    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setId(UUID.randomUUID());
    scheduleEvent.setOriginalEventId(scheduleEvent.getId());

    var authorWuaId = 55L;

    var newerComment = new EventComment();
    newerComment.setScheduleEvent(scheduleEvent);
    newerComment.setComment("Newer comment");
    newerComment.setAuthorWuaId(authorWuaId);
    newerComment.setTimestamp(Instant.parse("2025-06-01T15:00:00Z"));

    var olderComment = new EventComment();
    olderComment.setScheduleEvent(scheduleEvent);
    olderComment.setComment("Older comment");
    olderComment.setAuthorWuaId(authorWuaId);
    olderComment.setTimestamp(Instant.parse("2025-01-01T09:00:00Z"));

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(licenceSchedule, EventCommentStatus.PUBLISHED))
        .thenReturn(List.of(newerComment, olderComment));

    var userJson = new EnergyPortalUserJson(authorWuaId, null, "Bob", "Jones", null, null, true, null, false);

    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(authorWuaId)),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of(userJson));

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).hasSize(1);

    var comments = result.get(scheduleEvent.getOriginalEventId());
    assertThat(comments).hasSize(2);
    assertThat(comments.get(0).comment()).isEqualTo("Older comment");
    assertThat(comments.get(1).comment()).isEqualTo("Newer comment");
  }

  @Test
  void getEventCommentViewsForSchedule_emptySchedule() {
    var licenceSchedule = new LicenceSchedule();

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(licenceSchedule, EventCommentStatus.PUBLISHED))
        .thenReturn(List.of());

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).isEqualTo(Map.of());
  }

  @Test
  void getEventCommentViewsForSchedule_whenAuthorWuaIdIsNull_usesUnknownAuthorName() {
    var licenceSchedule = new LicenceSchedule();

    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setId(UUID.randomUUID());
    scheduleEvent.setOriginalEventId(scheduleEvent.getId());

    var comment = new EventComment();
    comment.setId(UUID.randomUUID());
    comment.setScheduleEvent(scheduleEvent);
    comment.setComment("A comment");
    comment.setAuthorWuaId(null);
    comment.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(licenceSchedule, EventCommentStatus.PUBLISHED))
        .thenReturn(List.of(comment));

    when(energyPortalUserService.findByWuaIds(
        List.of(),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of());

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).hasSize(1);
    assertThat(result.get(scheduleEvent.getOriginalEventId()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new EventCommentView(
            "A comment",
            "Unknown",
            DateFormatUtil.convertToDisplayTextWithTime(comment.getTimestamp()),
            ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(comment.getId(), null))
        )));
  }

  @Test
  void getEventCommentByIdOrThrow_whenCommentExists_returnsComment() {
    var id = UUID.randomUUID();
    var eventComment = new EventComment();
    eventComment.setId(id);

    when(eventCommentRepository.findById(id)).thenReturn(Optional.of(eventComment));

    assertThat(eventCommentService.getEventCommentByIdOrThrow(id)).isEqualTo(eventComment);
  }

  @Test
  void getEventCommentByIdOrThrow_whenCommentNotFound_throwsException() {
    var id = UUID.randomUUID();

    when(eventCommentRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventCommentService.getEventCommentByIdOrThrow(id))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getEventCommentViewFor_returnsViewWithResolvedAuthorName() {
    var authorWuaId = 77L;

    var eventComment = new EventComment();
    eventComment.setId(UUID.randomUUID());
    eventComment.setComment("A comment");
    eventComment.setAuthorWuaId(authorWuaId);
    eventComment.setTimestamp(Instant.parse("2025-03-15T09:30:00Z"));

    var userJson = new EnergyPortalUserJson(authorWuaId, null, "Alice", "Brown", null, null, true, null, false);

    when(energyPortalUserService.findByWuaId(
        WebUserAccountId.from(authorWuaId),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(Optional.of(userJson));

    assertThat(eventCommentService.getEventCommentViewFor(eventComment))
        .usingRecursiveComparison()
        .isEqualTo(new EventCommentView(
            "A comment",
            userJson.displayName(),
            DateFormatUtil.convertToDisplayTextWithTime(eventComment.getTimestamp()),
            ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(eventComment.getId(), null))
        ));
  }

  @Test
  void deletePendingCommentsForSchedule_deletesAllPendingComments() {
    var licenceSchedule = new LicenceSchedule();

    eventCommentService.deletePendingCommentsForSchedule(licenceSchedule);

    verify(eventCommentRepository).deleteAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PENDING);
  }

  @Test
  void publishPendingCommentsForSchedule_setsPendingCommentStatusToPublished() {
    var licenceSchedule = new LicenceSchedule();

    var comment1 = new EventComment();
    comment1.setStatus(EventCommentStatus.PENDING);

    var comment2 = new EventComment();
    comment2.setStatus(EventCommentStatus.PENDING);

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PENDING))
        .thenReturn(List.of(comment1, comment2));

    eventCommentService.publishPendingCommentsForSchedule(licenceSchedule);

    assertThat(comment1.getStatus()).isEqualTo(EventCommentStatus.PUBLISHED);
    assertThat(comment2.getStatus()).isEqualTo(EventCommentStatus.PUBLISHED);
    verify(eventCommentRepository).saveAll(List.of(comment1, comment2));
  }

  @Test
  void publishPendingCommentsForSchedule_whenNoPendingComments_savesNothing() {
    var licenceSchedule = new LicenceSchedule();

    when(eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        licenceSchedule, EventCommentStatus.PENDING))
        .thenReturn(List.of());

    eventCommentService.publishPendingCommentsForSchedule(licenceSchedule);

    verify(eventCommentRepository).saveAll(List.of());
  }

  @Test
  void findPendingCommentForScheduleEvent_whenPendingCommentExists_returnsComment() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());
    var pendingComment = new EventComment();

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.of(pendingComment));

    assertThat(eventCommentService.findPendingCommentForScheduleEvent(scheduleEvent))
        .contains(pendingComment);
  }

  @Test
  void findPendingCommentForScheduleEvent_whenNoPendingComment_returnsEmpty() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());

    when(eventCommentRepository.findByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING))
        .thenReturn(Optional.empty());

    assertThat(eventCommentService.findPendingCommentForScheduleEvent(scheduleEvent)).isEmpty();
  }

  @Test
  void deletePendingCommentForScheduleEvent_deletesViaPendingStatus() {
    var scheduleEvent = new WorkProgrammeActivity();
    scheduleEvent.setOriginalEventId(UUID.randomUUID());

    eventCommentService.deletePendingCommentForScheduleEvent(scheduleEvent);

    verify(eventCommentRepository).deleteByScheduleEvent_OriginalEventIdAndStatus(
        scheduleEvent.getOriginalEventId(), EventCommentStatus.PENDING);
  }

  @Test
  void deleteEventComment_deletesComment() {
    var eventComment = new EventComment();

    eventCommentService.deleteEventComment(eventComment);

    verify(eventCommentRepository).delete(eventComment);
  }

  @Test
  void checkCommenterHasPermissionsOrThrow_whenWorkProgrammeActivityAndUserHasWpaRole_doesNotThrow() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();

    when(teamQueryService.userHasAtLeastOneRoleIn(
        user.wuaId(),
        Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR)
    )).thenReturn(true);

    assertThatCode(() ->
        eventCommentService.checkCommenterHasPermissionsOrThrow(ScheduleEventType.WORK_PROGRAMME_ACTIVITY, user)
    ).doesNotThrowAnyException();
  }

  @Test
  void checkCommenterHasPermissionsOrThrow_whenWorkProgrammeActivityAndUserLacksWpaRole_throwsException() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();

    when(teamQueryService.userHasAtLeastOneRoleIn(
        user.wuaId(),
        Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR)
    )).thenReturn(false);

    assertThatThrownBy(() ->
        eventCommentService.checkCommenterHasPermissionsOrThrow(ScheduleEventType.WORK_PROGRAMME_ACTIVITY, user)
    ).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void checkCommenterHasPermissionsOrThrow_whenNonWpaEventTypeAndUserHasScheduleAdminRole_doesNotThrow() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();

    when(teamQueryService.userHasAtLeastOneRoleIn(
        user.wuaId(),
        Set.of(Role.SCHEDULE_ADMINISTRATOR)
    )).thenReturn(true);

    assertThatCode(() ->
        eventCommentService.checkCommenterHasPermissionsOrThrow(ScheduleEventType.RATE, user)
    ).doesNotThrowAnyException();
  }

  @Test
  void checkCommenterHasPermissionsOrThrow_whenNonWpaEventTypeAndUserLacksScheduleAdminRole_throwsException() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();

    when(teamQueryService.userHasAtLeastOneRoleIn(
        user.wuaId(),
        Set.of(Role.SCHEDULE_ADMINISTRATOR)
    )).thenReturn(false);

    assertThatThrownBy(() ->
        eventCommentService.checkCommenterHasPermissionsOrThrow(ScheduleEventType.RATE, user)
    ).isInstanceOf(ResponseStatusException.class);
  }
}

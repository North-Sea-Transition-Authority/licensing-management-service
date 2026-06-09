package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceTest {

  @Mock
  private EventCommentRepository eventCommentRepository;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private Clock clock;

  @InjectMocks
  private EventCommentService eventCommentService;

  @Captor
  private ArgumentCaptor<EventComment> eventCommentArgumentCaptor;

  @Test
  void addNewComment() {
    var eventReference = new EventReference();
    var form = new EventCommentForm();
    form.setComment("Test comment");
    var author = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(42L)
        .build();

    var fixedInstant = Instant.now();
    when(clock.instant()).thenReturn(fixedInstant);

    eventCommentService.addNewComment(form, eventReference, author);

    verify(eventCommentRepository).save(eventCommentArgumentCaptor.capture());

    assertThat(eventCommentArgumentCaptor.getValue())
        .extracting(
            EventComment::getEventReference,
            EventComment::getComment,
            EventComment::getStatus,
            EventComment::getAuthorWuaId,
            EventComment::getTimestamp
        )
        .containsExactly(
            eventReference,
            "Test comment",
            EventCommentStatus.PUBLISHED,
            42L,
            fixedInstant
        );
  }

  @Test
  void getEventCommentViewsForSchedule_returnsCommentsMappedByEventReferenceId() {
    var licenceSchedule = new LicenceSchedule();

    var eventRef1 = new EventReference();
    eventRef1.setId(UUID.randomUUID());

    var eventRef2 = new EventReference();
    eventRef2.setId(UUID.randomUUID());

    var authorWuaId = 99L;

    var comment1 = new EventComment();
    comment1.setEventReference(eventRef1);
    comment1.setComment("First comment");
    comment1.setAuthorWuaId(authorWuaId);
    comment1.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));

    var comment2 = new EventComment();
    comment2.setEventReference(eventRef2);
    comment2.setComment("Second comment");
    comment2.setAuthorWuaId(authorWuaId);
    comment2.setTimestamp(Instant.parse("2025-01-02T12:00:00Z"));

    when(eventCommentRepository.getAllByEventReference_LicenceSchedule(licenceSchedule))
        .thenReturn(List.of(comment1, comment2));

    var userJson = new EnergyPortalUserJson(authorWuaId, null, "Jane", "Smith", null, null, true, null, false);

    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(authorWuaId)),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of(userJson));

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).hasSize(2);

    assertThat(result.get(eventRef1.getId()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new EventCommentView(
            "First comment",
            userJson.displayName(),
            DateFormatUtil.convertToDisplayTextWithTime(comment1.getTimestamp())
        )));

    assertThat(result.get(eventRef2.getId()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new EventCommentView(
            "Second comment",
            userJson.displayName(),
            DateFormatUtil.convertToDisplayTextWithTime(comment2.getTimestamp())
        )));
  }

  @Test
  void getEventCommentViewsForSchedule_sortsByTimestamp() {
    var licenceSchedule = new LicenceSchedule();

    var eventRef = new EventReference();
    eventRef.setId(UUID.randomUUID());

    var authorWuaId = 55L;

    var newerComment = new EventComment();
    newerComment.setEventReference(eventRef);
    newerComment.setComment("Newer comment");
    newerComment.setAuthorWuaId(authorWuaId);
    newerComment.setTimestamp(Instant.parse("2025-06-01T15:00:00Z"));

    var olderComment = new EventComment();
    olderComment.setEventReference(eventRef);
    olderComment.setComment("Older comment");
    olderComment.setAuthorWuaId(authorWuaId);
    olderComment.setTimestamp(Instant.parse("2025-01-01T09:00:00Z"));

    when(eventCommentRepository.getAllByEventReference_LicenceSchedule(licenceSchedule))
        .thenReturn(List.of(newerComment, olderComment));

    var userJson = new EnergyPortalUserJson(authorWuaId, null, "Bob", "Jones", null, null, true, null, false);

    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(authorWuaId)),
        EventCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of(userJson));

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).hasSize(1);

    var comments = result.get(eventRef.getId());
    assertThat(comments).hasSize(2);
    assertThat(comments.get(0).comment()).isEqualTo("Older comment");
    assertThat(comments.get(1).comment()).isEqualTo("Newer comment");
  }

  @Test
  void getEventCommentViewsForSchedule_emptySchedule() {
    var licenceSchedule = new LicenceSchedule();

    when(eventCommentRepository.getAllByEventReference_LicenceSchedule(licenceSchedule))
        .thenReturn(List.of());

    var result = eventCommentService.getEventCommentViewsForSchedule(licenceSchedule);

    assertThat(result).isEqualTo(Map.of());
  }
}

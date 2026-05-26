package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceTest {

  @Mock
  private EventCommentRepository eventCommentRepository;

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
}

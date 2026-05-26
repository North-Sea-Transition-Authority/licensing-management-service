package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import jakarta.transaction.Transactional;
import java.time.Clock;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@Service
public class EventCommentService {

  private final EventCommentRepository eventCommentRepository;
  private final Clock clock;

  public EventCommentService(
      EventCommentRepository eventCommentRepository,
      Clock clock
  ) {
    this.eventCommentRepository = eventCommentRepository;
    this.clock = clock;
  }

  @Transactional
  public void addNewComment(
      EventCommentForm form,
      EventReference eventReference,
      ServiceUserDetail author
  ) {
    var eventComment = new EventComment();
    eventComment.setEventReference(eventReference);
    eventComment.setComment(form.getComment());
    eventComment.setStatus(EventCommentStatus.PUBLISHED);
    eventComment.setTimestamp(clock.instant());
    eventComment.setAuthorWuaId(author.wuaId());
    eventCommentRepository.save(eventComment);
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@Audited
@Entity
@Table(name = "event_comments")
public class EventComment {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "event_reference_id")
  private EventReference eventReference;

  private String comment;

  @Enumerated(value = EnumType.STRING)
  private EventCommentStatus status;

  private Instant timestamp;

  private Long authorWuaId;

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public EventReference getEventReference() {
    return eventReference;
  }

  public void setEventReference(EventReference eventReference) {
    this.eventReference = eventReference;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public EventCommentStatus getStatus() {
    return status;
  }

  public void setStatus(EventCommentStatus status) {
    this.status = status;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Long getAuthorWuaId() {
    return authorWuaId;
  }

  public void setAuthorWuaId(Long authorWuaId) {
    this.authorWuaId = authorWuaId;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@Audited
@Entity
@Table(name = "schedule_comments")
public class ScheduleComment {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  private String comment;

  private Instant timestamp;

  private Long authorWuaId;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceSchedule getLicenceSchedule() {
    return licenceSchedule;
  }

  public void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    this.licenceSchedule = licenceSchedule;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
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

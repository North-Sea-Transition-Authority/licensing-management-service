package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "schedule_events")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "event_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ScheduleEvent {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @Column(name = "original_event_id")
  private UUID originalEventId;

  @PrePersist
  void setOriginalEventIdIfAbsent() {
    if (originalEventId == null) {
      originalEventId = id;
    }
  }

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

  public UUID getOriginalEventId() {
    return originalEventId;
  }

  public void setOriginalEventId(UUID originalEventId) {
    this.originalEventId = originalEventId;
  }

  public abstract ScheduleEventType getEventType();

  public abstract String getEventCaption();
}

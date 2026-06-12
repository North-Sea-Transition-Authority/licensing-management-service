package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "event_references")
public class EventReference {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @Enumerated(EnumType.STRING)
  private ScheduleEventType eventType;

  public LicenceSchedule getLicenceSchedule() {
    return licenceSchedule;
  }

  public void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    this.licenceSchedule = licenceSchedule;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public ScheduleEventType getEventType() {
    return eventType;
  }

  public void setEventType(ScheduleEventType eventType) {
    this.eventType = eventType;
  }
}

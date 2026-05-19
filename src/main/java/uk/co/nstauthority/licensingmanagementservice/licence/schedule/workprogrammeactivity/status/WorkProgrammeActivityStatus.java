package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@Audited
@Entity(name = "work_programme_activity_statuses")
public class WorkProgrammeActivityStatus {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "event_reference_id")
  private EventReference eventReference;

  @Enumerated(EnumType.STRING)
  private WorkProgrammeStatus status;

  private Instant appliedDatetime;

  @ManyToOne
  @JoinColumn(name = "licence_transferred_to")
  private Licence licenceTransferredTo;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public EventReference getEventReference() {
    return eventReference;
  }

  public void setEventReference(EventReference eventReference) {
    this.eventReference = eventReference;
  }

  public WorkProgrammeStatus getStatus() {
    return status;
  }

  public void setStatus(WorkProgrammeStatus status) {
    this.status = status;
  }

  public Instant getAppliedDatetime() {
    return appliedDatetime;
  }

  public void setAppliedDatetime(Instant statusAppliedDateTime) {
    this.appliedDatetime = statusAppliedDateTime;
  }

  public Licence getLicenceTransferredTo() {
    return licenceTransferredTo;
  }

  public void setLicenceTransferredTo(Licence licenceTransferredTo) {
    this.licenceTransferredTo = licenceTransferredTo;
  }
}

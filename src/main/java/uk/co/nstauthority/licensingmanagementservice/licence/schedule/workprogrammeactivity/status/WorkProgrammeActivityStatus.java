package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import jakarta.persistence.Column;
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

@Audited
@Entity(name = "work_programme_activity_statuses")
public class WorkProgrammeActivityStatus {

  @Id
  @UuidGenerator
  private UUID id;

  @Column(name = "work_programme_activity_event_reference")
  private UUID activityEventReference;

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

  public UUID getActivityEventReference() {
    return activityEventReference;
  }

  public void setActivityEventReference(UUID workProgrammeActivityEventReference) {
    this.activityEventReference = workProgrammeActivityEventReference;
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

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@Entity(name = "licence_schedule_expiry_dates")
public class LicenceScheduleExpiry {
  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  private LocalDate expiryDate;

  private String comments;

  private UUID eventReference;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceScheduleDetail getLicenceScheduleDetail() {
    return licenceScheduleDetail;
  }

  public void setLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    this.licenceScheduleDetail = licenceScheduleDetail;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public UUID getEventReference() {
    return eventReference;
  }

  public void setEventReference(UUID eventReference) {
    this.eventReference = eventReference;
  }
}


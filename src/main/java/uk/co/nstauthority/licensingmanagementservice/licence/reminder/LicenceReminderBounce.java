package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "licence_reminder_bounces")
public class LicenceReminderBounce {

  @Id
  @UuidGenerator
  private UUID id;

  @Column(nullable = false)
  private UUID notificationBatchReference;

  @Column(nullable = false)
  private Instant reportedAt;

  public UUID getId() {
    return id;
  }

  public UUID getNotificationBatchReference() {
    return notificationBatchReference;
  }

  public void setNotificationBatchReference(UUID notificationBatchReference) {
    this.notificationBatchReference = notificationBatchReference;
  }

  public Instant getReportedAt() {
    return reportedAt;
  }

  public void setReportedAt(Instant reportedAt) {
    this.reportedAt = reportedAt;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@Entity
@Table(name = "licence_reminder_missing_contacts")
public class LicenceReminderMissingContact {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  private Licence licence;

  @Column(nullable = false)
  private Integer responsibleOrganisationId;

  private UUID originalEventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReminderType reminderType;

  @Column(nullable = false)
  private LocalDate deadlineDate;

  @Column(nullable = false)
  private Instant reportedAt;

  public UUID getId() {
    return id;
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }

  public Integer getResponsibleOrganisationId() {
    return responsibleOrganisationId;
  }

  public void setResponsibleOrganisationId(Integer responsibleOrganisationId) {
    this.responsibleOrganisationId = responsibleOrganisationId;
  }

  public UUID getOriginalEventId() {
    return originalEventId;
  }

  public void setOriginalEventId(UUID originalEventId) {
    this.originalEventId = originalEventId;
  }

  public ReminderType getReminderType() {
    return reminderType;
  }

  public void setReminderType(ReminderType reminderType) {
    this.reminderType = reminderType;
  }

  public LocalDate getDeadlineDate() {
    return deadlineDate;
  }

  public void setDeadlineDate(LocalDate deadlineDate) {
    this.deadlineDate = deadlineDate;
  }

  public Instant getReportedAt() {
    return reportedAt;
  }

  public void setReportedAt(Instant reportedAt) {
    this.reportedAt = reportedAt;
  }
}

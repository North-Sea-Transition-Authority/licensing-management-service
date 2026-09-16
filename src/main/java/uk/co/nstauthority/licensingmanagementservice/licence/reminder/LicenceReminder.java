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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;

@Audited
@Entity
@Table(name = "licence_reminders")
public class LicenceReminder {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_event_id")
  private ScheduleEvent scheduleEvent;

  private UUID originalEventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReminderType reminderType;

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  private Licence licence;

  @Column(nullable = false)
  private Integer responsibleOrganisationId;

  @Column(nullable = false)
  private LocalDate deadlineDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NoticePeriod noticePeriod;

  @Column(nullable = false)
  private UUID notificationBatchReference;

  @Column(nullable = false)
  private Instant queuedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleEvent getScheduleEvent() {
    return scheduleEvent;
  }

  public void setScheduleEvent(ScheduleEvent scheduleEvent) {
    this.scheduleEvent = scheduleEvent;
  }

  public ReminderType getReminderType() {
    return reminderType;
  }

  public void setReminderType(ReminderType reminderType) {
    this.reminderType = reminderType;
  }

  public UUID getOriginalEventId() {
    return originalEventId;
  }

  public void setOriginalEventId(UUID originalEventId) {
    this.originalEventId = originalEventId;
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

  public LocalDate getDeadlineDate() {
    return deadlineDate;
  }

  public void setDeadlineDate(LocalDate deadlineDate) {
    this.deadlineDate = deadlineDate;
  }

  public NoticePeriod getNoticePeriod() {
    return noticePeriod;
  }

  public void setNoticePeriod(NoticePeriod noticePeriod) {
    this.noticePeriod = noticePeriod;
  }

  public UUID getNotificationBatchReference() {
    return notificationBatchReference;
  }

  public void setNotificationBatchReference(UUID notificationBatchReference) {
    this.notificationBatchReference = notificationBatchReference;
  }

  public Instant getQueuedAt() {
    return queuedAt;
  }

  public void setQueuedAt(Instant queuedAt) {
    this.queuedAt = queuedAt;
  }
}

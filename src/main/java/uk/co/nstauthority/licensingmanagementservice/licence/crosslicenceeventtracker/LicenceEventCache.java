package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

/**
 * Flat, denormalised cache of licence events used by the cross licence event tracker.
 *
 * <p>This table duplicates data from the source licence/schedule/application entities to simplify
 * retrieval and filtering. It is kept up to date synchronously whenever a schedule change is applied
 * or an application is submitted, rather than being derived from the source tables on read.</p>
 */
@Audited
@Entity
@Table(name = "licence_event_cache")
public class LicenceEventCache {

  @Id
  @UuidGenerator
  private UUID id;

  // @Column added for jOOQ fetchInto()
  @Column(name = "licence_id", nullable = false)
  private Integer licenceId;

  @Column(name = "licence_reference")
  private String licenceReference;

  @Column(name = "original_event_id")
  private UUID originalEventId;

  @Column(name = "event_type")
  @Enumerated(EnumType.STRING)
  private ScheduleEventType eventType;

  @Column(name = "current_term_phase")
  private String currentTermPhase;

  @Column(name = "next_term_phase")
  private String nextTermPhase;

  @Column(name = "activity_type")
  private String activityType;

  @Column(name = "event_date")
  private LocalDate eventDate;

  @Column(name = "quad_block")
  private String quadBlock;

  @Column(name = "steward_wua_id")
  private Long stewardWuaId;

  @Column(name = "application_id")
  private UUID applicationId;

  @Column(name = "application_type")
  @Enumerated(EnumType.STRING)
  private ApplicationType applicationType;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Integer getLicenceId() {
    return licenceId;
  }

  public void setLicenceId(Integer licenceId) {
    this.licenceId = licenceId;
  }

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public UUID getOriginalEventId() {
    return originalEventId;
  }

  public void setOriginalEventId(UUID originalEventId) {
    this.originalEventId = originalEventId;
  }

  public ScheduleEventType getEventType() {
    return eventType;
  }

  public void setEventType(ScheduleEventType eventType) {
    this.eventType = eventType;
  }

  public String getCurrentTermPhase() {
    return currentTermPhase;
  }

  public void setCurrentTermPhase(String currentTermPhase) {
    this.currentTermPhase = currentTermPhase;
  }

  public String getNextTermPhase() {
    return nextTermPhase;
  }

  public void setNextTermPhase(String nextTermPhase) {
    this.nextTermPhase = nextTermPhase;
  }

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getQuadBlock() {
    return quadBlock;
  }

  public void setQuadBlock(String quadBlock) {
    this.quadBlock = quadBlock;
  }

  public Long getStewardWuaId() {
    return stewardWuaId;
  }

  public void setStewardWuaId(Long stewardWuaId) {
    this.stewardWuaId = stewardWuaId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(UUID applicationId) {
    this.applicationId = applicationId;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public void setApplicationType(ApplicationType applicationType) {
    this.applicationType = applicationType;
  }
}

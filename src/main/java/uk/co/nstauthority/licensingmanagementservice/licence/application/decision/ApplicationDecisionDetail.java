package uk.co.nstauthority.licensingmanagementservice.licence.application.decision;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Audited
@Entity
@Table(name = "application_decision_details")
public class ApplicationDecisionDetail {

  @Id
  @UuidGenerator
  private UUID id;

  @Enumerated(EnumType.STRING)
  private ApplicationType applicationType;

  private UUID applicationId;

  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  private ApplicationDecisionStatus status;

  private Instant createdDatetime;

  private Long createdByWuaId;

  public ApplicationDecisionDetail() {
  }

  @VisibleForTesting
  public ApplicationDecisionDetail(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public void setApplicationType(ApplicationType applicationType) {
    this.applicationType = applicationType;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(UUID applicationId) {
    this.applicationId = applicationId;
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(Integer versionNumber) {
    this.versionNumber = versionNumber;
  }

  public ApplicationDecisionStatus getStatus() {
    return status;
  }

  public void setStatus(ApplicationDecisionStatus status) {
    this.status = status;
  }

  public Instant getCreatedDatetime() {
    return createdDatetime;
  }

  public void setCreatedDatetime(Instant createdDatetime) {
    this.createdDatetime = createdDatetime;
  }

  public Long getCreatedByWuaId() {
    return createdByWuaId;
  }

  public void setCreatedByWuaId(Long createdByWuaId) {
    this.createdByWuaId = createdByWuaId;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@MappedSuperclass
public abstract class ApplicationDetail {

  @Id
  @UuidGenerator
  private UUID id;

  @Column
  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  @Column
  private ApplicationStatus status;

  @Column
  private Instant createdDatetime;

  @Column
  private Long createdByWuaId;

  @Column
  private Instant submittedDatetime;

  @Column
  private Long submittedByWuaId;

  @Column
  private Integer responsibleOrganisationUnitId;

  @Column
  private LocalDate updateDeadlineDate;

  @Column
  private String updateReason;

  protected ApplicationDetail() {
  }

  protected ApplicationDetail(UUID id) {
    this.id = id;
  }

  public abstract Application getApplication();

  public Licence getLicence() {
    return getApplication().getLicence();
  }

  /** True when this detail is a later version of the application, i.e. an update to an earlier submission. */
  public boolean isApplicationUpdate() {
    var versionNumber = getVersionNumber();
    return versionNumber != null && versionNumber > 1;
  }

  /** True once submitted, and stays true if later withdrawn or closed. */
  public boolean hasBeenSubmitted() {
    return getSubmittedDatetime() != null;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(Integer versionNumber) {
    this.versionNumber = versionNumber;
  }

  public ApplicationStatus getStatus() {
    return status;
  }

  public void setStatus(ApplicationStatus status) {
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

  public Instant getSubmittedDatetime() {
    return submittedDatetime;
  }

  public void setSubmittedDatetime(Instant submittedDatetime) {
    this.submittedDatetime = submittedDatetime;
  }

  public Long getSubmittedByWuaId() {
    return submittedByWuaId;
  }

  public void setSubmittedByWuaId(Long submittedByWuaId) {
    this.submittedByWuaId = submittedByWuaId;
  }

  public Integer getResponsibleOrganisationUnitId() {
    return responsibleOrganisationUnitId;
  }

  public void setResponsibleOrganisationUnitId(Integer responsibleOrganisationUnitId) {
    this.responsibleOrganisationUnitId = responsibleOrganisationUnitId;
  }

  public LocalDate getUpdateDeadlineDate() {
    return updateDeadlineDate;
  }

  public void setUpdateDeadlineDate(LocalDate updateDeadlineDate) {
    this.updateDeadlineDate = updateDeadlineDate;
  }

  public String getUpdateReason() {
    return updateReason;
  }

  public void setUpdateReason(String updateReason) {
    this.updateReason = updateReason;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import com.google.common.annotations.VisibleForTesting;
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
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Audited
@Entity(name = "licence_continuation_application_details")
@PathVariableEntity(pathVariableName = LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)
public class LicenceContinuationApplicationDetail implements LicenceApplicationDetail {

  public static final String LICENCE_CONTINUATION_APPLICATION_DETAIL_ID = "licenceContinuationApplicationDetailId";

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_continuation_application_id")
  private LicenceContinuationApplication licenceContinuationApplication;

  @Column
  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  @Column
  private LicenceContinuationApplicationStatus status;

  private Instant createdDateTime;

  private Integer responsibleOrganisationUnitId;

  @Column
  private Instant submittedDatetime;

  @Column
  private Long submittedByWuaId;

  @ManyToOne
  @JoinColumn(name = "current_term_id")
  private LicenceScheduleTerm currentTerm;

  @ManyToOne
  @JoinColumn(name = "current_phase_id")
  private LicenceSchedulePhase currentPhase;

  @ManyToOne
  @JoinColumn(name = "next_term_id")
  private LicenceScheduleTerm nextTerm;

  @ManyToOne
  @JoinColumn(name = "next_phase_id")
  private LicenceSchedulePhase nextPhase;

  public LicenceContinuationApplicationDetail() {
  }

  @VisibleForTesting
  public LicenceContinuationApplicationDetail(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceContinuationApplication getLicenceContinuationApplication() {
    return licenceContinuationApplication;
  }

  public void setLicenceContinuationApplication(
      LicenceContinuationApplication licenceContinuationApplication) {
    this.licenceContinuationApplication = licenceContinuationApplication;
  }

  @Override
  public LicenceApplication getLicenceApplication() {
    return getLicenceContinuationApplication();
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(Integer versionNumber) {
    this.versionNumber = versionNumber;
  }

  public LicenceContinuationApplicationStatus getStatus() {
    return status;
  }

  public void setStatus(LicenceContinuationApplicationStatus status) {
    this.status = status;
  }

  public Instant getCreatedDateTime() {
    return createdDateTime;
  }

  public void setCreatedDateTime(Instant createdInstant) {
    this.createdDateTime = createdInstant;
  }

  public Integer getResponsibleOrganisationUnitId() {
    return responsibleOrganisationUnitId;
  }

  public void setResponsibleOrganisationUnitId(Integer responsibleOrganisationUnitId) {
    this.responsibleOrganisationUnitId = responsibleOrganisationUnitId;
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

  public LicenceScheduleTerm getCurrentTerm() {
    return currentTerm;
  }

  public void setCurrentTerm(LicenceScheduleTerm currentTerm) {
    this.currentTerm = currentTerm;
  }

  public LicenceSchedulePhase getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(LicenceSchedulePhase currentPhase) {
    this.currentPhase = currentPhase;
  }

  public LicenceScheduleTerm getNextTerm() {
    return nextTerm;
  }

  public void setNextTerm(LicenceScheduleTerm nextTerm) {
    this.nextTerm = nextTerm;
  }

  public LicenceSchedulePhase getNextPhase() {
    return nextPhase;
  }

  public void setNextPhase(LicenceSchedulePhase nextPhase) {
    this.nextPhase = nextPhase;
  }

  public Licence getLicence() {
    return getLicenceContinuationApplication().getLicenceSchedule().getLicence();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;

@Audited
@Entity(name = "schedule_work_programme_application_details")
@PathVariableEntity(pathVariableName = ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID)
public class ScheduleWorkProgrammeApplicationDetail implements LicenceApplicationDetail {

  public static final String SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID = "scheduleWorkProgrammeApplicationDetailId";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_id")
  private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;

  @Column
  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  @Column
  private ApplicationStatus status;

  @Column
  private Boolean allLicenseesPermissionConfirmed;

  @Column
  private Instant submittedDatetime;

  @Column
  private Instant createdDatetime;

  @Column
  private Long submittedByWuaId;

  @Column
  private Integer responsibleOrganisationUnitId;

  @Column
  private LocalDate decisionDate;

  public ScheduleWorkProgrammeApplicationDetail() {
  }

  @VisibleForTesting
  public ScheduleWorkProgrammeApplicationDetail(UUID swpApplicationDetailId) {
    this.id = swpApplicationDetailId;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplication() {
    return scheduleWorkProgrammeApplication;
  }

  public void setScheduleWorkProgrammeApplication(ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
    this.scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplication;
  }

  @Override
  public LicenceApplication getLicenceApplication() {
    return getScheduleWorkProgrammeApplication();
  }

  public Boolean getAllLicenseesPermissionConfirmed() {
    return allLicenseesPermissionConfirmed;
  }

  public void setAllLicenseesPermissionConfirmed(Boolean allLicenseesPermissionConfirmed) {
    this.allLicenseesPermissionConfirmed = allLicenseesPermissionConfirmed;
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

  public void setStatus(
      ApplicationStatus status) {
    this.status = status;
  }

  public Instant getSubmittedDatetime() {
    return submittedDatetime;
  }

  public void setSubmittedDatetime(Instant submittedDatetime) {
    this.submittedDatetime = submittedDatetime;
  }

  public Instant getCreatedDatetime() {
    return createdDatetime;
  }

  public void setCreatedDatetime(Instant createdDatetime) {
    this.createdDatetime = createdDatetime;
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

  public LocalDate getDecisionDate() {
    return decisionDate;
  }

  public void setDecisionDate(LocalDate decisionDate) {
    this.decisionDate = decisionDate;
  }

  public Licence getLicence() {
    return getScheduleWorkProgrammeApplication().getLicenceSchedule().getLicence();
  }
}
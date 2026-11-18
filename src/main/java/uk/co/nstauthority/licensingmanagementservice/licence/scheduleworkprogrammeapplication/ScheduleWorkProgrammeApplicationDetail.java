package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.application.Application;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationDetail;

@Audited
@Entity(name = "schedule_work_programme_application_details")
@PathVariableEntity(pathVariableName = ScheduleWorkProgrammeApplicationDetail.SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID)
public class ScheduleWorkProgrammeApplicationDetail extends ApplicationDetail {

  public static final String SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID = "scheduleWorkProgrammeApplicationDetailId";

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_id")
  private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;

  @Column
  private Boolean allLicenseesPermissionConfirmed;

  @Column
  private LocalDate decisionDate;

  public ScheduleWorkProgrammeApplicationDetail() {
  }

  @VisibleForTesting
  public ScheduleWorkProgrammeApplicationDetail(UUID swpApplicationDetailId) {
    super(swpApplicationDetailId);
  }

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplication() {
    return scheduleWorkProgrammeApplication;
  }

  public void setScheduleWorkProgrammeApplication(ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
    this.scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplication;
  }

  @Override
  public Application getApplication() {
    return getScheduleWorkProgrammeApplication();
  }

  public Boolean getAllLicenseesPermissionConfirmed() {
    return allLicenseesPermissionConfirmed;
  }

  public void setAllLicenseesPermissionConfirmed(Boolean allLicenseesPermissionConfirmed) {
    this.allLicenseesPermissionConfirmed = allLicenseesPermissionConfirmed;
  }

  public LocalDate getDecisionDate() {
    return decisionDate;
  }

  public void setDecisionDate(LocalDate decisionDate) {
    this.decisionDate = decisionDate;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.envers.Audited;

@Audited
@Entity(name = "schedule_work_programme_application_details")
public class ScheduleWorkProgrammeApplicationDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_id")
  private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;

  @Column
  private Integer versionNumber;

  @Column
  private Boolean allLicenseesPermissionConfirmed;

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
}

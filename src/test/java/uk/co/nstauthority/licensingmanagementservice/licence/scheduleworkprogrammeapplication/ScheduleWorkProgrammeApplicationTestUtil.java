package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

public class ScheduleWorkProgrammeApplicationTestUtil {

  private ScheduleWorkProgrammeApplicationTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;
    private Integer versionNumber;
    private ScheduleWorkProgrammeApplicationStatus status;
    private Boolean allLicenseesPermissionConfirmed;

    private Builder() {}

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withScheduleWorkProgrammeApplication(ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
      this.scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplication;
      return this;
    }

    public Builder withVersionNumber(Integer versionNumber) {
      this.versionNumber = versionNumber;
      return this;
    }

    public Builder withStatus(ScheduleWorkProgrammeApplicationStatus status) {
      this.status = status;
      return this;
    }

    public Builder withAllLicenseesPermissionConfirmed(Boolean allLicenseesPermissionConfirmed) {
      this.allLicenseesPermissionConfirmed = allLicenseesPermissionConfirmed;
      return this;
    }

    public ScheduleWorkProgrammeApplicationDetail build() {
      var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
      scheduleWorkProgrammeApplicationDetail.setId(id);
      scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
      scheduleWorkProgrammeApplicationDetail.setVersionNumber(versionNumber);
      scheduleWorkProgrammeApplicationDetail.setStatus(status);
      scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(allLicenseesPermissionConfirmed);

      return scheduleWorkProgrammeApplicationDetail;
    }

  }

  public static ScheduleWorkProgrammeApplication createScheduleWorkProgrammeApplication(LicenceScheduleDetail licenceScheduleDetail) {
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplication.setLicenceScheduleDetail(licenceScheduleDetail);
    return scheduleWorkProgrammeApplication;
  }
}

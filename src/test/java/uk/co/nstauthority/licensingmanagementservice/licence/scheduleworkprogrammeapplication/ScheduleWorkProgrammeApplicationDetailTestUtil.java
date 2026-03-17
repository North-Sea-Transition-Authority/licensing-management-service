package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

public class ScheduleWorkProgrammeApplicationDetailTestUtil {

  private ScheduleWorkProgrammeApplicationDetailTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;
    private Integer versionNumber;
    private ScheduleWorkProgrammeApplicationStatus status;
    private Boolean allLicenseesPermissionConfirmed;
    private Instant createdDateTime;
    private Instant submittedDatetime;
    private String applicationReference;
    private Long submittedByWuaId;
    private Integer responsibleOrganisationUnitId;

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

    public Builder withCreatedDate(Instant createdDateTime) {
      this.createdDateTime = createdDateTime;
      return this;
    }

    public Builder withSubmittedDatetime(Instant submittedDatetime) {
      this.submittedDatetime = submittedDatetime;
      return this;
    }

    public Builder withApplicationReference(String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withSubmittedByWuaId(Long submittedByWuaId) {
      this.submittedByWuaId = submittedByWuaId;
      return this;
    }

    public Builder withResponsibleOrganisationUnitId(Integer responsibleOrganisationUnitId) {
      this.responsibleOrganisationUnitId = responsibleOrganisationUnitId;
      return this;
    }

    public ScheduleWorkProgrammeApplicationDetail build() {
      var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
      scheduleWorkProgrammeApplicationDetail.setId(id);
      scheduleWorkProgrammeApplicationDetail.setVersionNumber(versionNumber);
      scheduleWorkProgrammeApplicationDetail.setStatus(status);
      scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(allLicenseesPermissionConfirmed);
      scheduleWorkProgrammeApplicationDetail.setCreatedDatetime(createdDateTime);
      scheduleWorkProgrammeApplicationDetail.setSubmittedDatetime(submittedDatetime);
      scheduleWorkProgrammeApplicationDetail.setSubmittedByWuaId(submittedByWuaId);

      if (scheduleWorkProgrammeApplication == null) {
        var swpApplication = new ScheduleWorkProgrammeApplication();
        swpApplication.setId(UUID.randomUUID());
        swpApplication.setApplicationReference(applicationReference);
        scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(swpApplication);
      } else {
        scheduleWorkProgrammeApplication.setApplicationReference(applicationReference);
        scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
      }
      scheduleWorkProgrammeApplicationDetail.setResponsibleOrganisationUnitId(responsibleOrganisationUnitId);

      return scheduleWorkProgrammeApplicationDetail;
    }

  }

  public static ScheduleWorkProgrammeApplication createScheduleWorkProgrammeApplication(LicenceScheduleDetail licenceScheduleDetail) {
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    return scheduleWorkProgrammeApplication;
  }

  public static ScheduleWorkProgrammeApplicationDetail createScheduleWorkProgrammeApplicationDetail(LicenceScheduleDetail licenceScheduleDetail) {
    var scheduleWorkProgrammeApplication = createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    return builder()
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .withId(UUID.randomUUID())
        .build();
  }
}
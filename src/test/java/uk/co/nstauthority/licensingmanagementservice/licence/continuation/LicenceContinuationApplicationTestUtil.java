package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

public class LicenceContinuationApplicationTestUtil {

  private LicenceContinuationApplicationTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id = UUID.randomUUID();
    private LicenceContinuationApplication licenceContinuationApplication;
    private Integer versionNumber = 1;
    private LicenceContinuationApplicationStatus status = LicenceContinuationApplicationStatus.DRAFT;
    private Instant submittedDatetime;
    private Long submittedByWuaId;
    private String applicationReference;

    private Builder() {}

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLicenceContinuationApplication(LicenceContinuationApplication licenceContinuationApplication) {
      this.licenceContinuationApplication = licenceContinuationApplication;
      return this;
    }

    public Builder withVersionNumber(Integer versionNumber) {
      this.versionNumber = versionNumber;
      return this;
    }

    public Builder withStatus(LicenceContinuationApplicationStatus status) {
      this.status = status;
      return this;
    }

    public Builder withSubmittedDatetime(Instant submittedDatetime) {
      this.submittedDatetime = submittedDatetime;
      return this;
    }

    public Builder withSubmittedByWuaId(Long submittedByWuaId) {
      this.submittedByWuaId = submittedByWuaId;
      return this;
    }

    public Builder withApplicationReference(String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public LicenceContinuationApplicationDetail build() {
      var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
      licenceContinuationApplicationDetail.setId(id);
      licenceContinuationApplicationDetail.setVersionNumber(versionNumber);
      licenceContinuationApplicationDetail.setStatus(status);
      licenceContinuationApplicationDetail.setSubmittedDatetime(submittedDatetime);
      licenceContinuationApplicationDetail.setSubmittedByWuaId(submittedByWuaId);

      if (licenceContinuationApplication == null) {
        var app = new LicenceContinuationApplication();
        app.setId(UUID.randomUUID());
        app.setApplicationReference(applicationReference);
        licenceContinuationApplicationDetail.setLicenceContinuationApplication(app);
      } else {
        licenceContinuationApplication.setApplicationReference(applicationReference);
        licenceContinuationApplicationDetail.setLicenceContinuationApplication(licenceContinuationApplication);
      }
      return licenceContinuationApplicationDetail;
    }
  }

  public static LicenceContinuationApplication createLicenceContinuationApplication(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());
    licenceContinuationApplication.setLicenceScheduleDetail(licenceScheduleDetail);
    return licenceContinuationApplication;
  }

  public static LicenceContinuationApplicationDetail createLicenceContinuationApplicationDetail(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceContinuationApplication = createLicenceContinuationApplication(licenceScheduleDetail);
    return builder()
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();
  }
}
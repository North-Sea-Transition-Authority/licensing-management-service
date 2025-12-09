package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

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

    public LicenceContinuationApplicationDetail build() {
      var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
      licenceContinuationApplicationDetail.setId(id);
      licenceContinuationApplicationDetail.setLicenceContinuationApplication(licenceContinuationApplication);
      licenceContinuationApplicationDetail.setVersionNumber(versionNumber);
      licenceContinuationApplicationDetail.setStatus(status);

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

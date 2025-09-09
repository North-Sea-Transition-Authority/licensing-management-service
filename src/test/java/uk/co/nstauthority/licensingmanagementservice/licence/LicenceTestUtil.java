package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

public class LicenceTestUtil {

  private LicenceTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private LicenceType licenceType;
    private String licenceNumber;
    private String licencePrefix;
    private String licenceReference;

    private Builder() {}

    public Builder setLicenceType(LicenceType licenceType) {
      this.licenceType = licenceType;
      return this;
    }

    public Builder setLicenceNumber(String licenceNumber) {
      this.licenceNumber = licenceNumber;
      return this;
    }

    public Builder setLicencePrefix(String licencePrefix) {
      this.licencePrefix = licencePrefix;
      return this;
    }

    public Builder setLicenceReference(String licenceReference) {
      this.licenceReference = licenceReference;
      return this;
    }

    public Licence build() {
      var licence = new Licence();
      licence.setType(licenceType);
      licence.setLicenceNumber(licenceNumber);
      licence.setPrefix(licencePrefix);
      licence.setLicenceReference(licenceReference);

      return licence;
    }
  }

  public static LicenceSchedule createLicenceSchedule(Licence licence) {
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    return licenceSchedule;
  }

  public static LicenceScheduleDetail createLicenceScheduleDetail(Licence licence, UUID scheduleDetailId) {
    var licenceSchedule = createLicenceSchedule(licence);

    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    licenceScheduleDetail.setId(scheduleDetailId);
    return licenceScheduleDetail;
  }
}
